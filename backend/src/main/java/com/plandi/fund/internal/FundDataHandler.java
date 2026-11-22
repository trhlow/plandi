package com.plandi.fund.internal;

import com.plandi.fund.FundDeleted;
import com.plandi.privacy.PersonalDataHandler;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Xuất và xóa dữ liệu của fund (spec/SPEC-fund.md, mục Quyền riêng tư). Chạy trước consent và account. */
@Component
class FundDataHandler implements PersonalDataHandler {

    private final FundStore funds;
    private final ApplicationEventPublisher events;

    FundDataHandler(FundStore funds, ApplicationEventPublisher events) {
        this.funds = funds;
        this.events = events;
    }

    @Override
    public String section() {
        return "funds";
    }

    // Không xuất id hay tên người yêu: đó là dữ liệu của người kia.
    @Override
    public Object export(long accountId) {
        return funds.membershipsOf(accountId);
    }

    // shortcut: chưa có quỹ Cặp đôi (Task 14), nên chưa chạy luồng rời quỹ trước khi xóa; Task 15 thêm bước đó.
    @Override
    public void delete(long accountId) {
        List<Long> fundIds = funds.fundIdsOf(accountId);
        funds.deleteMemberships(accountId);
        for (long emptyFund : funds.withoutMembers(fundIds)) {
            events.publishEvent(new FundDeleted(emptyFund));
            funds.deleteFund(emptyFund);
        }
    }

    @Override
    public int deleteOrder() {
        return 80;
    }
}
