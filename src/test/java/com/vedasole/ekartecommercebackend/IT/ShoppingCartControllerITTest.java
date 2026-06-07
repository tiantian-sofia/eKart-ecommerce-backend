package com.vedasole.ekartecommercebackend.IT;

import com.vedasole.ekartecommercebackend.entity.Category;
import com.vedasole.ekartecommercebackend.entity.Customer;
import com.vedasole.ekartecommercebackend.entity.Product;
import com.vedasole.ekartecommercebackend.entity.ShoppingCart;
import com.vedasole.ekartecommercebackend.payload.ProductDto;
import com.vedasole.ekartecommercebackend.payload.ShoppingCartDto;
import com.vedasole.ekartecommercebackend.payload.ShoppingCartItemDto;
import com.vedasole.ekartecommercebackend.repository.CategoryRepo;
import com.vedasole.ekartecommercebackend.repository.CustomerRepo;
import com.vedasole.ekartecommercebackend.repository.ProductRepo;
import com.vedasole.ekartecommercebackend.repository.ShoppingCartRepo;
import com.vedasole.ekartecommercebackend.repository.ShoppingCartItemRepo;
import com.vedasole.ekartecommercebackend.utility.TestApplicationInitializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShoppingCartControllerITTest {

    @Autowired
    private TestApplicationInitializer testApplicationInitializer;
    @Autowired
    private CategoryRepo categoryRepo;
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private CustomerRepo customerRepo;
    @Autowired
    private ShoppingCartRepo shoppingCartRepo;
    @Autowired
    private ShoppingCartItemRepo shoppingCartItemRepo;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @LocalServerPort
    private int port;

    private static RestTemplate restTemplate;

    private String baseUrl;
    private long testCategoryId;
    private long testProductId;
    private long testProduct2Id;
    private long testCustomerId;
    private Product testProductSnapshot;
    private Product testProduct2Snapshot;

    @BeforeAll
    static void init() {
        restTemplate = new RestTemplate();
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/shopping-cart";

        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer " + testApplicationInitializer.getUserToken());
            return execution.execute(request, body);
        }));

        transactionTemplate.executeWithoutResult(status -> {
            shoppingCartItemRepo.deleteAllInBatch();
            shoppingCartRepo.deleteAllInBatch();
            productRepo.deleteAllInBatch();
            categoryRepo.deleteAllInBatch();

            Category category = categoryRepo.save(Category.builder()
                    .name("Electronics")
                    .image("/images/electronics.jpg")
                    .desc("Electronic products")
                    .active(true)
                    .build());
            testCategoryId = category.getCategoryId();

            Product p1 = productRepo.save(Product.builder()
                    .name("Laptop")
                    .image("/images/laptop.jpg")
                    .sku("LAP-INT-" + System.nanoTime())
                    .desc("A test laptop")
                    .price(1000.0)
                    .discount(10.0)
                    .qtyInStock(50)
                    .category(category)
                    .build());
            testProductId = p1.getProductId();
            testProductSnapshot = p1;

            Product p2 = productRepo.save(Product.builder()
                    .name("Phone")
                    .image("/images/phone.jpg")
                    .sku("PHN-INT-" + System.nanoTime())
                    .desc("A test phone")
                    .price(500.0)
                    .discount(5.0)
                    .qtyInStock(100)
                    .category(category)
                    .build());
            testProduct2Id = p2.getProductId();
            testProduct2Snapshot = p2;

            Customer customer = customerRepo.findByEmail("normal-user@ekart.com")
                    .orElseThrow(() -> new IllegalStateException("Normal user not found"));
            testCustomerId = customer.getCustomerId();
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            shoppingCartItemRepo.deleteAllInBatch();
            shoppingCartRepo.deleteAllInBatch();
            productRepo.deleteAllInBatch();
            categoryRepo.deleteAllInBatch();
        });
    }

    // ==================== Helper ====================

    private ProductDto toProductDto(Product product) {
        return ProductDto.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .image(product.getImage())
                .desc(product.getDesc())
                .price(product.getPrice())
                .discount(product.getDiscount())
                .qtyInStock(product.getQtyInStock())
                .categoryId(testCategoryId)
                .build();
    }

    @SuppressWarnings("unchecked")
    private long createCartViaApi(ProductDto productDto, long quantity) {
        ShoppingCartItemDto itemDto = ShoppingCartItemDto.builder()
                .product(productDto)
                .quantity(quantity)
                .build();
        ShoppingCartDto cartDto = ShoppingCartDto.builder()
                .customerId(testCustomerId)
                .shoppingCartItems(List.of(itemDto))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ShoppingCartDto> request = new HttpEntity<>(cartDto, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/customer/" + testCustomerId, request, Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        return transactionTemplate.execute(s -> {
            ShoppingCart cart = shoppingCartRepo.findByCustomer_CustomerId(testCustomerId).orElseThrow();
            return cart.getCartId();
        });
    }

    /**
     * Reads cart data inside a transaction to avoid LazyInitializationException.
     * Returns [itemCount, totalQuantity, total, discount].
     */
    private CartSnapshot readCartSnapshot(long cartId) {
        return transactionTemplate.execute(s -> {
            ShoppingCart cart = shoppingCartRepo.findById(cartId).orElseThrow();
            int itemCount = cart.getShoppingCartItems().size();
            long totalQty = cart.getShoppingCartItems().stream().mapToLong(i -> i.getQuantity()).sum();
            double total = cart.getTotal();
            double discount = cart.getDiscount();
            return new CartSnapshot(itemCount, totalQty, total, discount);
        });
    }

    private record CartSnapshot(int itemCount, long totalQty, double total, double discount) {}

    // ==================== Create Cart ====================

    @Test
    @Order(1)
    @DisplayName("POST /customer/{customerId} - should create a new cart with items")
    void testCreateShoppingCart() {
        ProductDto productDto = toProductDto(testProductSnapshot);
        long cartId = createCartViaApi(productDto, 2);

        CartSnapshot snapshot = readCartSnapshot(cartId);
        assertThat(snapshot.itemCount()).isEqualTo(1);
        assertThat(snapshot.totalQty()).isEqualTo(2);
    }

    // ==================== Add to Cart - Bug Fix Verification ====================

    @Test
    @Order(2)
    @DisplayName("PUT /{cartId} - adding same product twice should ACCUMULATE quantity, not overwrite")
    void testAddSameProductTwiceShouldAccumulateQuantity() {
        ProductDto productDto = toProductDto(testProductSnapshot);

        // Step 1: Create cart with 3 units via POST
        long cartId = createCartViaApi(productDto, 3);

        // Verify initial state: 3 units
        CartSnapshot initial = readCartSnapshot(cartId);
        assertThat(initial.itemCount()).isEqualTo(1);
        assertThat(initial.totalQty()).isEqualTo(3);

        // Step 2: Add 2 MORE units of the same product via PUT
        ShoppingCartItemDto addMoreDto = ShoppingCartItemDto.builder()
                .cartId(cartId)
                .product(productDto)
                .quantity(2)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ShoppingCartItemDto> addRequest = new HttpEntity<>(addMoreDto, headers);

        ResponseEntity<Map> addResponse = restTemplate.exchange(
                baseUrl + "/" + cartId, HttpMethod.PUT, addRequest, Map.class
        );
        assertThat(addResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 3: Verify quantity was ACCUMULATED (3 + 2 = 5), NOT overwritten to 2
        CartSnapshot afterAdd = readCartSnapshot(cartId);
        assertThat(afterAdd.itemCount())
                .as("Should still be 1 item (same product)")
                .isEqualTo(1);
        assertThat(afterAdd.totalQty())
                .as("Quantity should be 5 (3 + 2), not 2 (overwritten). Bug fix verification.")
                .isEqualTo(5);

        // Verify totals: price=1000, quantity=5, discount=10%
        // Total: 1000 * 5 = 5000, Discount: 10% * 5000 = 500
        assertThat(afterAdd.total()).isEqualTo(5000.0);
        assertThat(afterAdd.discount()).isEqualTo(500.0);
    }

    @Test
    @Order(3)
    @DisplayName("PUT /{cartId} - adding different products should create separate cart items")
    void testAddDifferentProductsCreatesSeparateItems() {
        ProductDto laptopDto = toProductDto(testProductSnapshot);
        ProductDto phoneDto = toProductDto(testProduct2Snapshot);

        // Create cart with laptop (2 units)
        long cartId = createCartViaApi(laptopDto, 2);

        // Add phone (3 units) via PUT
        ShoppingCartItemDto phoneItemDto = ShoppingCartItemDto.builder()
                .cartId(cartId)
                .product(phoneDto)
                .quantity(3)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(baseUrl + "/" + cartId,
                HttpMethod.PUT, new HttpEntity<>(phoneItemDto, headers), Map.class);

        // Verify: 2 separate items with correct quantities
        CartSnapshot snapshot = readCartSnapshot(cartId);
        assertThat(snapshot.itemCount()).isEqualTo(2);

        // Verify individual quantities via per-product query
        long[] quantities = transactionTemplate.execute(s -> {
            ShoppingCart cart = shoppingCartRepo.findById(cartId).orElseThrow();
            long laptopQty = cart.getShoppingCartItems().stream()
                    .filter(item -> item.getProduct().getProductId() == testProductId)
                    .findFirst().orElseThrow().getQuantity();
            long phoneQty = cart.getShoppingCartItems().stream()
                    .filter(item -> item.getProduct().getProductId() == testProduct2Id)
                    .findFirst().orElseThrow().getQuantity();
            return new long[]{laptopQty, phoneQty};
        });

        assertThat(quantities[0]).isEqualTo(2);
        assertThat(quantities[1]).isEqualTo(3);

        // Total: (1000 * 2) + (500 * 3) = 3500
        assertThat(snapshot.total()).isEqualTo(3500.0);
    }

    @Test
    @Order(4)
    @DisplayName("PUT /{cartId} - adding same product three times should accumulate correctly")
    void testMultipleAccumulationsOfSameProduct() {
        ProductDto productDto = toProductDto(testProductSnapshot);

        // Create cart with 1 unit
        long cartId = createCartViaApi(productDto, 1);

        // Add 1 more unit two more times via PUT
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        for (int i = 0; i < 2; i++) {
            ShoppingCartItemDto addDto = ShoppingCartItemDto.builder()
                    .cartId(cartId)
                    .product(productDto)
                    .quantity(1)
                    .build();
            restTemplate.exchange(baseUrl + "/" + cartId,
                    HttpMethod.PUT, new HttpEntity<>(addDto, headers), Map.class);
        }

        // Verify: 1 + 1 + 1 = 3
        CartSnapshot snapshot = readCartSnapshot(cartId);
        assertThat(snapshot.itemCount()).isEqualTo(1);
        assertThat(snapshot.totalQty())
                .as("Adding 1 unit three times should result in quantity 3")
                .isEqualTo(3);
    }

    // ==================== Get Cart ====================

    @Test
    @Order(5)
    @DisplayName("GET /customer/{customerId} - should return cart for existing customer")
    void testGetShoppingCart() {
        ProductDto productDto = toProductDto(testProductSnapshot);
        long cartId = createCartViaApi(productDto, 1);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/customer/" + testCustomerId,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(((Number) response.getBody().get("cartId")).longValue())
                .isEqualTo(cartId);
    }

    // ==================== Delete Cart ====================

    @Test
    @Order(6)
    @DisplayName("DELETE /customer/{customerId} - should delete the cart and all its items")
    void testDeleteShoppingCart() {
        ProductDto productDto = toProductDto(testProductSnapshot);
        long cartId = createCartViaApi(productDto, 2);

        // Verify cart and items exist before deletion
        boolean cartExistsBefore = transactionTemplate.execute(
                s -> shoppingCartRepo.findByCustomer_CustomerId(testCustomerId).isPresent()
        );
        int itemCountBefore = transactionTemplate.execute(
                s -> shoppingCartItemRepo.findAllByShoppingCartCartId(cartId).size()
        );
        assertThat(cartExistsBefore).as("Cart should exist before deletion").isTrue();
        assertThat(itemCountBefore).as("Cart should have items before deletion").isGreaterThan(0);

        // Use admin token for delete
        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer " + testApplicationInitializer.getAdminToken());
            return execution.execute(request, body);
        }));

        // DELETE the cart
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/customer/" + testCustomerId,
                HttpMethod.DELETE, null, Void.class
        );
        System.out.println("DELETE response status: " + response.getStatusCode());
        assertThat(response.getStatusCode())
                .as("Delete should return 204 No Content")
                .isEqualTo(HttpStatus.NO_CONTENT);

        // Verify cart is gone from the database
        boolean cartExistsAfter = transactionTemplate.execute(
                s -> shoppingCartRepo.findByCustomer_CustomerId(testCustomerId).isPresent()
        );
        assertThat(cartExistsAfter)
                .as("Cart should be deleted from the database")
                .isFalse();

        // Verify cart items are also gone
        int itemCountAfter = transactionTemplate.execute(
                s -> shoppingCartItemRepo.findAllByShoppingCartCartId(cartId).size()
        );
        assertThat(itemCountAfter)
                .as("Cart items should be deleted along with the cart")
                .isEqualTo(0);

        // Verify GET endpoint now returns 404
        ResponseEntity<Map> getResponse = restTemplate.getForEntity(
                baseUrl + "/customer/" + testCustomerId,
                Map.class
        );
        assertThat(getResponse.getStatusCode())
                .as("GET should return 404 after cart is deleted")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /customer/{customerId} - should return 404 when cart does not exist")
    void testDeleteShoppingCartNotFound() {
        // No cart created for this customer
        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer " + testApplicationInitializer.getAdminToken());
            return execution.execute(request, body);
        }));

        // RestTemplate throws HttpClientErrorException for 4xx responses
        org.springframework.web.client.HttpClientErrorException thrown =
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.springframework.web.client.HttpClientErrorException.class,
                        () -> restTemplate.exchange(
                                baseUrl + "/customer/" + testCustomerId,
                                HttpMethod.DELETE, null, Map.class
                        )
                );
        assertThat(thrown.getStatusCode())
                .as("DELETE should return 404 when no cart exists")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /customer/{customerId} - should handle deleting empty cart (no items)")
    void testDeleteEmptyShoppingCart() {
        // Create cart via POST with an item, then delete items to make it empty,
        // or create via API and verify deletion works even after items are removed.
        ProductDto productDto = toProductDto(testProductSnapshot);
        long cartId = createCartViaApi(productDto, 1);

        // First delete the items from the cart via the items endpoint
        List<ShoppingCartItemDto> items = transactionTemplate.execute(s -> {
            return shoppingCartItemRepo.findAllByShoppingCartCartId(cartId).stream()
                    .map(item -> ShoppingCartItemDto.builder()
                            .cartItemId(item.getCartItemId())
                            .cartId(cartId)
                            .build())
                    .toList();
        });

        // Delete each item via DELETE /shopping-cart/{cartId}/items/{cartItemId}
        String itemsBaseUrl = baseUrl + "/" + cartId + "/items";
        for (ShoppingCartItemDto item : items) {
            restTemplate.delete(itemsBaseUrl + "/" + item.getCartItemId());
        }

        // Verify cart exists but has no items
        boolean cartExists = transactionTemplate.execute(
                s -> shoppingCartRepo.findByCustomer_CustomerId(testCustomerId).isPresent()
        );
        int itemCount = transactionTemplate.execute(
                s -> shoppingCartItemRepo.findAllByShoppingCartCartId(cartId).size()
        );
        assertThat(cartExists).as("Cart should exist").isTrue();
        assertThat(itemCount).as("Cart should have no items").isEqualTo(0);

        // Now delete the empty cart
        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer " + testApplicationInitializer.getAdminToken());
            return execution.execute(request, body);
        }));

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/customer/" + testCustomerId,
                HttpMethod.DELETE, null, Void.class
        );
        assertThat(response.getStatusCode())
                .as("Delete empty cart should return 204")
                .isEqualTo(HttpStatus.NO_CONTENT);

        // Verify cart is gone
        boolean cartExistsAfter = transactionTemplate.execute(
                s -> shoppingCartRepo.findByCustomer_CustomerId(testCustomerId).isPresent()
        );
        assertThat(cartExistsAfter)
                .as("Empty cart should be deleted")
                .isFalse();
    }
}
