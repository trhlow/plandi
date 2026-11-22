package com.plandi.account.internal;

import com.plandi.account.AccountService;
import com.plandi.account.AccountView;
import com.plandi.account.CurrentAccount;
import com.plandi.account.ProfileUpdate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
class ProfileController {

    private final AccountService accounts;
    private final CurrentAccount currentAccount;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    ProfileController(
            AccountService accounts,
            CurrentAccount currentAccount,
            FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.accounts = accounts;
        this.currentAccount = currentAccount;
        this.sessions = sessions;
    }

    record PasswordChange(String currentPassword, String newPassword) {}

    @GetMapping
    AccountView me() {
        return accounts.find(currentAccount.id());
    }

    @PatchMapping
    AccountView update(@Valid @RequestBody ProfileUpdate update) {
        return accounts.updateProfile(currentAccount.id(), update);
    }

    // Đổi mật khẩu thì các thiết bị khác phải đăng nhập lại; thiết bị đang đổi vẫn giữ phiên.
    @PutMapping("/password")
    ResponseEntity<Void> changePassword(@RequestBody PasswordChange body, HttpServletRequest request) {
        long accountId = currentAccount.id();
        accounts.changePassword(accountId, body.currentPassword(), body.newPassword());
        String currentSessionId = request.getSession().getId();
        sessions.findByPrincipalName(String.valueOf(accountId)).keySet().stream()
                .filter(id -> !id.equals(currentSessionId))
                .forEach(sessions::deleteById);
        return ResponseEntity.noContent().build();
    }
}
