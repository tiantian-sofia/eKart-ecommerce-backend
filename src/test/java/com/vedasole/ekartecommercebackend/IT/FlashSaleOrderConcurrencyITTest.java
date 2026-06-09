package com.vedasole.ekartecommercebackend.IT;

import com.vedasole.ekartecommercebackend.config.TestMailConfig;
import com.vedasole.ekartecommercebackend.entity.*;
import com.vedasole.ekartecommercebackend.payload.FlashSaleOrderRequestDto;
import com.vedasole.ekartecommercebackend.repository.*;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSaleService;
import com.vedasole.ekartecommercebackend.utility.AppConstant;
import com.vedasole.ekartecommercebackend.utility.AppConstant.FlashSaleStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestMailConfig.class)
class FlashSaleOrderConcurrencyITTest {

    @Autowired private FlashSaleService flashSaleService;
    @Autowired private FlashSaleCampaignRepo campaignRepo;
    @Autowired private FlashSaleItemRepo flashSaleItemRepo;
    @Autowired private FlashSalePurchaseRecordRepo purchaseRecordRepo;
    @Autowired private ProductRepo productRepo;
    @Autowired private CustomerRepo customerRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private AddressRepo addressRepo;
    @Autowired private OrderRepo orderRepo;

    private FlashSaleItem flashSaleItem;
    private Address address;
    private final List<Customer> testCustomers = new ArrayList<>();
    private Product product;

    @BeforeEach
    void setUp() {
        // Create product with unique SKU per test run (let cascade handle Category)
        Category category = Category.builder().name("FlashTestCat").active(true).build();
        String uniqueSku = "FT-TestP-" + System.nanoTime();
        product = productRepo.save(
                Product.builder()
                        .name("TestProduct").image("img.png").sku(uniqueSku)
                        .desc("desc").price(1000.0).discount(0).qtyInStock(100)
                        .category(category)
                        .build()
        );

        FlashSaleCampaign campaign = campaignRepo.save(
                FlashSaleCampaign.builder()
                        .name("Concurrency Test Sale")
                        .startTime(LocalDateTime.now().minusHours(1))
                        .endTime(LocalDateTime.now().plusHours(1))
                        .status(FlashSaleStatus.ACTIVE)
                        .build()
        );

        flashSaleItem = flashSaleItemRepo.save(
                FlashSaleItem.builder()
                        .campaign(campaign).product(product)
                        .flashPrice(500.0).totalStock(5).availableStock(5).perUserLimit(1)
                        .build()
        );

        address = addressRepo.save(
                new Address("Test Line 1", "Test Line 2", "City", "State", "Country", 100001)
        );

        // Create 10 test customers (let Customer cascade handle User persistence)
        testCustomers.clear();
        for (int i = 0; i < 10; i++) {
            String email = "flashtest_" + System.nanoTime() + "_" + i + "@test.com";
            User user = User.builder()
                    .email(email)
                    .password("password")
                    .role(AppConstant.Role.USER)
                    .build();
            Customer customer = customerRepo.save(
                    Customer.builder()
                            .firstName("Test" + i).lastName("User")
                            .phoneNumber("123456789" + i)
                            .email(email)
                            .user(user)
                            .build()
            );
            testCustomers.add(customer);
        }
    }

    @AfterEach
    void tearDown() {
        purchaseRecordRepo.deleteAll();
        orderRepo.deleteAll();
        flashSaleItemRepo.deleteAll();
        campaignRepo.deleteAll();
        // Delete only our test customers (not the ones from TestApplicationInitializer)
        for (Customer c : testCustomers) {
            customerRepo.deleteById(c.getCustomerId());
        }
        testCustomers.clear();
        productRepo.deleteById(product.getProductId());
    }

    @Test
    void concurrentOrders_shouldNotOversell() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                            .flashSaleItemId(flashSaleItem.getFlashSaleItemId())
                            .customerId(testCustomers.get(idx).getCustomerId())
                            .quantity(1)
                            .addressId(address.getAddressId())
                            .build();
                    flashSaleService.placeFlashSaleOrder(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify: exactly 5 succeed (stock=5), 5 fail
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);

        // Verify stock is exactly 0, never negative
        FlashSaleItem updated = flashSaleItemRepo.findById(flashSaleItem.getFlashSaleItemId()).orElseThrow();
        assertThat(updated.getAvailableStock()).isZero();
    }

    @Test
    void concurrentOrders_sameUser_shouldEnforcePerUserLimit() throws InterruptedException {
        // Reconfigure: stock=10, perUserLimit=2
        flashSaleItem.setTotalStock(10);
        flashSaleItem.setAvailableStock(10);
        flashSaleItem.setPerUserLimit(2);
        flashSaleItem = flashSaleItemRepo.save(flashSaleItem);

        Customer singleCustomer = testCustomers.get(0);
        int threadCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                            .flashSaleItemId(flashSaleItem.getFlashSaleItemId())
                            .customerId(singleCustomer.getCustomerId())
                            .quantity(1)
                            .addressId(address.getAddressId())
                            .build();
                    flashSaleService.placeFlashSaleOrder(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly 2 succeed (perUserLimit=2), 3 fail
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failCount.get()).isEqualTo(3);

        // Verify purchase record
        FlashSalePurchaseRecord record = purchaseRecordRepo
                .findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(
                        flashSaleItem.getFlashSaleItemId(), singleCustomer.getCustomerId())
                .orElseThrow();
        assertThat(record.getQuantityPurchased()).isEqualTo(2);

        // Verify stock decremented by exactly 2
        FlashSaleItem updated = flashSaleItemRepo.findById(flashSaleItem.getFlashSaleItemId()).orElseThrow();
        assertThat(updated.getAvailableStock()).isEqualTo(8);
    }

    @Test
    void concurrentOrders_stockZero_noneShouldSucceed() throws InterruptedException {
        // Set stock to 0
        flashSaleItem.setAvailableStock(0);
        flashSaleItem = flashSaleItemRepo.save(flashSaleItem);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                            .flashSaleItemId(flashSaleItem.getFlashSaleItemId())
                            .customerId(testCustomers.get(idx).getCustomerId())
                            .quantity(1)
                            .addressId(address.getAddressId())
                            .build();
                    flashSaleService.placeFlashSaleOrder(request);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isZero();

        FlashSaleItem updated = flashSaleItemRepo.findById(flashSaleItem.getFlashSaleItemId()).orElseThrow();
        assertThat(updated.getAvailableStock()).isZero();
    }
}
