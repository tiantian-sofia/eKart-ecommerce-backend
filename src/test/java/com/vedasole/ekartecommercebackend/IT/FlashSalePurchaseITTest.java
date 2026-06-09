package com.vedasole.ekartecommercebackend.IT;

import com.stripe.model.checkout.Session;
import com.vedasole.ekartecommercebackend.config.TestMailConfig;
import com.vedasole.ekartecommercebackend.entity.*;
import com.vedasole.ekartecommercebackend.exception.APIException;
import com.vedasole.ekartecommercebackend.repository.*;
import com.vedasole.ekartecommercebackend.service.service_impl.StripeService;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSalePurchaseService;
import com.vedasole.ekartecommercebackend.utility.AppConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestMailConfig.class)
class FlashSalePurchaseITTest {

    @Autowired
    private FlashSalePurchaseService flashSalePurchaseService;

    @Autowired
    private FlashSaleRepo flashSaleRepo;
    @Autowired
    private FlashSaleItemRepo flashSaleItemRepo;
    @Autowired
    private FlashSalePurchaseRepo flashSalePurchaseRepo;
    @Autowired
    private CustomerRepo customerRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private CategoryRepo categoryRepo;
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private AddressRepo addressRepo;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private StripeService stripeService;

    private FlashSale flashSale;
    private FlashSaleItem flashSaleItem;
    private List<Customer> customers = new ArrayList<>();
    private List<User> users = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        // Mock Stripe
        Session mockSession = org.mockito.Mockito.mock(Session.class);
        given(mockSession.getUrl()).willReturn("https://checkout.stripe.com/test-session");
        given(stripeService.createFlashSaleCheckoutSession(any(), anyDouble(), anyLong(), any(), any()))
                .willReturn(mockSession);

        // Use TransactionTemplate to ensure all setup happens in one transaction
        // This avoids the "detached entity passed to persist" issue with Product→Category cascade
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            Category category = Category.builder()
                    .name("IT Test Category")
                    .image("img.jpg")
                    .desc("desc")
                    .active(true)
                    .build();
            category = categoryRepo.save(category);

            Product product = Product.builder()
                    .name("IT Test Product")
                    .image("img.jpg")
                    .sku("SKU-IT-001-" + System.nanoTime())
                    .desc("Test desc")
                    .price(100.0)
                    .discount(0.0)
                    .qtyInStock(100)
                    .category(category)
                    .build();
            product = productRepo.save(product);

            flashSale = FlashSale.builder()
                    .name("IT Test Sale")
                    .startTime(LocalDateTime.now().minusHours(1))
                    .endTime(LocalDateTime.now().plusHours(1))
                    .build();
            flashSale = flashSaleRepo.save(flashSale);

            flashSaleItem = FlashSaleItem.builder()
                    .flashSale(flashSale)
                    .product(product)
                    .flashPrice(50.0)
                    .stock(5)
                    .limitPerCustomer(2)
                    .build();
            flashSaleItem = flashSaleItemRepo.save(flashSaleItem);

