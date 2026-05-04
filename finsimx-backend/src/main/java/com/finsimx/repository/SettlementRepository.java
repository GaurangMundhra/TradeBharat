package com.finsimx.repository;

import com.finsimx.entity.Settlement;
import com.finsimx.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByTradeId(Long tradeId);

    List<Settlement> findByBuyerOrderBySettledAtDesc(User buyer);

    List<Settlement> findBySellerOrderBySettledAtDesc(User seller);

    List<Settlement> findByBuyerOrSellerOrderBySettledAtDesc(User buyer, User seller);

    List<Settlement> findByStatus(Settlement.SettlementStatus status);

    int countByStatus(Settlement.SettlementStatus status);
}
