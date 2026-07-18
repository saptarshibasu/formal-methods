package com.formalmethods.repository;

import com.formalmethods.domain.Order;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link Order} (constitution Article IV — mocked in tests). */
public interface OrderRepository extends JpaRepository<Order, UUID> {
}
