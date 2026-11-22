package com.plandi.account;

import com.plandi.account.internal.Account;
import com.plandi.account.internal.AccountRepository;
import com.plandi.account.internal.EmailTakenException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder, ApplicationEventPublisher events) {
        this.accounts = accounts;
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
}
