package com.vedasole.ekartecommercebackend.repository;

import com.vedasole.ekartecommercebackend.entity.FlashSaleItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FlashSaleItemRepo extends JpaRepository<FlashSaleItem, Long> {

    List<FlashSaleItem> findByCampaign_CampaignId(long campaignId);

    Optional<FlashSaleItem> findByCampaign_CampaignIdAndProduct_ProductId(
            long campaignId, long productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fsi FROM FlashSaleItem fsi WHERE fsi.flashSaleItemId = :id")
    Optional<FlashSaleItem> findByIdForUpdate(@Param("id") long id);
}
