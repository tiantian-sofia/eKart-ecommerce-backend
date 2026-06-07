package com.vedasole.ekartecommercebackend.IT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vedasole.ekartecommercebackend.entity.*;
import com.vedasole.ekartecommercebackend.payload.ProductDto;
import com.vedasole.ekartecommercebackend.payload.ShoppingCartItemDto;
import com.vedasole.ekartecommercebackend.repository.*;
import com.vedasole.ekartecommercebackend.utility.AppConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class ShoppingCartControllerITTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ShoppingCartRepo shoppingCartRepo;
    @Autowired
    private CustomerRepo customerRepo;
    @Autowired
    private CategoryRepo categoryRepo;
    @Autowired
    private ProductRepo productRepo;

    private ShoppingCart shoppingCart;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        // Create category
        Category category = Category.builder()
                .name("Electronics")
                .image("/img/electronics.jpg")
                .desc("Electronic items")
                .active(true)
                .build();
        category = categoryRepo.saveAndFlush(category);

        // Create products
        product1 = Product.builder()
                .name("Phone")
                .image("/img/phone.jpg")
                .sku("PHN-IT-" + System.nanoTime())
                .desc("A smartphone")
                .price(100.0)
                .discount(10.0)
                .qtyInStock(50)
                .category(category)
                .build();
        product1 = productRepo.saveAndFlush(product1);

        product2 = Product.builder()
                .name("Laptop")
                .image("/img/laptop.jpg")
                .sku("LPT-IT-" + System.nanoTime())
                .desc("A laptop")
                .price(500.0)
                .discount(5.0)
                .qtyInStock(30)
                .category(category)
                .build();
        product2 = productRepo.saveAndFlush(product2);

        // Create customer with user
        User user = new User("carttest@email.com", "password", AppConstant.Role.USER);
        Customer customer = Customer.builder()
                .firstName("Cart")
                .lastName("Tester")
                .phoneNumber("1234567890")
                .email("carttest@email.com")
                .user(user)
                .build();
        customer = customerRepo.saveAndFlush(customer);

        // Create shopping cart for customer
        shoppingCart = ShoppingCart.builder()
                .customer(customer)
                .shoppingCartItems(new ArrayList<>())
                .total(0)
                .discount(0)
                .build();
        shoppingCart = shoppingCartRepo.saveAndFlush(shoppingCart);
        customer.setShoppingCart(shoppingCart);
        customerRepo.saveAndFlush(customer);
    }

    @Test
    void shouldAddNewItemToCart() throws Exception {
        ShoppingCartItemDto itemDto = buildItemDto(product1, 3);

        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shoppingCartItems", hasSize(1)))
                .andExpect(jsonPath("$.shoppingCartItems[0].quantity").value(3))
                .andExpect(jsonPath("$.shoppingCartItems[0].product.productId").value(product1.getProductId()));
    }

    @Test
    void shouldAccumulateQuantityWhenAddingSameProductTwice() throws Exception {
        ShoppingCartItemDto firstAdd = buildItemDto(product1, 2);

        // First add
        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstAdd)))
                .andExpect(status().isOk());

        // Second add of same product
        ShoppingCartItemDto secondAdd = buildItemDto(product1, 3);

        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondAdd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shoppingCartItems", hasSize(1)))
                .andExpect(jsonPath("$.shoppingCartItems[0].quantity").value(5));
    }

    @Test
    void shouldAccumulateQuantityOverMultipleAdds() throws Exception {
        // Add 1 + 2 + 4 = 7
        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildItemDto(product1, 1))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildItemDto(product1, 2))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildItemDto(product1, 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shoppingCartItems", hasSize(1)))
                .andExpect(jsonPath("$.shoppingCartItems[0].quantity").value(7));
    }

    @Test
    void shouldHandleDifferentProductsIndependently() throws Exception {
        // Add product1 with quantity 2
        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildItemDto(product1, 2))))
                .andExpect(status().isOk());

        // Add product2 with quantity 3
        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildItemDto(product2, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shoppingCartItems", hasSize(2)));
    }

    @Test
    void shouldCalculateTotalAndDiscountCorrectly() throws Exception {
        // Add product1 (price=100, discount=10%) with quantity 2
        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildItemDto(product1, 2))))
                .andExpect(status().isOk());

        // Add same product with quantity 3 -> total qty = 5
        // total = 5 * 100 = 500, discount = 500 * 10% = 50
        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildItemDto(product1, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(500.0))
                .andExpect(jsonPath("$.discount").value(50.0));
    }

    @Test
    void shouldDeleteCartSuccessfully() throws Exception {
        // Given - add an item first
        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildItemDto(product1, 2))))
                .andExpect(status().isOk());

        // When - delete cart
        mockMvc.perform(delete("/api/v1/shopping-cart/customer/{customerId}",
                        shoppingCart.getCustomer().getCustomerId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldGetCartSuccessfully() throws Exception {
        // Given - add an item
        mockMvc.perform(put("/api/v1/shopping-cart/{cartId}", shoppingCart.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildItemDto(product1, 4))))
                .andExpect(status().isOk());

        // When / Then
        mockMvc.perform(get("/api/v1/shopping-cart/customer/{customerId}",
                        shoppingCart.getCustomer().getCustomerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(shoppingCart.getCartId()))
                .andExpect(jsonPath("$.shoppingCartItems", hasSize(1)))
                .andExpect(jsonPath("$.shoppingCartItems[0].quantity").value(4));
    }

    private ShoppingCartItemDto buildItemDto(Product product, long quantity) {
        ProductDto productDto = ProductDto.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .image(product.getImage())
                .desc(product.getDesc())
                .price(product.getPrice())
                .discount(product.getDiscount())
                .qtyInStock(product.getQtyInStock())
                .categoryId(product.getCategory().getCategoryId())
                .build();

        return ShoppingCartItemDto.builder()
                .cartId(shoppingCart.getCartId())
                .product(productDto)
                .quantity(quantity)
                .build();
    }
}
