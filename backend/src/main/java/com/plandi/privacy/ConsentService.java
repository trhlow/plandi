package com.plandi.privacy;

import com.plandi.privacy.internal.ConsentEvent;
import com.plandi.privacy.internal.ConsentEventRepository;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

@Service
public class ConsentService {

    /** Đổi điều khoản hoặc chính sách thì tăng version: người dùng phải đồng ý lại TERMS mới dùng tiếp được. */
    public static final String POLICY_VERSION = "2026-11";

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);

    private final ConsentEventRepository consents;
    private final ApplicationEventPublisher events;

    ConsentService(ConsentEventRepository consents, ApplicationEventPublisher events) {
        this.consents = consents;
        this.events = events;
    }

    /** TERMS chỉ tính là đã đồng ý khi đồng ý đúng phiên bản chính sách hiện tại. */
    @Transactional(readOnly = true)
    public boolean isGranted(long accountId, Purpose purpose) {
        return consents.findFirstByAccountIdAndPurposeOrderByIdDesc(accountId, purpose)
                .filter(ConsentEvent::granted)
                .filter(e -> purpose != Purpose.TERMS || POLICY_VERSION.equals(e.policyVersion()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public Map<Purpose, Boolean> current(long accountId) {
        Map<Purpose, Boolean> state = new EnumMap<>(Purpose.class);
        for (Purpose purpose : Purpose.values()) {
            state.put(purpose, isGranted(accountId, purpose));
        }
        return state;
    }

    @Transactional
    public void set(long accountId, Purpose purpose, boolean granted) {
        if (purpose == Purpose.TERMS && !granted) {
            throw new ErrorResponseException(
                    HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.BAD_REQUEST,
                            "Không rút riêng điều khoản được. Muốn ngừng dùng app thì xóa tài khoản."),
                    null);
        }
        boolean wasGranted = isGranted(accountId, purpose);
        consents.save(new ConsentEvent(accountId, purpose, granted, POLICY_VERSION));
        if (wasGranted && !granted) {
            events.publishEvent(new ConsentWithdrawn(accountId, purpose));
        }
        log.info("consent recorded accountId={} purpose={} granted={}", accountId, purpose, granted);
    }
}
