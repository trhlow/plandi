package com.plandi.account.internal;

import com.plandi.account.AccountService;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

/**
 * Người dùng nhắn admin qua Zalo, admin tạo mã 8 ký tự gửi lại, người dùng nhập mã cùng mật khẩu mới (spec/SPEC-account.md).
 */
@Service
class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration VALID_FOR = Duration.ofHours(1);
    // Bỏ I, O, 0, 1 để đọc qua Zalo không nhầm.
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;

    private final AccountRepository accounts;
    private final PasswordResetRepository resets;
    private final PasswordEncoder passwordEncoder;
    private final LoginThrottle throttle;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;
    private final SecureRandom random = new SecureRandom();

    PasswordResetService(
            AccountRepository accounts,
            PasswordResetRepository resets,
            PasswordEncoder passwordEncoder,
            LoginThrottle throttle,
            FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.accounts = accounts;
        this.resets = resets;
        this.passwordEncoder = passwordEncoder;
        this.throttle = throttle;
        this.sessions = sessions;
    }

    record IssuedCode(String code, Instant expiresAt) {}

    @Transactional
    IssuedCode issue(String email) {
        Account account = accounts.findByEmail(AccountService.normalizeEmail(email))
                .orElseThrow(() -> new ErrorResponseException(HttpStatus.NOT_FOUND));
        Instant now = Instant.now();
        resets.findByAccountIdAndUsedAtIsNullAndExpiresAtAfter(account.id(), now)
                .forEach(old -> old.expire(now));
        String code = newCode();
        Instant expiresAt = now.plus(VALID_FOR);
        resets.save(new PasswordReset(account.id(), passwordEncoder.encode(code), expiresAt));
        log.info("password reset code issued accountId={}", account.id());
        return new IssuedCode(code, expiresAt);
    }

    @Transactional
    void reset(String email, String code, String newPassword, String ip) {
        if (email == null) {
            throw invalidCode();
        }
        if (throttle.isLocked("reset", email, ip)) {
            log.info("password reset locked");
            throw new ErrorResponseException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.TOO_MANY_REQUESTS, "Bạn đã nhập sai quá nhiều lần. Thử lại sau 15 phút."),
                    null);
        }
        String normalizedEmail = AccountService.normalizeEmail(email);
        // Kiểm tra mật khẩu mới trước mã: lỗi này không cho biết mã đúng hay sai.
        AccountService.requireValidPassword(newPassword, normalizedEmail);
        Instant now = Instant.now();
        Optional<Account> account = accounts.findByEmail(normalizedEmail);
        List<PasswordReset> active = account.map(
                        a -> resets.findByAccountIdAndUsedAtIsNullAndExpiresAtAfter(a.id(), now))
                .orElse(List.of());
        String typed = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        Optional<PasswordReset> match = typed.getBytes(StandardCharsets.UTF_8).length > CODE_LENGTH
                ? Optional.empty()
                : active.stream()
                        .filter(r -> passwordEncoder.matches(typed, r.codeHash()))
                        .findFirst();
        if (match.isEmpty()) {
            throttle.recordFailure("reset", email, ip);
            log.info("password reset rejected");
            throw invalidCode();
        }
        Account target = account.get();
        match.get().markUsed(now);
        target.changePassword(passwordEncoder.encode(newPassword), now);
        throttle.reset("reset", email, ip);
        throttle.reset("login", email, ip);
        sessions.findByPrincipalName(String.valueOf(target.id())).keySet().forEach(sessions::deleteById);
        log.info("password reset done accountId={}", target.id());
    }

    private String newCode() {
        char[] code = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }

    // Mã sai, hết hạn, đã dùng hay đã bị mã mới thay thế đều trả cùng một lỗi.
    private static ErrorResponseException invalidCode() {
        return new ErrorResponseException(
                HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Mã không đúng hoặc đã hết hạn. Hãy nhắn admin để lấy mã mới."),
                null);
    }
}
