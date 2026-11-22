package com.plandi.fund;

import com.plandi.fund.internal.FundStore;
import com.plandi.fund.internal.FundStore.Kind;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FundQuery {

    private final FundStore funds;

    FundQuery(FundStore funds) {
        this.funds = funds;
    }

    /** Mỗi tài khoản luôn có đúng một quỹ Cá nhân, tạo lúc đăng ký. */
    @Transactional(readOnly = true)
    public long personalFundOf(long accountId) {
        return funds.activeFundOf(accountId, Kind.PERSONAL).orElseThrow(FundNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Optional<Long> activeCoupleFundOf(long accountId) {
        return funds.activeFundOf(accountId, Kind.COUPLE);
    }

    @Transactional(readOnly = true)
    public List<Long> activeMemberIds(long fundId) {
        return funds.activeMemberIds(fundId);
    }
}
