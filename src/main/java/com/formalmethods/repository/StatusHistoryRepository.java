package com.formalmethods.repository;

import com.formalmethods.domain.StatusHistoryEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link StatusHistoryEntry} (constitution Article IV — mocked in tests). */
public interface StatusHistoryRepository extends JpaRepository<StatusHistoryEntry, Long> {

    /** Chronological history for one order — monotonic identity {@code id} order (plan.md). */
    List<StatusHistoryEntry> findByOrderIdOrderByIdAsc(UUID orderId);
}
