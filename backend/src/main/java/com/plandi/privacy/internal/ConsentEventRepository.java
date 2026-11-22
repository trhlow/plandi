package com.plandi.privacy.internal;

import com.plandi.privacy.Purpose;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentEventRepository extends JpaRepository<ConsentEvent, Long> {

    // Sắp theo id chứ không theo created_at: hai dòng trong cùng transaction có cùng now().
    Optional<ConsentEvent> findFirstByAccountIdAndPurposeOrderByIdDesc(long accountId, Purpose purpose);

    List<ConsentEvent> findByAccountIdOrderByIdAsc(long accountId);

    void deleteByAccountId(long accountId);
}
