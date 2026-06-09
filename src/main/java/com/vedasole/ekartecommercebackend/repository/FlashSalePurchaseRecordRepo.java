package com.vedasole.ekartecommercebackend.repository;

import com.vedasole.ekartecommercebackend.entity.FlashSalePurchaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlashSalePurchaseRecordRepo extends JpaRepository<FlashSalePurchaseRecord, Long> {

    Optional<FlashSalePurchaseRecord> findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(
            long flashSaleItemId, long customerId
    );
}
