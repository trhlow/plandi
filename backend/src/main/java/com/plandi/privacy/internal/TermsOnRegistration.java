package com.plandi.privacy.internal;

import com.plandi.account.AccountRegistered;
import com.plandi.privacy.ConsentService;
import com.plandi.privacy.Purpose;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Đăng ký là đồng ý điều khoản (form có link Điều khoản và Chính sách). Chạy trong transaction đăng ký.
@Component
class TermsOnRegistration {

    private final ConsentService consents;

    TermsOnRegistration(ConsentService consents) {
        this.consents = consents;
    }

    @EventListener
    void on(AccountRegistered event) {
        consents.set(event.accountId(), Purpose.TERMS, true);
    }
}
