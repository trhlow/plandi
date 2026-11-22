package com.plandi.fund.internal;

import com.plandi.account.CurrentAccount;
import com.plandi.fund.FundAccess;
import com.plandi.fund.FundNotFoundException;
import com.plandi.fund.internal.FundStore.Kind;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/funds")
class FundController {

    private final FundStore funds;
    private final FundAccess fundAccess;
    private final CurrentAccount currentAccount;

    FundController(FundStore funds, FundAccess fundAccess, CurrentAccount currentAccount) {
        this.funds = funds;
        this.fundAccess = fundAccess;
        this.currentAccount = currentAccount;
    }

    record FundSummary(long id, Kind kind, int memberCount) {}

    record FundDetail(long id, Kind kind, String status, int memberCount) {}

    @GetMapping
    List<FundSummary> mine() {
        return funds.activeFundsOf(currentAccount.id()).stream()
                .map(f -> new FundSummary(f.id(), f.kind(), f.memberCount()))
                .toList();
    }

    @GetMapping("/{id}")
    FundDetail one(@PathVariable long id) {
        fundAccess.requireMember(currentAccount.id(), id);
        FundStore.FundRow fund = funds.find(id).orElseThrow(FundNotFoundException::new);
        return new FundDetail(
                fund.id(), fund.kind(), fund.status(), funds.activeMemberIds(id).size());
    }

    // Rời quỹ Cặp đôi (luồng chia tay) làm ở Task 15; quỹ Cá nhân không bao giờ rời được.
    @PostMapping("/{id}/leave")
    void leave(@PathVariable long id) {
        fundAccess.requireMember(currentAccount.id(), id);
        if (funds.find(id).orElseThrow(FundNotFoundException::new).kind() == Kind.PERSONAL) {
            throw new ErrorResponseException(
                    HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ngân sách cá nhân không rời được."),
                    null);
        }
        throw new ErrorResponseException(HttpStatus.NOT_IMPLEMENTED);
    }
}
