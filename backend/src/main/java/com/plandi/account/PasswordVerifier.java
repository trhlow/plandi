package com.plandi.account;

import com.plandi.account.internal.AccountRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Privacy dùng khi người dùng nhập lại mật khẩu để xác nhận xóa tài khoản. */
@Component
public class PasswordVerifier {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    PasswordVerifier(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean matches(long accountId, String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        return accounts.findById(accountId)
                .map(a -> passwordEncoder.matches(password, a.passwordHash()))
                .orElse(false);
    }
}
