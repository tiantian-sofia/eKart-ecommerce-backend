package com.vedasole.ekartecommercebackend.repository;

import com.vedasole.ekartecommercebackend.entity.FlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlashSaleRepo extends JpaRepository<FlashSale, Long> {

    @Query("SELECT fs FROM FlashSale fs WHERE fs.startTime <= :now AND fs.endTime > :now")
    List<FlashSale> findActiveFlashSales(@Param("now") LocalDateTime now);

    @Query("SELECT fs FROM FlashSale fs WHERE fs.startTime > :now")
    List<FlashSale> findUpcomingFlashSales(@Param("now") LocalDateTime now);
}
