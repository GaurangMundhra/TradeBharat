package com.finsimx.repository;

import com.finsimx.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndAsset(Long userId, String asset);

    List<Order> findByAssetAndStatus(String asset, Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.asset = :asset AND o.status IN ('OPEN', 'PARTIAL') ORDER BY o.createdAt ASC")
    List<Order> findActiveOrdersByAsset(@Param("asset") String asset);

    List<Order> findByAsset(String asset);
}
