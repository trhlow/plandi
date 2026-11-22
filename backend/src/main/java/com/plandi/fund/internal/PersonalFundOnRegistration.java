package com.plandi.fund.internal;

import com.plandi.account.AccountRegistered;
import com.plandi.fund.internal.FundStore.Kind;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Chạy trong transaction đăng ký: tạo quỹ lỗi thì đăng ký rollback, không có tài khoản nào thiếu quỹ Cá nhân.
@Component
class PersonalFundOnRegistration {

    private final FundStore funds;

    PersonalFundOnRegistration(FundStore funds) {
        this.funds = funds;
    }

    @EventListener
    void on(AccountRegistered event) {
        long fundId = funds.createFund(Kind.PERSONAL);
        funds.addMember(fundId, event.accountId(), Kind.PERSONAL);
    }
}
