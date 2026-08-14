package com.tieto.poc.ai_servicenow.repository;

import com.tieto.poc.ai_servicenow.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatusAndUpdatedAtBefore(
            Order.OrderStatus status,
            LocalDateTime cutoff
    );

    boolean existsByOrderId(String orderId);
}
