package com.plandi.fund;

import com.plandi.fund.internal.FundStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cổng kiểm tra quyền cho mọi dữ liệu theo quỹ. Mọi thao tác đọc hoặc ghi theo {@code fundId} phải gọi qua đây trước. */
@Service
public class FundAccess {

    private final FundStore funds;

    FundAccess(FundStore funds) {
        this.funds = funds;
    }

    /** Người gọi là thành viên hiện tại của một quỹ đang hoạt động; nếu không thì 404. */
    @Transactional(readOnly = true)
    public void requireMember(long accountId, long fundId) {
        if (!funds.isActiveMember(accountId, fundId)) {
            throw new FundNotFoundException();
        }
    }
}
