package com.plandi.account.internal;

import com.plandi.account.AccountService;
import com.plandi.account.AccountView;
import com.plandi.account.RegistrationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AccountService accounts;
    private final AuthenticationManager authenticationManager;
    private final LoginThrottle throttle;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    AuthController(AccountService accounts, AuthenticationManager authenticationManager, LoginThrottle throttle) {
        this.accounts = accounts;
        this.authenticationManager = authenticationManager;
        this.throttle = throttle;
    }

    record LoginRequest(String email, String password) {}

    @PostMapping("/register")
    ResponseEntity<AccountView> register(
            @Valid @RequestBody RegistrationRequest body, HttpServletRequest request, HttpServletResponse response) {
        AccountView account = accounts.register(body);
        signIn(new AuthenticatedAccount(account.id(), Role.valueOf(account.role())), request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @PostMapping("/login")
    ResponseEntity<Void> login(
            @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        // Mật khẩu quá 72 byte không thể đúng (bcrypt), trả cùng lỗi như sai mật khẩu thay vì để encoder ném lỗi.
        if (body.email() == null
                || body.password() == null
                || body.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badCredentials();
        }
        // Sau proxy (Cloudflare Tunnel), Tomcat lấy IP thật từ X-Forwarded-For: server.forward-headers-strategy=native.
        String ip = request.getRemoteAddr();
        if (throttle.isLocked(body.email(), ip)) {
            log.info("login locked");
            throw new ErrorResponseException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.TOO_MANY_REQUESTS, "Bạn đã nhập sai quá nhiều lần. Thử lại sau 15 phút."),
                    null);
        }
        Authentication authenticated;
        try {
            authenticated = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.email(), body.password()));
        } catch (AuthenticationException e) {
            throttle.recordFailure(body.email(), ip);
            log.info("login failed");
            throw badCredentials();
        }
        throttle.reset(body.email(), ip);
        long accountId = Long.parseLong(authenticated.getName());
        Role role = authenticated.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                ? Role.ADMIN
                : Role.USER;
        signIn(new AuthenticatedAccount(accountId, role), request, response);
        log.info("login ok accountId={}", accountId);
        return ResponseEntity.noContent().build();
    }

    // Sai email và sai mật khẩu trả cùng một body, để không dò được email nào đã đăng ký.
    private static ErrorResponseException badCredentials() {
        return new ErrorResponseException(
                HttpStatus.UNAUTHORIZED,
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng."),
                null);
    }

    private void signIn(AuthenticatedAccount account, HttpServletRequest request, HttpServletResponse response) {
        request.getSession();
        request.changeSessionId(); // chống session fixation: id phiên đổi sau khi đăng nhập
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                account,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name()))));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
