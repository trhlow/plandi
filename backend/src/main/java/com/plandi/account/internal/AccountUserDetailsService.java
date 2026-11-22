package com.plandi.account.internal;

import com.plandi.account.AccountService;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accounts;

    AccountUserDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    // Username trả về là id tài khoản, để principal sau khi xác thực không mang email.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Account account = accounts.findByEmail(AccountService.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("not found"));
        return new User(
                String.valueOf(account.id()),
                account.passwordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name())));
    }
}
