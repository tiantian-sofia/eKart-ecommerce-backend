package com.vedasole.ekartecommercebackend.repository;

import com.vedasole.ekartecommercebackend.entity.FlashSaleCampaign;
import com.vedasole.ekartecommercebackend.utility.AppConstant.FlashSaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FlashSaleCampaignRepo extends JpaRepository<FlashSaleCampaign, Long> {

    List<FlashSaleCampaign> findByStatus(FlashSaleStatus status);

    @Query("SELECT c FROM FlashSaleCampaign c WHERE c.startTime <= :now AND c.endTime > :now")
    List<FlashSaleCampaign> findActiveCampaigns(@Param("now") LocalDateTime now);
}
