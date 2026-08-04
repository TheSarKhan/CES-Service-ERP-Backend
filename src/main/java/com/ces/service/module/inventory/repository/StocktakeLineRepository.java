package com.ces.service.module.inventory.repository;

import com.ces.service.module.inventory.entity.StocktakeLine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StocktakeLineRepository extends JpaRepository<StocktakeLine, UUID> {

    List<StocktakeLine> findByStocktakeIdAndDeletedAtIsNull(UUID stocktakeId);

    Optional<StocktakeLine> findByStocktakeIdAndItemIdAndDeletedAtIsNull(UUID stocktakeId, UUID itemId);
}
