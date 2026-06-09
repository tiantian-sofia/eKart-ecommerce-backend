package com.vedasole.ekartecommercebackend.repository;

import com.vedasole.ekartecommercebackend.entity.Category;
import com.vedasole.ekartecommercebackend.entity.FlashSaleCampaign;
import com.vedasole.ekartecommercebackend.entity.FlashSaleItem;
import com.vedasole.ekartecommercebackend.entity.Product;
import com.vedasole.ekartecommercebackend.utility.AppConstant.FlashSaleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FlashSaleCampaignRepoTest {

    @Autowired
    private FlashSaleCampaignRepo campaignRepo;

    @Autowired
    private FlashSaleItemRepo flashSaleItemRepo;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private ProductRepo productRepo;

    @BeforeEach
    void setUp() {
        flashSaleItemRepo.deleteAll();
        campaignRepo.deleteAll();
    }

    @Test
    void findActiveCampaigns_shouldReturnOnlyActiveCampaigns() {
        LocalDateTime now = LocalDateTime.now();

        // Past campaign (ended)
        FlashSaleCampaign past = FlashSaleCampaign.builder()
                .name("Past Sale")
                .startTime(now.minusDays(3))
                .endTime(now.minusDays(1))
                .status(FlashSaleStatus.ENDED)
                .build();

        // Current campaign (active)
        FlashSaleCampaign current = FlashSaleCampaign.builder()
                .name("Current Sale")
                .startTime(now.minusHours(1))
                .endTime(now.plusHours(1))
                .status(FlashSaleStatus.ACTIVE)
                .build();

        // Future campaign (not started)
        FlashSaleCampaign future = FlashSaleCampaign.builder()
                .name("Future Sale")
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(3))
                .status(FlashSaleStatus.PENDING)
                .build();

        campaignRepo.saveAll(List.of(past, current, future));
        entityManager.flush();

        List<FlashSaleCampaign> active = campaignRepo.findActiveCampaigns(now);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getName()).isEqualTo("Current Sale");
    }

    @Test
    void findByStatus_shouldFilterCorrectly() {
        FlashSaleCampaign c1 = FlashSaleCampaign.builder()
                .name("Active").startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusDays(1))
                .status(FlashSaleStatus.ACTIVE).build();
        FlashSaleCampaign c2 = FlashSaleCampaign.builder()
                .name("Pending").startTime(LocalDateTime.now().plusDays(1)).endTime(LocalDateTime.now().plusDays(2))
                .status(FlashSaleStatus.PENDING).build();

        campaignRepo.saveAll(List.of(c1, c2));

        List<FlashSaleCampaign> active = campaignRepo.findByStatus(FlashSaleStatus.ACTIVE);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getName()).isEqualTo("Active");
    }

    @Test
    void findActiveCampaigns_shouldReturnEmptyWhenNoneActive() {
        FlashSaleCampaign past = FlashSaleCampaign.builder()
                .name("Past").startTime(LocalDateTime.now().minusDays(3))
                .endTime(LocalDateTime.now().minusDays(1))
                .status(FlashSaleStatus.ENDED).build();
        campaignRepo.save(past);

        List<FlashSaleCampaign> active = campaignRepo.findActiveCampaigns(LocalDateTime.now());
        assertThat(active).isEmpty();
    }
}
