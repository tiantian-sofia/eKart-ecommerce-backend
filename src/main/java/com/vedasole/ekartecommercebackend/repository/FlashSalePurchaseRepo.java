package com.vedasole.ekartecommercebackend.repository;

import com.vedasole.ekartecommercebackend.entity.FlashSalePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlashSalePurchaseRepo extends JpaRepository<FlashSalePurchase, Long> {

    @Query("SELECT COALESCE(SUM(fsp.quantity), 0) FROM FlashSalePurchase fsp " +
            "WHERE fsp.flashSaleItem.flashSaleItemId = :itemId AND fsp.customer.customerId = :customerId")
    int sumQuantityByFlashSaleItemAndCustomer(
            @Param("itemId") long itemId,
            @Param("customerId") long customerId
    );

    Optional<FlashSalePurchase> findByOrderId(long orderId);
}
