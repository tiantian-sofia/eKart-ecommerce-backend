package com.vedasole.ekartecommercebackend.repository;

import com.vedasole.ekartecommercebackend.entity.FlashSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashSaleItemRepo extends JpaRepository<FlashSaleItem, Long> {

    List<FlashSaleItem> findAllByFlashSale_FlashSaleId(long flashSaleId);

    Optional<FlashSaleItem> findByFlashSale_FlashSaleIdAndFlashSaleItemId(long flashSaleId, long itemId);

    /**
     * Atomically deducts stock. Returns number of rows affected.
     * Returns 0 if stock is less than the requested quantity (oversell prevented at DB level).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FlashSaleItem fsi SET fsi.stock = fsi.stock - :qty WHERE fsi.flashSaleItemId = :itemId AND fsi.stock >= :qty")
    int deductStock(@Param("itemId") long itemId, @Param("qty") int quantity);
}
