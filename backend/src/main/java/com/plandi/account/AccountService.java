package com.plandi.account;

import com.plandi.account.internal.Account;
import com.plandi.account.internal.AccountRepository;
import com.plandi.account.internal.EmailTakenException;
import com.plandi.account.internal.PasswordResetRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accounts;
    private final PasswordResetRepository passwordResets;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    AccountService(
            AccountRepository accounts,
            PasswordResetRepository passwordResets,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events) {
        this.accounts = accounts;
        this.passwordResets = passwordResets;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AccountView register(RegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (accounts.existsByEmail(email)) {
            throw new EmailTakenException();
        }
        Account account;
        try {
            account = accounts.saveAndFlush(new Account(
                    email,
                    passwordEncoder.encode(request.password()),
                    request.displayName().strip(),
                    request.area()));
        } catch (DataIntegrityViolationException raceOnUniqueEmail) {
            throw new EmailTakenException();
        }
        events.publishEvent(new AccountRegistered(account.id()));
        log.info("account registered accountId={}", account.id());
        return account.view();
    }

    @Transactional(readOnly = true)
    public AccountView find(long accountId) {
        return accounts.findById(accountId).orElseThrow().view();
    }

    /** Dữ liệu của module account trong file xuất (privacy gọi). Không có hash mật khẩu. */
    @Transactional(readOnly = true)
    public AccountExport exportData(long accountId) {
        return accounts.findById(accountId).orElseThrow().export();
    }

    /** Xóa mọi dòng của module account; privacy gọi cuối cùng, trong transaction xóa tài khoản. */
    @Transactional
    public void deleteData(long accountId) {
        passwordResets.deleteByAccountId(accountId);
        accounts.deleteById(accountId);
        log.info("account data deleted accountId={}", accountId);
    }

    @Transactional
    public AccountView updateProfile(long accountId, ProfileUpdate update) {
        Account account = accounts.findById(accountId).orElseThrow();
        account.updateProfile(update.displayName(), update.area());
        return account.view();
    }

    /** Chỉ đổi mật khẩu; hủy các phiên khác là việc của tầng web (cần biết phiên hiện tại). */
    @Transactional
    public void changePassword(long accountId, String currentPassword, String newPassword) {
        Account account = accounts.findById(accountId).orElseThrow();
        if (!fitsBcrypt(currentPassword) || !passwordEncoder.matches(currentPassword, account.passwordHash())) {
            throw badRequest("Mật khẩu hiện tại không đúng.");
        }
        requireValidPassword(newPassword, account.email());
        account.changePassword(passwordEncoder.encode(newPassword), Instant.now());
        log.info("password changed accountId={}", accountId);
    }

    // Cùng quy tắc với RegistrationRequest: ít nhất 8 ký tự, tối đa 72 byte (bcrypt), khác email.
    public static void requireValidPassword(String password, String email) {
        if (password == null
                || password.length() < 8
                || !fitsBcrypt(password)
                || password.strip().equalsIgnoreCase(email)) {
            throw badRequest("Mật khẩu mới phải có ít nhất 8 ký tự, tối đa 72 byte và khác email.");
        }
    }

    private static boolean fitsBcrypt(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private static ErrorResponseException badRequest(String detail) {
        return new ErrorResponseException(
                HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail), null);
    }
}
