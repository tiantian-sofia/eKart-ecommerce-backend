package com.vedasole.ekartecommercebackend.service.serviceImpl;

import com.vedasole.ekartecommercebackend.entity.Category;
import com.vedasole.ekartecommercebackend.entity.Customer;
import com.vedasole.ekartecommercebackend.entity.Product;
import com.vedasole.ekartecommercebackend.entity.ShoppingCart;
import com.vedasole.ekartecommercebackend.entity.ShoppingCartItem;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.payload.ProductDto;
import com.vedasole.ekartecommercebackend.payload.ShoppingCartDto;
import com.vedasole.ekartecommercebackend.payload.ShoppingCartItemDto;
import com.vedasole.ekartecommercebackend.repository.CustomerRepo;
import com.vedasole.ekartecommercebackend.repository.ShoppingCartItemRepo;
import com.vedasole.ekartecommercebackend.repository.ShoppingCartRepo;
import com.vedasole.ekartecommercebackend.service.service_impl.ShoppingCartServiceImpl;
import com.vedasole.ekartecommercebackend.service.service_interface.ShoppingCartItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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

    @InjectMocks
    private ShoppingCartServiceImpl underTest;

    private Customer customer;
    private ShoppingCart shoppingCart;
    private Product product1;
    private Product product2;
    private ProductDto productDto1;
    private ProductDto productDto2;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId(1L)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .email("john@email.com")
                .build();

        shoppingCart = ShoppingCart.builder()
                .cartId(1L)
                .customer(customer)
                .shoppingCartItems(new ArrayList<>())
                .total(0)
                .discount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customer.setShoppingCart(shoppingCart);

        Category category = Category.builder()
                .categoryId(1L)
                .name("Electronics")
                .build();

        product1 = Product.builder()
                .productId(1L)
                .name("Phone")
                .image("/img/phone.jpg")
                .sku("PHN-001")
                .desc("A smartphone")
                .price(100.0)
                .discount(10.0)
                .qtyInStock(50)
                .category(category)
                .build();

        product2 = Product.builder()
                .productId(2L)
                .name("Laptop")
                .image("/img/laptop.jpg")
                .sku("LPT-001")
                .desc("A laptop")
                .price(500.0)
                .discount(5.0)
                .qtyInStock(30)
                .category(category)
                .build();

        productDto1 = ProductDto.builder()
                .productId(1L)
                .name("Phone")
                .image("/img/phone.jpg")
                .desc("A smartphone")
                .price(100.0)
                .discount(10.0)
                .qtyInStock(50)
                .categoryId(1L)
                .build();

        productDto2 = ProductDto.builder()
                .productId(2L)
                .name("Laptop")
                .image("/img/laptop.jpg")
                .desc("A laptop")
                .price(500.0)
                .discount(5.0)
                .qtyInStock(30)
                .categoryId(1L)
                .build();
    }

    @Test
    void shouldAddNewItemToCart() {
        // Given
        ShoppingCartItemDto itemDto = ShoppingCartItemDto.builder()
                .cartId(1L)
                .product(productDto1)
                .quantity(2)
                .build();

        ShoppingCartItem newItem = ShoppingCartItem.builder()
                .cartItemId(0)
                .product(product1)
                .shoppingCart(shoppingCart)
                .quantity(0)
                .build();

        given(shoppingCartRepo.findById(1L)).willReturn(Optional.of(shoppingCart));
        given(shoppingCartItemService.convertToShoppingCartItem(itemDto)).willReturn(newItem);
        given(shoppingCartItemRepo.save(any(ShoppingCartItem.class))).willAnswer(inv -> inv.getArgument(0));
        given(shoppingCartRepo.save(any(ShoppingCart.class))).willReturn(shoppingCart);
        given(shoppingCartItemService.convertToShoppingCartItemDto(any(ShoppingCartItem.class)))
                .willAnswer(inv -> {
                    ShoppingCartItem item = inv.getArgument(0);
                    return ShoppingCartItemDto.builder()
                            .cartItemId(item.getCartItemId())
                            .product(productDto1)
                            .cartId(1L)
                            .quantity(item.getQuantity())
                            .build();
                });

        // When
        ShoppingCartDto result = underTest.addOrUpdateItemInCart(itemDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getShoppingCartItems()).hasSize(1);
        assertThat(result.getShoppingCartItems().get(0).getQuantity()).isEqualTo(2);
        verify(shoppingCartItemRepo).save(any(ShoppingCartItem.class));
        verify(shoppingCartRepo).save(shoppingCart);
    }

    @Test
    void shouldAccumulateQuantityWhenAddingExistingItem() {
        // Given - cart already has product1 with quantity 3
        ShoppingCartItem existingItem = ShoppingCartItem.builder()
                .cartItemId(10L)
                .product(product1)
                .shoppingCart(shoppingCart)
                .quantity(3)
                .build();
        shoppingCart.getShoppingCartItems().add(existingItem);

        ShoppingCartItemDto itemDto = ShoppingCartItemDto.builder()
                .cartId(1L)
                .product(productDto1)
                .quantity(2)
                .build();

        given(shoppingCartRepo.findById(1L)).willReturn(Optional.of(shoppingCart));
        given(shoppingCartItemRepo.save(any(ShoppingCartItem.class))).willAnswer(inv -> inv.getArgument(0));
        given(shoppingCartRepo.save(any(ShoppingCart.class))).willReturn(shoppingCart);
        given(shoppingCartItemService.convertToShoppingCartItemDto(any(ShoppingCartItem.class)))
                .willAnswer(inv -> {
                    ShoppingCartItem item = inv.getArgument(0);
                    return ShoppingCartItemDto.builder()
                            .cartItemId(item.getCartItemId())
                            .product(productDto1)
                            .cartId(1L)
                            .quantity(item.getQuantity())
                            .build();
                });

        // When
        ShoppingCartDto result = underTest.addOrUpdateItemInCart(itemDto);

        // Then - quantity should be 3 + 2 = 5, NOT 2
        assertThat(result).isNotNull();
        assertThat(result.getShoppingCartItems()).hasSize(1);
        assertThat(result.getShoppingCartItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(existingItem.getQuantity()).isEqualTo(5);
    }

    @Test
    void shouldAccumulateQuantityMultipleTimes() {
        // Given - cart already has product1 with quantity 1
        ShoppingCartItem existingItem = ShoppingCartItem.builder()
                .cartItemId(10L)
                .product(product1)
                .shoppingCart(shoppingCart)
                .quantity(1)
                .build();
        shoppingCart.getShoppingCartItems().add(existingItem);

        given(shoppingCartRepo.findById(1L)).willReturn(Optional.of(shoppingCart));
        given(shoppingCartItemRepo.save(any(ShoppingCartItem.class))).willAnswer(inv -> inv.getArgument(0));
        given(shoppingCartRepo.save(any(ShoppingCart.class))).willReturn(shoppingCart);
        given(shoppingCartItemService.convertToShoppingCartItemDto(any(ShoppingCartItem.class)))
                .willAnswer(inv -> {
                    ShoppingCartItem item = inv.getArgument(0);
                    return ShoppingCartItemDto.builder()
                            .cartItemId(item.getCartItemId())
                            .product(productDto1)
                            .cartId(1L)
                            .quantity(item.getQuantity())
                            .build();
                });

        // When - add 2 more
        ShoppingCartItemDto itemDto1 = ShoppingCartItemDto.builder()
                .cartId(1L).product(productDto1).quantity(2).build();
        underTest.addOrUpdateItemInCart(itemDto1);

        // Then add 3 more
        ShoppingCartItemDto itemDto2 = ShoppingCartItemDto.builder()
                .cartId(1L).product(productDto1).quantity(3).build();
        ShoppingCartDto result = underTest.addOrUpdateItemInCart(itemDto2);

        // Then - quantity should be 1 + 2 + 3 = 6
        assertThat(result.getShoppingCartItems().get(0).getQuantity()).isEqualTo(6);
        assertThat(existingItem.getQuantity()).isEqualTo(6);
    }

    @Test
    void shouldAddDifferentProductsIndependently() {
        // Given - cart already has product1 with quantity 3
        ShoppingCartItem existingItem1 = ShoppingCartItem.builder()
                .cartItemId(10L)
                .product(product1)
                .shoppingCart(shoppingCart)
                .quantity(3)
                .build();
        shoppingCart.getShoppingCartItems().add(existingItem1);

        // Add product2 with quantity 5
        ShoppingCartItemDto itemDto2 = ShoppingCartItemDto.builder()
                .cartId(1L)
                .product(productDto2)
                .quantity(5)
                .build();

        ShoppingCartItem newItem2 = ShoppingCartItem.builder()
                .cartItemId(0)
                .product(product2)
                .shoppingCart(shoppingCart)
                .quantity(0)
                .build();

        given(shoppingCartRepo.findById(1L)).willReturn(Optional.of(shoppingCart));
        given(shoppingCartItemService.convertToShoppingCartItem(itemDto2)).willReturn(newItem2);
        given(shoppingCartItemRepo.save(any(ShoppingCartItem.class))).willAnswer(inv -> inv.getArgument(0));
        given(shoppingCartRepo.save(any(ShoppingCart.class))).willReturn(shoppingCart);
        given(shoppingCartItemService.convertToShoppingCartItemDto(any(ShoppingCartItem.class)))
                .willAnswer(inv -> {
                    ShoppingCartItem item = inv.getArgument(0);
                    ProductDto pDto = item.getProduct().getProductId() == 1L ? productDto1 : productDto2;
                    return ShoppingCartItemDto.builder()
                            .cartItemId(item.getCartItemId())
                            .product(pDto)
                            .cartId(1L)
                            .quantity(item.getQuantity())
                            .build();
                });

        // When
        ShoppingCartDto result = underTest.addOrUpdateItemInCart(itemDto2);

        // Then - product1 quantity untouched (3), product2 quantity = 5
        assertThat(result.getShoppingCartItems()).hasSize(2);
        assertThat(existingItem1.getQuantity()).isEqualTo(3);
        assertThat(newItem2.getQuantity()).isEqualTo(5);
    }

    @Test
    void shouldThrowExceptionWhenCartNotFound() {
        // Given
        ShoppingCartItemDto itemDto = ShoppingCartItemDto.builder()
                .cartId(999L)
                .product(productDto1)
                .quantity(1)
                .build();

        given(shoppingCartRepo.findById(999L)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> underTest.addOrUpdateItemInCart(itemDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ShoppingCart")
                .hasMessageContaining("999");
    }

    @Test
    void shouldCalculateTotalCorrectlyAfterAccumulation() {
        // Given - cart has product1 (price=100, discount=10%) with quantity 2
        ShoppingCartItem existingItem = ShoppingCartItem.builder()
                .cartItemId(10L)
                .product(product1)
                .shoppingCart(shoppingCart)
                .quantity(2)
                .build();
        shoppingCart.getShoppingCartItems().add(existingItem);

        ShoppingCartItemDto itemDto = ShoppingCartItemDto.builder()
                .cartId(1L)
                .product(productDto1)
                .quantity(3)
                .build();

        given(shoppingCartRepo.findById(1L)).willReturn(Optional.of(shoppingCart));
        given(shoppingCartItemRepo.save(any(ShoppingCartItem.class))).willAnswer(inv -> inv.getArgument(0));
        given(shoppingCartRepo.save(any(ShoppingCart.class))).willAnswer(inv -> inv.getArgument(0));
        given(shoppingCartItemService.convertToShoppingCartItemDto(any(ShoppingCartItem.class)))
                .willAnswer(inv -> {
                    ShoppingCartItem item = inv.getArgument(0);
                    return ShoppingCartItemDto.builder()
                            .cartItemId(item.getCartItemId())
                            .product(productDto1)
                            .cartId(1L)
                            .quantity(item.getQuantity())
                            .build();
                });

        // When
        ShoppingCartDto result = underTest.addOrUpdateItemInCart(itemDto);

        // Then - quantity = 2+3=5, total = 5*100=500, discount = 500*10/100=50
        assertThat(result.getTotal()).isEqualTo(500.0);
        assertThat(result.getDiscount()).isEqualTo(50.0);
    }

    @Test
    void shouldDeleteCartSuccessfully() {
        // Given
        long customerId = 1L;

        // When
        underTest.deleteCart(customerId);

        // Then
        verify(shoppingCartRepo).deleteByCustomer_CustomerId(customerId);
    }

    @Test
    void shouldPropagateExceptionWhenDeleteCartFails() {
        // Given
        long customerId = 999L;
        doThrow(new RuntimeException("DB error"))
                .when(shoppingCartRepo).deleteByCustomer_CustomerId(customerId);

        // When / Then
        assertThatThrownBy(() -> underTest.deleteCart(customerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        verify(shoppingCartRepo).deleteByCustomer_CustomerId(customerId);
    }

    @Test
    void shouldGetCartSuccessfully() {
        // Given
        long customerId = 1L;
        ShoppingCartItem item = ShoppingCartItem.builder()
                .cartItemId(10L)
                .product(product1)
                .shoppingCart(shoppingCart)
                .quantity(2)
                .build();
        shoppingCart.setShoppingCartItems(new ArrayList<>(java.util.List.of(item)));

        given(customerRepo.findById(customerId)).willReturn(Optional.of(customer));
        given(shoppingCartItemService.convertToShoppingCartItemDto(any(ShoppingCartItem.class)))
                .willAnswer(inv -> {
                    ShoppingCartItem i = inv.getArgument(0);
                    return ShoppingCartItemDto.builder()
                            .cartItemId(i.getCartItemId())
                            .product(productDto1)
                            .cartId(1L)
                            .quantity(i.getQuantity())
                            .build();
                });

        // When
        ShoppingCartDto result = underTest.getCart(customerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCartId()).isEqualTo(1L);
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getShoppingCartItems()).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenGetCartCustomerNotFound() {
        // Given
        long customerId = 999L;
        given(customerRepo.findById(customerId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> underTest.getCart(customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("999");
    }

    @Test
    void shouldThrowExceptionWhenCustomerHasNoCart() {
        // Given
        long customerId = 1L;
        customer.setShoppingCart(null);
        given(customerRepo.findById(customerId)).willReturn(Optional.of(customer));

        // When / Then
        assertThatThrownBy(() -> underTest.getCart(customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ShoppingCart");
    }
}
