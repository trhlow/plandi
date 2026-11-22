package com.plandi.account.internal;

import com.plandi.account.AccountService;
import com.plandi.account.AccountView;
import com.plandi.account.CurrentAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

    private final AccountService accounts;
    private final CurrentAccount currentAccount;

    MeController(AccountService accounts, CurrentAccount currentAccount) {
        this.accounts = accounts;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/api/v1/me")
    AccountView me() {
        return accounts.find(currentAccount.id());
    }
}
