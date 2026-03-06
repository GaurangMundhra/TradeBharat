package com.finsimx.repository;

import com.finsimx.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByAssetOrderByCreatedAtDesc(String asset);

    List<Trade> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId);

    @Query("SELECT t FROM Trade t WHERE t.asset = :asset ORDER BY t.createdAt DESC LIMIT :limit")
    List<Trade> findRecentTradesByAsset(@Param("asset") String asset, @Param("limit") int limit);
}
