package com.vedasole.ekartecommercebackend.service.serviceImpl;

import com.vedasole.ekartecommercebackend.entity.*;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.payload.ProductDto;
import com.vedasole.ekartecommercebackend.payload.ShoppingCartDto;
import com.vedasole.ekartecommercebackend.payload.ShoppingCartItemDto;
import com.vedasole.ekartecommercebackend.repository.CustomerRepo;
import com.vedasole.ekartecommercebackend.repository.ShoppingCartItemRepo;
import com.vedasole.ekartecommercebackend.repository.ShoppingCartRepo;
import com.vedasole.ekartecommercebackend.service.service_impl.ShoppingCartServiceImpl;
import com.vedasole.ekartecommercebackend.service.service_interface.ShoppingCartItemService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceImplTest {

        @Mock
        private ShoppingCartRepo shoppingCartRepo;
        @Mock
        private CustomerRepo customerRepo;
        @Mock
        private ShoppingCartItemRepo shoppingCartItemRepo;
        @Mock
        private ShoppingCartItemService shoppingCartItemService;
        @Mock
        private EntityManager entityManager;

        @InjectMocks
        private ShoppingCartServiceImpl shoppingCartService;

        private Customer customer;
        private ShoppingCart shoppingCart;
        private Product product;
        private Category category;

        @BeforeEach
        void setUp() {
                category = Category.builder()
                                .categoryId(1L)
                                .name("Electronics")
                                .image("/images/cat.jpg")
                                .desc("Electronics category")
                                .active(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                product = Product.builder()
                                .productId(1L)
                                .name("Laptop")
                                .image("/images/laptop.jpg")
                                .sku("LAP-001")
                                .desc("A powerful laptop")
                                .price(1000.0)
                                .discount(10.0)
                                .qtyInStock(50)
                                .category(category)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                User user = new User("john@email.com", "password",
                                com.vedasole.ekartecommercebackend.utility.AppConstant.Role.USER);
                customer = Customer.builder()
                                .customerId(1L)
                                .firstName("John")
                                .lastName("Doe")
                                .phoneNumber("1234567890")
                                .email("john@email.com")
                                .user(user)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                shoppingCart = ShoppingCart.builder()
                                .cartId(1L)
                                .customer(customer)
                                .shoppingCartItems(new ArrayList<>())
                                .total(0.0)
                                .discount(0.0)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                customer.setShoppingCart(shoppingCart);
        }

        // ==================== addOrUpdateItemInCart ====================

        @Nested
        @DisplayName("addOrUpdateItemInCart")
        class AddOrUpdateItemInCart {

                private ShoppingCartItemDto buildCartItemDto(long quantity) {
                        ProductDto productDto = ProductDto.builder()
                                        .productId(product.getProductId())
                                        .name(product.getName())
                                        .image(product.getImage())
                                        .desc(product.getDesc())
                                        .price(product.getPrice())
                                        .discount(product.getDiscount())
                                        .qtyInStock(product.getQtyInStock())
                                        .categoryId(category.getCategoryId())
                                        .build();
                        return ShoppingCartItemDto.builder()
                                        .cartId(shoppingCart.getCartId())
                                        .product(productDto)
                                        .quantity(quantity)
                                        .build();
                }

                private ShoppingCartItem buildCartItemEntity(long quantity) {
                        return ShoppingCartItem.builder()
                                        .cartItemId(1L)
                                        .product(product)
                                        .shoppingCart(shoppingCart)
                                        .quantity(quantity)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();
                }

                @Test
                @DisplayName("should throw ResourceNotFoundException when cart does not exist")
                void shouldThrowWhenCartNotFound() {
                        ShoppingCartItemDto dto = buildCartItemDto(2);
                        when(shoppingCartRepo.findById(anyLong())).thenReturn(Optional.empty());

                        assertThrows(ResourceNotFoundException.class,
                                        () -> shoppingCartService.addOrUpdateItemInCart(dto));

                        verify(shoppingCartRepo).findById(dto.getCartId());
                        verifyNoMoreInteractions(shoppingCartItemRepo);
                }

                @Test
                @DisplayName("should add new item when product not yet in cart")
                void shouldAddNewItemWhenProductNotInCart() {
                        ShoppingCartItemDto dto = buildCartItemDto(3);
                        ShoppingCartItem newItem = buildCartItemEntity(3);

                        when(shoppingCartRepo.findById(shoppingCart.getCartId())).thenReturn(Optional.of(shoppingCart));
                        when(shoppingCartItemService.convertToShoppingCartItem(dto)).thenReturn(newItem);
                        when(shoppingCartItemRepo.save(any(ShoppingCartItem.class))).thenReturn(newItem);
                        when(shoppingCartRepo.save(any(ShoppingCart.class))).thenReturn(shoppingCart);

                        ShoppingCartDto result = shoppingCartService.addOrUpdateItemInCart(dto);

                        // Verify item was added to cart
                        assertThat(shoppingCart.getShoppingCartItems()).contains(newItem);
                        assertThat(newItem.getQuantity()).isEqualTo(3);

                        // Verify total: 1000 * 3 = 3000
                        assertThat(result.getTotal()).isEqualTo(3000.0);
                        // Verify discount: 10 * 1000 / 100 * 3 = 300
                        assertThat(result.getDiscount()).isEqualTo(300.0);

                        verify(shoppingCartItemService).convertToShoppingCartItem(dto);
                        verify(shoppingCartItemRepo).save(newItem);
                        verify(shoppingCartRepo).save(shoppingCart);
                }

                @Test
                @DisplayName("should INCREMENT quantity when product already in cart (bug fix)")
                void shouldIncrementQuantityWhenProductAlreadyInCart() {
                        // Cart already has 3 units of this product
                        ShoppingCartItem existingItem = buildCartItemEntity(3);
                        shoppingCart.getShoppingCartItems().add(existingItem);

                        // User adds 2 more
                        ShoppingCartItemDto dto = buildCartItemDto(2);

                        when(shoppingCartRepo.findById(shoppingCart.getCartId())).thenReturn(Optional.of(shoppingCart));
                        when(shoppingCartItemRepo.save(any(ShoppingCartItem.class))).thenReturn(existingItem);
                        when(shoppingCartRepo.save(any(ShoppingCart.class))).thenReturn(shoppingCart);

                        ShoppingCartDto result = shoppingCartService.addOrUpdateItemInCart(dto);

                        // The key assertion: quantity should be 3 + 2 = 5, NOT overwritten to 2
                        assertThat(existingItem.getQuantity()).isEqualTo(5);

                        // Total: 1000 * 5 = 5000
                        assertThat(result.getTotal()).isEqualTo(5000.0);
                        // Discount: 10 * 1000 / 100 * 5 = 500
                        assertThat(result.getDiscount()).isEqualTo(500.0);

                        // Should NOT create a new item (convertToShoppingCartItem not called)
                        verify(shoppingCartItemService, never()).convertToShoppingCartItem(any());
                        verify(shoppingCartItemRepo).save(existingItem);
                        verify(shoppingCartRepo).save(shoppingCart);
                }

                @Test
                @DisplayName("should correctly increment quantity when adding same product multiple times")
                void shouldAccumulateQuantityAcrossMultipleAdds() {
                        // Cart already has 5 units
                        ShoppingCartItem existingItem = buildCartItemEntity(5);
                        shoppingCart.getShoppingCartItems().add(existingItem);

                        // User adds 1 more
                        ShoppingCartItemDto dto = buildCartItemDto(1);

                        when(shoppingCartRepo.findById(shoppingCart.getCartId())).thenReturn(Optional.of(shoppingCart));
                        when(shoppingCartItemRepo.save(any(ShoppingCartItem.class))).thenReturn(existingItem);
                        when(shoppingCartRepo.save(any(ShoppingCart.class))).thenReturn(shoppingCart);

                        shoppingCartService.addOrUpdateItemInCart(dto);

                        // 5 + 1 = 6
                        assertThat(existingItem.getQuantity()).isEqualTo(6);
                }

                @Test
                @DisplayName("should handle adding a different product as a new item while existing items remain")
                void shouldAddDifferentProductAsNewItem() {
                        // Cart has product A (productId=1)
                        ShoppingCartItem existingItem = buildCartItemEntity(3);
                        shoppingCart.getShoppingCartItems().add(existingItem);

                        // User adds product B (productId=2)
                        Product productB = Product.builder()
                                        .productId(2L)
                                        .name("Phone")
                                        .image("/images/phone.jpg")
                                        .sku("PHN-001")
                                        .desc("A smartphone")
                                        .price(500.0)
                                        .discount(5.0)
                                        .qtyInStock(100)
                                        .category(category)
                                        .build();

                        ProductDto productBDto = ProductDto.builder()
                                        .productId(2L)
                                        .name("Phone")
                                        .image("/images/phone.jpg")
                                        .desc("A smartphone")
                                        .price(500.0)
                                        .discount(5.0)
                                        .qtyInStock(100)
                                        .categoryId(category.getCategoryId())
                                        .build();

                        ShoppingCartItemDto dto = ShoppingCartItemDto.builder()
                                        .cartId(shoppingCart.getCartId())
                                        .product(productBDto)
                                        .quantity(2)
                                        .build();

                        ShoppingCartItem newCartItem = ShoppingCartItem.builder()
                                        .cartItemId(2L)
                                        .product(productB)
                                        .shoppingCart(shoppingCart)
                                        .quantity(2)
                                        .build();

                        when(shoppingCartRepo.findById(shoppingCart.getCartId())).thenReturn(Optional.of(shoppingCart));
                        when(shoppingCartItemService.convertToShoppingCartItem(dto)).thenReturn(newCartItem);
                        when(shoppingCartItemRepo.save(any(ShoppingCartItem.class))).thenReturn(newCartItem);
                        when(shoppingCartRepo.save(any(ShoppingCart.class))).thenReturn(shoppingCart);

                        ShoppingCartDto result = shoppingCartService.addOrUpdateItemInCart(dto);

                        // existing item quantity should be unchanged
                        assertThat(existingItem.getQuantity()).isEqualTo(3);
                        // new item should be added with quantity 2
                        assertThat(shoppingCart.getShoppingCartItems()).hasSize(2);

                        // Total: (1000 * 3) + (500 * 2) = 4000
                        assertThat(result.getTotal()).isEqualTo(4000.0);
                }
        }

        // ==================== createCartWithItems ====================

        @Nested
        @DisplayName("createCartWithItems")
        class CreateCartWithItems {

                @Test
                @DisplayName("should create a new cart with items when no cart exists for customer")
                void shouldCreateNewCartWithItems() {
                        ProductDto productDto = ProductDto.builder()
                                        .productId(product.getProductId())
                                        .name(product.getName())
                                        .image(product.getImage())
                                        .desc(product.getDesc())
                                        .price(product.getPrice())
                                        .discount(product.getDiscount())
                                        .qtyInStock(product.getQtyInStock())
                                        .categoryId(category.getCategoryId())
                                        .build();

                        ShoppingCartItemDto itemDto = ShoppingCartItemDto.builder()
                                        .product(productDto)
                                        .quantity(2)
                                        .build();

                        ShoppingCartDto cartDto = ShoppingCartDto.builder()
                                        .customerId(customer.getCustomerId())
                                        .shoppingCartItems(List.of(itemDto))
                                        .build();

                        ShoppingCartItem cartItem = ShoppingCartItem.builder()
                                        .cartItemId(1L)
                                        .product(product)
                                        .shoppingCart(shoppingCart)
                                        .quantity(2)
                                        .build();

                        // No existing cart for this customer
                        when(shoppingCartRepo.findByCustomer_CustomerId(customer.getCustomerId()))
                                        .thenReturn(Optional.empty());
                        when(customerRepo.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
                        when(shoppingCartRepo.save(any(ShoppingCart.class))).thenReturn(shoppingCart);
                        when(shoppingCartItemService.convertToShoppingCartItem(any(ShoppingCartItemDto.class)))
                                        .thenReturn(cartItem);
                        when(shoppingCartItemRepo.saveAll(any(List.class))).thenReturn(List.of(cartItem));

                        ShoppingCartDto result = shoppingCartService.createCartWithItems(cartDto);

                        assertThat(result).isNotNull();
                        assertThat(result.getCustomerId()).isEqualTo(customer.getCustomerId());

                        verify(shoppingCartRepo).findByCustomer_CustomerId(customer.getCustomerId());
                        verify(customerRepo).findById(customer.getCustomerId());
                }

                @Test
                @DisplayName("should use existing cart when cart already exists for customer")
                void shouldUseExistingCartWhenCartExists() {
                        ProductDto productDto = ProductDto.builder()
                                        .productId(product.getProductId())
                                        .build();

                        ShoppingCartItemDto itemDto = ShoppingCartItemDto.builder()
                                        .product(productDto)
                                        .quantity(1)
                                        .build();

                        ShoppingCartDto cartDto = ShoppingCartDto.builder()
                                        .customerId(customer.getCustomerId())
                                        .shoppingCartItems(List.of(itemDto))
                                        .build();

                        ShoppingCartItem cartItem = ShoppingCartItem.builder()
                                        .cartItemId(1L)
                                        .product(product)
                                        .shoppingCart(shoppingCart)
                                        .quantity(1)
                                        .build();

                        // Cart already exists
                        when(shoppingCartRepo.findByCustomer_CustomerId(customer.getCustomerId()))
                                        .thenReturn(Optional.of(shoppingCart));
                        when(shoppingCartItemService.convertToShoppingCartItem(any(ShoppingCartItemDto.class)))
                                        .thenReturn(cartItem);
                        when(shoppingCartItemRepo.saveAll(any(List.class))).thenReturn(List.of(cartItem));
                        when(shoppingCartRepo.save(any(ShoppingCart.class))).thenReturn(shoppingCart);

                        ShoppingCartDto result = shoppingCartService.createCartWithItems(cartDto);

                        assertThat(result).isNotNull();
                        // Should NOT create a new cart
                        verify(customerRepo, never()).findById(anyLong());
                }
        }

        // ==================== getCart ====================

        @Nested
        @DisplayName("getCart")
        class GetCart {

                @Test
                @DisplayName("should return cart DTO when customer and cart exist")
                void shouldReturnCartWhenExists() {
                        ShoppingCartItem cartItem = ShoppingCartItem.builder()
                                        .cartItemId(1L)
                                        .product(product)
                                        .shoppingCart(shoppingCart)
                                        .quantity(2)
                                        .build();
                        shoppingCart.getShoppingCartItems().add(cartItem);

                        ProductDto productDto = ProductDto.builder()
                                        .productId(product.getProductId())
                                        .name(product.getName())
                                        .image(product.getImage())
                                        .desc(product.getDesc())
                                        .price(product.getPrice())
                                        .discount(product.getDiscount())
                                        .qtyInStock(product.getQtyInStock())
                                        .categoryId(category.getCategoryId())
                                        .build();

                        ShoppingCartItemDto itemDto = ShoppingCartItemDto.builder()
                                        .cartItemId(1L)
                                        .product(productDto)
                                        .cartId(shoppingCart.getCartId())
                                        .quantity(2)
                                        .build();

                        when(customerRepo.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
                        when(shoppingCartItemService.convertToShoppingCartItemDto(cartItem)).thenReturn(itemDto);

                        ShoppingCartDto result = shoppingCartService.getCart(customer.getCustomerId());

                        assertThat(result).isNotNull();
                        assertThat(result.getCustomerId()).isEqualTo(customer.getCustomerId());
                        assertThat(result.getShoppingCartItems()).hasSize(1);
                }

                @Test
                @DisplayName("should throw ResourceNotFoundException when customer does not exist")
                void shouldThrowWhenCustomerNotFound() {
                        when(customerRepo.findById(99L)).thenReturn(Optional.empty());

                        assertThrows(ResourceNotFoundException.class,
                                        () -> shoppingCartService.getCart(99L));
                }

                @Test
                @DisplayName("should throw ResourceNotFoundException when customer has no cart")
                void shouldThrowWhenCartNotFound() {
                        customer.setShoppingCart(null);
                        when(customerRepo.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));

                        assertThrows(ResourceNotFoundException.class,
                                        () -> shoppingCartService.getCart(customer.getCustomerId()));
                }
        }

        // ==================== deleteCart ====================

        @Nested
        @DisplayName("deleteCart")
        class DeleteCart {

                @Test
                @DisplayName("should load items, clear collection, then delete cart (orphanRemoval cascades)")
                void shouldDeleteCartSuccessfully() {
                        ShoppingCartItem cartItem = ShoppingCartItem.builder()
                                        .cartItemId(1L)
                                        .product(product)
                                        .shoppingCart(shoppingCart)
                                        .quantity(2)
                                        .build();
                        shoppingCart.getShoppingCartItems().add(cartItem);

                        when(shoppingCartRepo.findByCustomer_CustomerId(customer.getCustomerId()))
                                        .thenReturn(Optional.of(shoppingCart));
                        when(shoppingCartItemRepo.findAllByShoppingCartCartId(shoppingCart.getCartId()))
                                        .thenReturn(List.of(cartItem));

                        shoppingCartService.deleteCart(customer.getCustomerId());

                        // Verify items loaded into persistence context
                        verify(shoppingCartItemRepo).findAllByShoppingCartCartId(shoppingCart.getCartId());
                        // Verify collection was cleared directly (bypassing setter)
                        assertThat(shoppingCart.getShoppingCartItems()).isEmpty();
                        // Verify cart deleted (orphanRemoval cascades delete to removed items)
                        verify(shoppingCartRepo).delete(shoppingCart);
                }

                @Test
                @DisplayName("should throw ResourceNotFoundException when cart does not exist")
                void shouldThrowWhenCartNotFound() {
                        when(shoppingCartRepo.findByCustomer_CustomerId(99L)).thenReturn(Optional.empty());

                        assertThrows(ResourceNotFoundException.class,
                                        () -> shoppingCartService.deleteCart(99L));

                        verify(shoppingCartItemRepo, never()).deleteAllByShoppingCartCartId(anyLong());
                        verify(entityManager, never()).flush();
                        verify(shoppingCartRepo, never()).delete(any());
                }

                @Test
                @DisplayName("should propagate exception when delete fails")
                void shouldPropagateExceptionWhenDeleteFails() {
                        when(shoppingCartRepo.findByCustomer_CustomerId(customer.getCustomerId()))
                                        .thenReturn(Optional.of(shoppingCart));
                        when(shoppingCartItemRepo.findAllByShoppingCartCartId(shoppingCart.getCartId()))
                                        .thenReturn(List.of());
                        doThrow(new RuntimeException("DB error")).when(entityManager).flush();

                        assertThrows(RuntimeException.class,
                                        () -> shoppingCartService.deleteCart(customer.getCustomerId()));
                }
        }

        // ==================== convertToShoppingCart ====================

        @Nested
        @DisplayName("convertToShoppingCart")
        class ConvertToShoppingCart {

                @Test
                @DisplayName("should convert DTO to entity with items")
                void shouldConvertDtoToEntityWithItems() {
                        ProductDto productDto = ProductDto.builder()
                                        .productId(product.getProductId())
                                        .build();

                        ShoppingCartItemDto itemDto = ShoppingCartItemDto.builder()
                                        .product(productDto)
                                        .quantity(2)
                                        .build();

                        ShoppingCartDto cartDto = ShoppingCartDto.builder()
                                        .customerId(customer.getCustomerId())
                                        .shoppingCartItems(List.of(itemDto))
                                        .build();

                        ShoppingCartItem cartItem = ShoppingCartItem.builder()
                                        .product(product)
                                        .quantity(2)
                                        .build();

                        when(customerRepo.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
                        when(shoppingCartItemService.convertToShoppingCartItem(itemDto)).thenReturn(cartItem);

                        ShoppingCart result = shoppingCartService.convertToShoppingCart(cartDto);

                        assertThat(result).isNotNull();
                        assertThat(result.getCustomer()).isEqualTo(customer);
                        assertThat(result.getShoppingCartItems()).hasSize(1);
                }
        }
}
