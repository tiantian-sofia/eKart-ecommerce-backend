package com.vedasole.ekartecommercebackend.repository;

import com.vedasole.ekartecommercebackend.entity.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FlashSalePurchaseRepoTest {

    @Autowired
    private FlashSalePurchaseRepo underTest;

    @Autowired
    private FlashSaleItemRepo flashSaleItemRepo;

    @Autowired
    private FlashSaleRepo flashSaleRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private UserRepo userRepo;

    private FlashSaleItem flashSaleItem;
    private Customer customer;

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
                .sku("SKU-FSP-001")
                .desc("Test desc")
                .price(100.0)
                .discount(0.0)
                .qtyInStock(50)
                .category(category)
                .build();
        product = productRepo.save(product);

        FlashSale flashSale = FlashSale.builder()
                .name("Test Sale")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        flashSale = flashSaleRepo.save(flashSale);

        flashSaleItem = FlashSaleItem.builder()
                .flashSale(flashSale)
                .product(product)
                .flashPrice(50.0)
                .stock(10)
                .limitPerCustomer(5)
                .build();
        flashSaleItem = flashSaleItemRepo.save(flashSaleItem);

        User user = User.builder()
                .email("fsp-test@test.com")
                .password("password")
                .role(com.vedasole.ekartecommercebackend.utility.AppConstant.Role.USER)
                .build();
        user = userRepo.save(user);

        customer = Customer.builder()
                .firstName("Test")
                .lastName("User")
                .email("fsp-test@test.com")
                .phoneNumber("1234567890")
                .user(user)
                .build();
        customer = customerRepo.save(customer);
    }

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
        flashSaleItemRepo.deleteAll();
        flashSaleRepo.deleteAll();
        customerRepo.deleteAll();
        userRepo.deleteAll();
        productRepo.deleteAll();
        categoryRepo.deleteAll();
    }

    @Test
    void sumQuantity_returnsTotal() {
        // Given
        FlashSalePurchase p1 = FlashSalePurchase.builder()
                .flashSaleItem(flashSaleItem)
                .customer(customer)
                .quantity(2)
                .orderId(1L)
                .flashPriceTotal(100.0)
                .build();
        FlashSalePurchase p2 = FlashSalePurchase.builder()
                .flashSaleItem(flashSaleItem)
                .customer(customer)
                .quantity(1)
                .orderId(2L)
                .flashPriceTotal(50.0)
                .build();
        underTest.save(p1);
        underTest.save(p2);

        // When
        int total = underTest.sumQuantityByFlashSaleItemAndCustomer(
                flashSaleItem.getFlashSaleItemId(), customer.getCustomerId());

        // Then
        assertThat(total).isEqualTo(3);
    }

    @Test
    void sumQuantity_noPurchases_returnsZero() {
        // When
        int total = underTest.sumQuantityByFlashSaleItemAndCustomer(
                flashSaleItem.getFlashSaleItemId(), customer.getCustomerId());

        // Then
        assertThat(total).isEqualTo(0);
    }

    @Test
    void findByOrderId_exists() {
        // Given
        FlashSalePurchase purchase = FlashSalePurchase.builder()
                .flashSaleItem(flashSaleItem)
                .customer(customer)
                .quantity(1)
                .orderId(100L)
                .flashPriceTotal(50.0)
                .build();
        underTest.save(purchase);

        // When
        Optional<FlashSalePurchase> found = underTest.findByOrderId(100L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFlashPriceTotal()).isEqualTo(50.0);
    }

    @Test
    void findByOrderId_notExists() {
        // When
        Optional<FlashSalePurchase> found = underTest.findByOrderId(999L);

        // Then
        assertThat(found).isEmpty();
    }
}
