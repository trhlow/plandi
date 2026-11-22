package com.plandi.account.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    /** Mã còn hiệu lực: chưa dùng và chưa hết hạn (tạo mã mới thì mã cũ bị đặt hết hạn ngay). */
    List<PasswordReset> findByAccountIdAndUsedAtIsNullAndExpiresAtAfter(long accountId, Instant now);

    void deleteByAccountId(long accountId);
}
