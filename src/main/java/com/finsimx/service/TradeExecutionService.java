package com.finsimx.service;

import com.finsimx.dto.PortfolioResponse;
import com.finsimx.dto.PositionResponse;
import com.finsimx.dto.SettlementResponse;
import com.finsimx.entity.Position;
import com.finsimx.entity.Settlement;
import com.finsimx.entity.Trade;
import com.finsimx.entity.User;
import com.finsimx.exception.OrderException;
import com.finsimx.repository.PositionRepository;
import com.finsimx.repository.SettlementRepository;
import com.finsimx.repository.TradeRepository;
import com.finsimx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Trade Execution & Settlement Service
 * Handles:
 * - Settlement of trades
 * - Position updates
 * - P&L calculation
 * - Portfolio management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradeExecutionService {

    private final TradeRepository tradeRepository;
    private final SettlementRepository settlementRepository;
    private final PositionRepository positionRepository;
    private final UserRepository userRepository;
    private final MatchingService matchingService;

    /**
     * Execute/settle a trade
     * Updates buyer and seller positions
     * Records settlement details
     */
    @Transactional
    public SettlementResponse executeTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new OrderException("TRADE_NOT_FOUND", 404, "Trade not found"));

        // Check if already settled
        var existing = settlementRepository.findByTradeId(tradeId);
        if (existing.isPresent()) {
            return SettlementResponse.from(existing.get());
        }

        // Calculate settlement values
        BigDecimal buyerCost = trade.getPrice().multiply(trade.getQuantity());
        BigDecimal sellerProceeds = buyerCost; // Same as buyer cost for now

        // Get buyer and seller's original positions for P&L
        Position sellerPosition = positionRepository.findByUserAndAsset(trade.getSeller(), trade.getAsset())
                .orElse(null);
        BigDecimal sellerCostBasis = sellerPosition != null
                ? sellerPosition.getAverageCost().multiply(trade.getQuantity())
                : BigDecimal.ZERO;
        BigDecimal sellerGainLoss = sellerProceeds.subtract(sellerCostBasis);

        // Create settlement record
        Settlement settlement = Settlement.builder()
                .trade(trade)
                .buyer(trade.getBuyer())
                .seller(trade.getSeller())
                .asset(trade.getAsset())

                .price(trade.getPrice())
                .quantity(trade.getQuantity())

                .totalValue(trade.getPrice().multiply(trade.getQuantity()))

                .quantityBought(trade.getQuantity())
                .quantitySold(trade.getQuantity())

                .buyerCostBasis(trade.getPrice().multiply(trade.getQuantity()))
                .sellerProceeds(trade.getPrice().multiply(trade.getQuantity()))

                .status(Settlement.SettlementStatus.PENDING)
                .notes("Settlement for trade " + trade.getId())
                .build();

        try {
            // Update buyer position (add assets)
            updateBuyerPosition(trade.getBuyer(), trade.getAsset(), trade.getQuantity(), trade.getPrice());

            // Update seller position (remove assets)
            updateSellerPosition(trade.getSeller(), trade.getAsset(), trade.getQuantity());

            // Mark settlement as completed
            settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
            settlement.setSettledAt(LocalDateTime.now());

            settlement = settlementRepository.save(settlement);

            log.info("Trade {} settled: Buyer {}, Seller {}, Asset {}, Qty {}, Price {}",
                    tradeId, trade.getBuyer().getUsername(), trade.getSeller().getUsername(),
                    trade.getAsset(), trade.getQuantity(), trade.getPrice());

        } catch (Exception e) {
            settlement.setStatus(Settlement.SettlementStatus.FAILED);
            settlement = settlementRepository.save(settlement);
            log.error("Failed to settle trade {}: {}", tradeId, e.getMessage());
            throw new OrderException("SETTLEMENT_FAILED", 400, "Failed to settle trade: " + e.getMessage());
        }

        return SettlementResponse.from(settlement);
    }

    /**
     * Update buyer position when purchasing
     * Uses cost-averaging method
     */
    @Transactional
    private void updateBuyerPosition(User buyer, String asset, BigDecimal quantity, BigDecimal price) {
        var position = positionRepository.findByUserAndAsset(buyer, asset);

        if (position.isPresent()) {
            // Update existing position with cost averaging
            Position p = position.get();
            BigDecimal oldTotal = p.getQuantity().multiply(p.getAverageCost());
            BigDecimal newTotal = quantity.multiply(price);
            BigDecimal totalQuantity = p.getQuantity().add(quantity);
            BigDecimal newAverageCost = (oldTotal.add(newTotal)).divide(totalQuantity, 8, RoundingMode.HALF_UP);

            p.setQuantity(totalQuantity);
            p.setAverageCost(newAverageCost);
            p.setTotalCost(totalQuantity.multiply(newAverageCost));

            positionRepository.save(p);

            log.debug("Updated buyer position: asset={}, qty={}, avgCost={}", asset, totalQuantity, newAverageCost);
        } else {
            // Create new position
            Position newPosition = Position.builder()
                    .user(buyer)
                    .asset(asset)
                    .quantity(quantity)
                    .averageCost(price)
                    .totalCost(quantity.multiply(price))
                    .build();

            positionRepository.save(newPosition);

            log.debug("Created new buyer position: asset={}, qty={}, cost={}", asset, quantity, price);
        }
    }

    /**
     * Update seller position when selling
     * Reduces position, potentially to zero
     */
    @Transactional
    private void updateSellerPosition(User seller, String asset, BigDecimal quantity) {
        var position = positionRepository.findByUserAndAsset(seller, asset);

        if (position.isEmpty()) {
            log.warn("Seller {} has no position in {} to sell", seller.getUsername(), asset);
            throw new OrderException("INSUFFICIENT_POSITION", 400, "Seller has no position in " + asset);
        }

        Position p = position.get();

        if (p.getQuantity().compareTo(quantity) < 0) {
            log.warn("Seller {} insufficient position: have {}, selling {}",
                    seller.getUsername(), p.getQuantity(), quantity);
            throw new OrderException("INSUFFICIENT_POSITION", 400,
                    "Insufficient position. Have: " + p.getQuantity() + ", Selling: " + quantity);
        }

        // Reduce position
        BigDecimal newQuantity = p.getQuantity().subtract(quantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // Delete position if zero quantity
            positionRepository.delete(p);
            log.debug("Deleted seller position: asset={}", asset);
        } else {
            // Update with new quantity
            p.setQuantity(newQuantity);
            p.setTotalCost(newQuantity.multiply(p.getAverageCost()));
            positionRepository.save(p);
            log.debug("Updated seller position: asset={}, remaining qty={}", asset, newQuantity);
        }
    }

    /**
     * Get user's positions
     */
    @Transactional(readOnly = true)
    public List<PositionResponse> getUserPositions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OrderException("USER_NOT_FOUND", 404, "User not found"));

        List<Position> positions = positionRepository.findByUserOrderByAsset(user);
        return positions.stream()
                .map(PositionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get single position
     */
    @Transactional(readOnly = true)
    public PositionResponse getPosition(Long userId, String asset) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OrderException("USER_NOT_FOUND", 404, "User not found"));

        Position position = positionRepository.findByUserAndAsset(user, asset)
                .orElseThrow(() -> new OrderException("POSITION_NOT_FOUND", 404,
                        "No position in " + asset));

        return PositionResponse.from(position);
    }

    /**
     * Get settlement details
     */
    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new OrderException("SETTLEMENT_NOT_FOUND", 404, "Settlement not found"));

        return SettlementResponse.from(settlement);
    }

    /**
     * Get user's settlements (as buyer or seller)
     */
    @Transactional(readOnly = true)
    public List<SettlementResponse> getUserSettlements(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OrderException("USER_NOT_FOUND", 404, "User not found"));

        List<Settlement> settlements = settlementRepository.findByBuyerOrSellerOrderBySettledAtDesc(user, user);
        return settlements.stream()
                .map(SettlementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get portfolio value including positions and cash
     */
    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long userId, MatchingService matchingService) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OrderException("USER_NOT_FOUND", 404, "User not found"));

        // Get positions
        List<Position> positions = positionRepository.findByUserOrderByAsset(user);

        // Calculate portfolio value and P&L
        BigDecimal portfolioValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal unrealizedPnL = BigDecimal.ZERO;

        List<PositionResponse> positionResponses = positions.stream()
                .map(p -> {
                    BigDecimal marketPrice = matchingService.getMarketPrice(p.getAsset());
                    return PositionResponse.from(p, marketPrice);
                })
                .collect(Collectors.toList());

        for (PositionResponse pos : positionResponses) {
            if (pos.getCurrentValue() != null) {
                portfolioValue = portfolioValue.add(pos.getCurrentValue());
            }
            if (pos.getTotalCost() != null) {
                totalInvested = totalInvested.add(pos.getTotalCost());
            }
            if (pos.getUnrealizedPnL() != null) {
                unrealizedPnL = unrealizedPnL.add(pos.getUnrealizedPnL());
            }
        }

        return PortfolioResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .cashBalance(user.getBalance())
                .portfolioValue(portfolioValue)
                .unrealizedPnL(unrealizedPnL)
                .totalInvested(totalInvested)
                .positionCount(positions.size())
                .positions(positionResponses)
                .build();
    }

    /**
     * Get P&L realized gains/losses
     */
    @Transactional(readOnly = true)
    public BigDecimal getRealizingGainLoss(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OrderException("USER_NOT_FOUND", 404, "User not found"));

        List<Settlement> settlements = settlementRepository.findBySellerOrderBySettledAtDesc(user);
        return settlements.stream()
                .map(Settlement::getSellerGainLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get pending settlements
     */
    @Transactional(readOnly = true)
    public List<SettlementResponse> getPendingSettlements() {
        List<Settlement> settlements = settlementRepository.findByStatus(Settlement.SettlementStatus.PENDING);
        return settlements.stream()
                .map(SettlementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Batch execute all pending trades
     * Called periodically to settle all outstanding trades
     */
    @Transactional
    public List<SettlementResponse> executeAllPendingTrades() {
        // Get all unsettled trades
        List<Trade> unsettledTrades = tradeRepository.findAll().stream()
                .filter(t -> settlementRepository.findByTradeId(t.getId()).isEmpty())
                .collect(Collectors.toList());

        List<SettlementResponse> results = unsettledTrades.stream()
                .map(trade -> {
                    try {
                        return executeTrade(trade.getId());
                    } catch (Exception e) {
                        log.error("Failed to execute trade {}: {}", trade.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());

        log.info("Executed {} pending trades", results.size());
        return results;
    }
}
