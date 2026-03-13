package com.finsimx.repository;

import com.finsimx.entity.Position;
import com.finsimx.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByUserOrderByAsset(User user);

    Optional<Position> findByUserAndAsset(User user, String asset);

    List<Position> findByUserAndQuantityGreaterThan(User user, java.math.BigDecimal quantity);

    int countByUser(User user);
}
