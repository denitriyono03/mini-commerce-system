package com.assessment.order.repository;

import com.assessment.order.model.dto.TopSpenderReport;
import com.assessment.order.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = """
        WITH CustomerSpending AS (
            SELECT customer_id, SUM(total_amount) as total_spent
            FROM orders
            WHERE status = :status
            GROUP BY customer_id
        ),
        RankedSpending AS (
            SELECT customer_id, total_spent,
                DENSE_RANK() OVER (ORDER BY total_spent DESC) as rank_position
            FROM CustomerSpending
        )
        SELECT customer_id as customerId, 
            total_spent as totalSpent, 
            rank_position as rankPosition
        FROM RankedSpending
        WHERE rank_position <= 3
        ORDER BY rank_position ASC
        """, nativeQuery = true)
    List<TopSpenderReport> findTopSpendersByStatus(@Param("status") String status);
}