package com.finsimx.repository;

import com.finsimx.entity.Order;
import com.finsimx.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Legacy methods
    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndAsset(Long userId, String asset);

    List<Order> findByAssetAndStatus(String asset, Order.OrderStatus status);

    List<Order> findByAsset(String asset);

    @Query("SELECT o FROM Order o WHERE o.asset = :asset AND o.status IN ('OPEN', 'PARTIAL') ORDER BY o.createdAt ASC")
    List<Order> findActiveOrdersByAsset(@Param("asset") String asset);

    // New methods for Step 4

    /**
     * Find all user orders with pagination
     */
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find all user orders (non-paginated)
     */
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find user orders by status
     */
    List<Order> findByUserAndStatusOrderByCreatedAtDesc(User user, Order.OrderStatus status);

    /**
     * Find user orders by type (BUY/SELL)
     */
    List<Order> findByUserAndTypeOrderByCreatedAtDesc(User user, Order.OrderType type);

    /**
     * Find user orders by asset
     */
    List<Order> findByUserAndAssetOrderByCreatedAtDesc(User user, String asset);

    /**
     * Count total orders for user
     */
    long countByUser(User user);

    /**
     * Count orders for user by status
     */
    long countByUserAndStatus(User user, Order.OrderStatus status);
}