            for (int i = 0; i < 10; i++) {
                User user = User.builder()
                        .email("it-test-" + i + "-" + System.nanoTime() + "@test.com")
                        .password("password")
                        .role(AppConstant.Role.USER)
                        .build();
                user = userRepo.save(user);
                users.add(user);

                Customer customer = Customer.builder()
                        .firstName("Test" + i)
                        .lastName("User" + i)
                        .email(user.getEmail())
                        .phoneNumber("123456789" + i)
                        .user(user)
                        .build();
                customer = customerRepo.save(customer);
                customers.add(customer);
            }
            return null;
        });

        // Re-fetch entities so they are managed in subsequent operations
        flashSale = flashSaleRepo.findById(flashSale.getFlashSaleId()).orElseThrow();
        flashSaleItem = flashSaleItemRepo.findById(flashSaleItem.getFlashSaleItemId()).orElseThrow();
    }

    @AfterEach
    void tearDown() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            flashSalePurchaseRepo.deleteAll();
            orderRepo.deleteAll();
            flashSaleItemRepo.deleteAll();
            flashSaleRepo.deleteAll();
            customerRepo.deleteAll();
            userRepo.deleteAll();
            productRepo.deleteAll();
            categoryRepo.deleteAll();
            addressRepo.deleteAll();
            return null;
        });
        customers.clear();
        users.clear();
    }

    @Test
    void fullPurchaseFlow_success() {
        Customer customer = customers.get(0);

        String checkoutUrl = flashSalePurchaseService.purchaseFlashSaleItem(
                flashSale.getFlashSaleId(),
                flashSaleItem.getFlashSaleItemId(),
                1,
                customer.getCustomerId()
        );

        assertThat(checkoutUrl).isEqualTo("https://checkout.stripe.com/test-session");

        // Verify stock was deducted
        FlashSaleItem updatedItem = flashSaleItemRepo.findById(flashSaleItem.getFlashSaleItemId()).orElseThrow();
        assertThat(updatedItem.getStock()).isEqualTo(4);

        // Verify purchase record exists
        int purchased = flashSalePurchaseRepo.sumQuantityByFlashSaleItemAndCustomer(
                flashSaleItem.getFlashSaleItemId(), customer.getCustomerId());
        assertThat(purchased).isEqualTo(1);

        // Verify order was created
        assertThat(orderRepo.findAllByCustomer_CustomerId(customer.getCustomerId())).isNotEmpty();
    }

    @Test
    void purchaseOutsideActiveTime_rejected() {
        // Create a future flash sale in a transaction
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        long[] ids = txTemplate.execute(status -> {
            Product product = productRepo.findAll().get(0);
            FlashSale futureSale = FlashSale.builder()
                    .name("Future Sale")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(2))
                    .build();
            futureSale = flashSaleRepo.save(futureSale);

            FlashSaleItem futureItem = FlashSaleItem.builder()
                    .flashSale(futureSale)
                    .product(product)
                    .flashPrice(30.0)
                    .stock(10)
                    .limitPerCustomer(2)
                    .build();
            futureItem = flashSaleItemRepo.save(futureItem);
            return new long[]{futureSale.getFlashSaleId(), futureItem.getFlashSaleItemId()};
        });

        Customer customer = customers.get(0);
        try {
            flashSalePurchaseService.purchaseFlashSaleItem(ids[0], ids[1], 1, customer.getCustomerId());
            assertThat(false).as("Should have thrown APIException").isTrue();
        } catch (APIException e) {
            assertThat(e.getMessage()).contains("not started yet");
        }
    }

    @Test
    void purchaseExpiredSale_rejected() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        long[] ids = txTemplate.execute(status -> {
            Product product = productRepo.findAll().get(0);
            FlashSale expiredSale = FlashSale.builder()
                    .name("Expired Sale")
                    .startTime(LocalDateTime.now().minusDays(2))
                    .endTime(LocalDateTime.now().minusDays(1))
                    .build();
            expiredSale = flashSaleRepo.save(expiredSale);

            FlashSaleItem expiredItem = FlashSaleItem.builder()
                    .flashSale(expiredSale)
                    .product(product)
                    .flashPrice(30.0)
                    .stock(10)
                    .limitPerCustomer(2)
                    .build();
            expiredItem = flashSaleItemRepo.save(expiredItem);
            return new long[]{expiredSale.getFlashSaleId(), expiredItem.getFlashSaleItemId()};
        });

        Customer customer = customers.get(0);
        try {
            flashSalePurchaseService.purchaseFlashSaleItem(ids[0], ids[1], 1, customer.getCustomerId());
            assertThat(false).as("Should have thrown APIException").isTrue();
        } catch (APIException e) {
            assertThat(e.getMessage()).contains("ended");
        }
    }

    @Test
    void purchaseOverPersonalLimit_rejected() {
        Customer customer = customers.get(0);

        // First purchase: buy 2 (at limit)
        flashSalePurchaseService.purchaseFlashSaleItem(
                flashSale.getFlashSaleId(),
                flashSaleItem.getFlashSaleItemId(),
                2,
                customer.getCustomerId()
        );

        // Second purchase: try to buy 1 more (exceeds limit of 2)
        try {
            flashSalePurchaseService.purchaseFlashSaleItem(
                    flashSale.getFlashSaleId(),
                    flashSaleItem.getFlashSaleItemId(),
                    1,
                    customer.getCustomerId()
            );
            assertThat(false).as("Should have thrown APIException").isTrue();
        } catch (APIException e) {
            assertThat(e.getMessage()).contains("Purchase limit exceeded");
        }
    }

    @Test
    void concurrentPurchases_noOverselling() throws Exception {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        final long saleId = flashSale.getFlashSaleId();
        final long itemId = flashSaleItem.getFlashSaleItemId();

        for (int i = 0; i < threadCount; i++) {
            final long customerId = customers.get(i).getCustomerId();
            executor.submit(() -> {
                try {
                    startLatch.await();
                    flashSalePurchaseService.purchaseFlashSaleItem(saleId, itemId, 1, customerId);
                    successCount.incrementAndGet();
                } catch (APIException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify: exactly 5 succeeded, 5 failed
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);

        // Verify: stock is exactly 0
        FlashSaleItem item = flashSaleItemRepo.findById(itemId).orElseThrow();
        assertThat(item.getStock()).isEqualTo(0);
    }

    @Test
    void purchaseStockDepleted_rejected() {
        Customer customer = customers.get(0);

        // Buy 2
        flashSalePurchaseService.purchaseFlashSaleItem(
                flashSale.getFlashSaleId(),
                flashSaleItem.getFlashSaleItemId(),
                2,
                customer.getCustomerId()
        );

        Customer customer2 = customers.get(1);
        // Buy 2 more
        flashSalePurchaseService.purchaseFlashSaleItem(
                flashSale.getFlashSaleId(),
                flashSaleItem.getFlashSaleItemId(),
                2,
                customer2.getCustomerId()
        );

        Customer customer3 = customers.get(2);
        // Buy 1 more (total now 5, stock = 0)
        flashSalePurchaseService.purchaseFlashSaleItem(
                flashSale.getFlashSaleId(),
                flashSaleItem.getFlashSaleItemId(),
                1,
                customer3.getCustomerId()
        );

        // Stock should now be 0, try to buy 1 more
        Customer customer4 = customers.get(3);
        try {
            flashSalePurchaseService.purchaseFlashSaleItem(
                    flashSale.getFlashSaleId(),
                    flashSaleItem.getFlashSaleItemId(),
                    1,
                    customer4.getCustomerId()
            );
            assertThat(false).as("Should have thrown APIException").isTrue();
        } catch (APIException e) {
            assertThat(e.getMessage()).contains("Insufficient flash sale stock");
        }

        // Verify stock is 0
        FlashSaleItem item = flashSaleItemRepo.findById(flashSaleItem.getFlashSaleItemId()).orElseThrow();
        assertThat(item.getStock()).isEqualTo(0);
    }
}
