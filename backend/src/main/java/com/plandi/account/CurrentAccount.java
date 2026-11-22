package com.plandi.account;

import com.plandi.account.internal.AuthenticatedAccount;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Module khác lấy "người đang gọi là ai" qua đây, không tự đọc session. */
@Component
public class CurrentAccount {

    public long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedAccount account) {
            return account.id();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
