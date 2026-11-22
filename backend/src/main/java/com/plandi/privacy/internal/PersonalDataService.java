package com.plandi.privacy.internal;

import com.plandi.privacy.PersonalDataHandler;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

/** Xuất toàn bộ dữ liệu và xóa tài khoản bằng cách gọi mọi {@link PersonalDataHandler} (spec/SPEC-privacy.md). */
@Service
class PersonalDataService {

    private static final Logger log = LoggerFactory.getLogger(PersonalDataService.class);
    private static final int EXPORTS_PER_DAY = 5;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final List<PersonalDataHandler> handlers;
    private final StringRedisTemplate redis;

    PersonalDataService(List<PersonalDataHandler> handlers, StringRedisTemplate redis) {
        this.handlers = handlers;
        this.redis = redis;
    }

    record DataExport(Instant exportedAt, Map<String, Object> sections) {}

    @Transactional(readOnly = true)
    DataExport export(long accountId) {
        String key = "exports:" + accountId + ":" + LocalDate.now(VIETNAM);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofDays(1));
        }
        if (count != null && count > EXPORTS_PER_DAY) {
            throw new ErrorResponseException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.TOO_MANY_REQUESTS, "Mỗi ngày chỉ tải dữ liệu được 5 lần. Thử lại ngày mai."),
                    null);
        }
        Map<String, Object> sections = new TreeMap<>();
        handlers.forEach(h -> sections.put(h.section(), h.export(accountId)));
        log.info("data exported accountId={}", accountId);
        return new DataExport(Instant.now(), sections);
    }

    /** Một transaction: handler nào lỗi thì không dòng nào bị xóa. Hủy phiên là việc của controller, sau khi commit. */
    @Transactional
    void deleteEverything(long accountId) {
        handlers.stream()
                .sorted(Comparator.comparingInt(PersonalDataHandler::deleteOrder))
                .forEach(h -> h.delete(accountId));
        log.info("account deleted accountId={}", accountId);
    }
}
