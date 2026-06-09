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
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class FlashSaleItemRepoTest {

    @Autowired private FlashSaleItemRepo flashSaleItemRepo;
    @Autowired private FlashSaleCampaignRepo campaignRepo;
    @Autowired private CategoryRepo categoryRepo;
    @Autowired private ProductRepo productRepo;
    @Autowired private TestEntityManager entityManager;

    private FlashSaleCampaign campaign;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        flashSaleItemRepo.deleteAll();
        campaignRepo.deleteAll();
        productRepo.deleteAll();
        categoryRepo.deleteAll();

        Category category = Category.builder().name("Electronics").active(true).build();
        category = categoryRepo.save(category);

        product1 = Product.builder()
                .name("Phone").image("img1.png").sku("ELE-Phone-des")
                .desc("A phone").price(1000.0).discount(0).qtyInStock(100).category(category)
                .build();
        product2 = Product.builder()
                .name("Tablet").image("img2.png").sku("ELE-Table-des")
                .desc("A tablet").price(2000.0).discount(10).qtyInStock(50).category(category)
                .build();
        product1 = productRepo.save(product1);
        product2 = productRepo.save(product2);

        campaign = FlashSaleCampaign.builder()
                .name("Test Sale")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .status(FlashSaleStatus.ACTIVE)
                .build();
        campaign = campaignRepo.save(campaign);
        entityManager.flush();
    }

    @Test
    void findByIdForUpdate_shouldReturnItemWhenExists() {
        FlashSaleItem item = FlashSaleItem.builder()
                .campaign(campaign).product(product1)
                .flashPrice(500.0).totalStock(10).availableStock(10).perUserLimit(2)
                .build();
        item = flashSaleItemRepo.save(item);
        entityManager.flush();

        Optional<FlashSaleItem> found = flashSaleItemRepo.findByIdForUpdate(item.getFlashSaleItemId());

        assertThat(found).isPresent();
        assertThat(found.get().getFlashPrice()).isEqualTo(500.0);
    }

    @Test
    void findByIdForUpdate_shouldReturnEmptyWhenNotExists() {
        Optional<FlashSaleItem> found = flashSaleItemRepo.findByIdForUpdate(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByCampaignId_shouldReturnAllItems() {
        FlashSaleItem item1 = FlashSaleItem.builder()
                .campaign(campaign).product(product1)
                .flashPrice(500.0).totalStock(10).availableStock(10).perUserLimit(2)
                .build();
        FlashSaleItem item2 = FlashSaleItem.builder()
                .campaign(campaign).product(product2)
                .flashPrice(1500.0).totalStock(5).availableStock(5).perUserLimit(1)
                .build();
        flashSaleItemRepo.saveAll(List.of(item1, item2));
        entityManager.flush();

        List<FlashSaleItem> items = flashSaleItemRepo.findByCampaign_CampaignId(campaign.getCampaignId());

        assertThat(items).hasSize(2);
    }

    @Test
    void findByCampaignAndProduct_shouldReturnItem() {
        FlashSaleItem item = FlashSaleItem.builder()
                .campaign(campaign).product(product1)
                .flashPrice(500.0).totalStock(10).availableStock(10).perUserLimit(2)
                .build();
        flashSaleItemRepo.save(item);
        entityManager.flush();

        Optional<FlashSaleItem> found = flashSaleItemRepo
                .findByCampaign_CampaignIdAndProduct_ProductId(
                        campaign.getCampaignId(), product1.getProductId());

        assertThat(found).isPresent();
        assertThat(found.get().getFlashPrice()).isEqualTo(500.0);
    }

    @Test
    void findByCampaignAndProduct_shouldReturnEmptyWhenNotExists() {
        Optional<FlashSaleItem> found = flashSaleItemRepo
                .findByCampaign_CampaignIdAndProduct_ProductId(campaign.getCampaignId(), 999L);
        assertThat(found).isEmpty();
    }

    @Test
    void uniqueConstraint_shouldRejectDuplicateCampaignProduct() {
        FlashSaleItem item1 = FlashSaleItem.builder()
                .campaign(campaign).product(product1)
                .flashPrice(500.0).totalStock(10).availableStock(10).perUserLimit(2)
                .build();
        flashSaleItemRepo.save(item1);
        entityManager.flush();

        FlashSaleItem item2 = FlashSaleItem.builder()
                .campaign(campaign).product(product1)
                .flashPrice(600.0).totalStock(5).availableStock(5).perUserLimit(1)
                .build();

        assertThrows(Exception.class, () -> {
            flashSaleItemRepo.save(item2);
            entityManager.flush();
        });
    }
}
