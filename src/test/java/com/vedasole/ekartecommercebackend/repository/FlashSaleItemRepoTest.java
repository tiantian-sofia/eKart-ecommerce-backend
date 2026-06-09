package com.vedasole.ekartecommercebackend.repository;

import com.vedasole.ekartecommercebackend.entity.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FlashSaleItemRepoTest {

    @Autowired
    private FlashSaleItemRepo underTest;

    @Autowired
    private FlashSaleRepo flashSaleRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    private FlashSale flashSale;
    private FlashSaleItem item;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .name("Test Category")
                .image("img.jpg")
                .desc("desc")
                .active(true)
                .build();
        category = categoryRepo.save(category);

        Product product = Product.builder()
                .name("Test Product")
                .image("img.jpg")
                .sku("SKU-FSI-001")
                .desc("Test desc")
                .price(100.0)
                .discount(0.0)
                .qtyInStock(50)
                .category(category)
                .build();
        product = productRepo.save(product);

        flashSale = FlashSale.builder()
                .name("Test Sale")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        flashSale = flashSaleRepo.save(flashSale);

        item = FlashSaleItem.builder()
                .flashSale(flashSale)
                .product(product)
                .flashPrice(50.0)
                .stock(10)
                .limitPerCustomer(3)
                .build();
        item = underTest.save(item);
    }

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
        flashSaleRepo.deleteAll();
        productRepo.deleteAll();
        categoryRepo.deleteAll();
    }

    @Test
    void deductStock_success() {
        // When
        int rowsAffected = underTest.deductStock(item.getFlashSaleItemId(), 3);

        // Then
        assertThat(rowsAffected).isEqualTo(1);
        FlashSaleItem updated = underTest.findById(item.getFlashSaleItemId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(7);
    }

    @Test
    void deductStock_insufficientStock_returnsZero() {
        // When - request 15 but only 10 available
        int rowsAffected = underTest.deductStock(item.getFlashSaleItemId(), 15);

        // Then
        assertThat(rowsAffected).isEqualTo(0);
        FlashSaleItem updated = underTest.findById(item.getFlashSaleItemId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(10); // Unchanged
    }

    @Test
    void deductStock_exactStock_success() {
        // When - request exactly 10 (all available)
        int rowsAffected = underTest.deductStock(item.getFlashSaleItemId(), 10);

        // Then
        assertThat(rowsAffected).isEqualTo(1);
        FlashSaleItem updated = underTest.findById(item.getFlashSaleItemId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(0);
    }

    @Test
    void deductStock_zeroStock_returnsZero() {
        // Given - set stock to 0
        item.setStock(0);
        underTest.save(item);

        // When
        int rowsAffected = underTest.deductStock(item.getFlashSaleItemId(), 1);

        // Then
        assertThat(rowsAffected).isEqualTo(0);
    }

    @Test
    void deductStock_sequentialDeductions() {
        // Deduct 3
        int first = underTest.deductStock(item.getFlashSaleItemId(), 3);
        assertThat(first).isEqualTo(1);

        // Deduct 5 more
        int second = underTest.deductStock(item.getFlashSaleItemId(), 5);
        assertThat(second).isEqualTo(1);

        // Stock should now be 2
        FlashSaleItem updated = underTest.findById(item.getFlashSaleItemId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(2);

        // Try to deduct 3 more - should fail
        int third = underTest.deductStock(item.getFlashSaleItemId(), 3);
        assertThat(third).isEqualTo(0);

        // Stock still 2
        updated = underTest.findById(item.getFlashSaleItemId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(2);
    }

    @Test
    void findAllByFlashSale_FlashSaleId_returnsItems() {
        // When
        List<FlashSaleItem> items = underTest.findAllByFlashSale_FlashSaleId(flashSale.getFlashSaleId());

        // Then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getFlashSaleItemId()).isEqualTo(item.getFlashSaleItemId());
    }

    @Test
    void findByFlashSale_FlashSaleIdAndFlashSaleItemId_returnsItem() {
        // When
        Optional<FlashSaleItem> found = underTest.findByFlashSale_FlashSaleIdAndFlashSaleItemId(
                flashSale.getFlashSaleId(), item.getFlashSaleItemId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFlashPrice()).isEqualTo(50.0);
    }

    @Test
    void findByFlashSale_FlashSaleIdAndFlashSaleItemId_notFound() {
        // When
        Optional<FlashSaleItem> found = underTest.findByFlashSale_FlashSaleIdAndFlashSaleItemId(
                flashSale.getFlashSaleId(), 999L);

        // Then
        assertThat(found).isEmpty();
    }
}
