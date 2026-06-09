package com.vedasole.ekartecommercebackend.service.serviceImpl;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.vedasole.ekartecommercebackend.entity.*;
import com.vedasole.ekartecommercebackend.exception.APIException;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.repository.*;
import com.vedasole.ekartecommercebackend.service.service_impl.FlashSalePurchaseServiceImpl;
import com.vedasole.ekartecommercebackend.service.service_impl.StripeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashSalePurchaseServiceImplTest {

    @Mock private FlashSaleRepo flashSaleRepo;
    @Mock private FlashSaleItemRepo flashSaleItemRepo;
    @Mock private FlashSalePurchaseRepo flashSalePurchaseRepo;
    @Mock private CustomerRepo customerRepo;
    @Mock private ProductRepo productRepo;
    @Mock private OrderRepo orderRepo;
    @Mock private AddressRepo addressRepo;
    @Mock private StripeService stripeService;
    @Mock private EntityManager entityManager;
    @Mock private Query nativeQuery;

    private FlashSalePurchaseServiceImpl purchaseService;

    private FlashSale activeFlashSale;
    private FlashSale futureFlashSale;
    private FlashSale expiredFlashSale;
    private FlashSaleItem flashSaleItem;
    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        purchaseService = new FlashSalePurchaseServiceImpl(
                flashSaleRepo, flashSaleItemRepo, flashSalePurchaseRepo,
                customerRepo, productRepo, orderRepo, addressRepo,
                stripeService, entityManager
        );

        Category category = Category.builder().categoryId(1L).name("Cat").active(true).build();

        customer = Customer.builder().customerId(1L).email("test@test.com").firstName("Test").lastName("User").build();

        product = Product.builder()
                .productId(1L).name("Test Product").image("img.jpg").sku("SKU-001")
                .desc("Desc").price(100.0).discount(0.0).qtyInStock(50).category(category)
                .build();

        activeFlashSale = FlashSale.builder()
                .flashSaleId(1L).name("Active Sale")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        futureFlashSale = FlashSale.builder()
                .flashSaleId(2L).name("Future Sale")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();

        expiredFlashSale = FlashSale.builder()
                .flashSaleId(3L).name("Expired Sale")
                .startTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().minusDays(1))
                .build();

        flashSaleItem = FlashSaleItem.builder()
                .flashSaleItemId(1L).flashSale(activeFlashSale).product(product)
                .flashPrice(50.0).stock(10).limitPerCustomer(3)
                .build();
    }

    @Test
    void purchase_saleNotStarted_throws() {
        // Given
        given(flashSaleRepo.findById(2L)).willReturn(Optional.of(futureFlashSale));

        // When/Then
        assertThatThrownBy(() -> purchaseService.purchaseFlashSaleItem(2L, 1L, 1, 1L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("not started yet");
    }

    @Test
    void purchase_saleExpired_throws() {
        // Given
        given(flashSaleRepo.findById(3L)).willReturn(Optional.of(expiredFlashSale));

        // When/Then
        assertThatThrownBy(() -> purchaseService.purchaseFlashSaleItem(3L, 1L, 1, 1L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("ended");
    }

    @Test
    void purchase_flashSaleNotFound_throws() {
        // Given
        given(flashSaleRepo.findById(999L)).willReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> purchaseService.purchaseFlashSaleItem(999L, 1L, 1, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void purchase_flashSaleItemNotFound_throws() {
        // Given
        given(flashSaleRepo.findById(1L)).willReturn(Optional.of(activeFlashSale));
        given(flashSaleItemRepo.findByFlashSale_FlashSaleIdAndFlashSaleItemId(1L, 999L))
                .willReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> purchaseService.purchaseFlashSaleItem(1L, 999L, 1, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void purchase_overPersonalLimit_throws() {
        // Given
        given(flashSaleRepo.findById(1L)).willReturn(Optional.of(activeFlashSale));
        given(flashSaleItemRepo.findByFlashSale_FlashSaleIdAndFlashSaleItemId(1L, 1L))
                .willReturn(Optional.of(flashSaleItem));
        // Already purchased 2, limit is 3, requesting 2 more (2+2=4 > 3)
        given(flashSalePurchaseRepo.sumQuantityByFlashSaleItemAndCustomer(1L, 1L)).willReturn(2);

        // When/Then
        assertThatThrownBy(() -> purchaseService.purchaseFlashSaleItem(1L, 1L, 2, 1L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Purchase limit exceeded");
    }

    @Test
    void purchase_stockDepleted_throws() {
        // Given
        given(flashSaleRepo.findById(1L)).willReturn(Optional.of(activeFlashSale));
        given(flashSaleItemRepo.findByFlashSale_FlashSaleIdAndFlashSaleItemId(1L, 1L))
                .willReturn(Optional.of(flashSaleItem));
        given(flashSalePurchaseRepo.sumQuantityByFlashSaleItemAndCustomer(1L, 1L)).willReturn(0);
        // Stock deduction returns 0 (no stock)
        given(flashSaleItemRepo.deductStock(1L, 1)).willReturn(0);

        // When/Then
        assertThatThrownBy(() -> purchaseService.purchaseFlashSaleItem(1L, 1L, 1, 1L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Insufficient flash sale stock");
    }

    @Test
    void purchase_customerNotFound_throws() {
        // Given
        given(flashSaleRepo.findById(1L)).willReturn(Optional.of(activeFlashSale));
        given(flashSaleItemRepo.findByFlashSale_FlashSaleIdAndFlashSaleItemId(1L, 1L))
                .willReturn(Optional.of(flashSaleItem));
        given(flashSalePurchaseRepo.sumQuantityByFlashSaleItemAndCustomer(1L, 999L)).willReturn(0);
        given(flashSaleItemRepo.deductStock(1L, 1)).willReturn(1);
        given(customerRepo.findById(999L)).willReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> purchaseService.purchaseFlashSaleItem(1L, 1L, 1, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void purchase_success() throws StripeException {
        // Given
        given(flashSaleRepo.findById(1L)).willReturn(Optional.of(activeFlashSale));
        given(flashSaleItemRepo.findByFlashSale_FlashSaleIdAndFlashSaleItemId(1L, 1L))
                .willReturn(Optional.of(flashSaleItem));
        given(flashSalePurchaseRepo.sumQuantityByFlashSaleItemAndCustomer(1L, 1L)).willReturn(0);
        given(flashSaleItemRepo.deductStock(1L, 1)).willReturn(1);
        given(customerRepo.findById(1L)).willReturn(Optional.of(customer));
        given(productRepo.findById(1L)).willReturn(Optional.of(product));
        given(addressRepo.save(any(Address.class))).willReturn(new Address(1L, "a", "b", "c", "d", "e", 100001));

        Order savedOrder = Order.builder()
                .orderId(1L).customer(customer).orderStatus(com.vedasole.ekartecommercebackend.utility.AppConstant.OrderStatus.ORDER_CREATED)
                .address(new Address(1L, "a", "b", "c", "d", "e", 100001))
                .build();
        given(orderRepo.save(any(Order.class))).willReturn(savedOrder);

        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(anyString(), any())).willReturn(nativeQuery);

        Session mockSession = mock(Session.class);
        given(mockSession.getUrl()).willReturn("https://checkout.stripe.com/test");
        given(stripeService.createFlashSaleCheckoutSession(any(), anyDouble(), anyLong(), any(), any()))
                .willReturn(mockSession);

        // When
        String result = purchaseService.purchaseFlashSaleItem(1L, 1L, 1, 1L);

        // Then
        assertThat(result).isEqualTo("https://checkout.stripe.com/test");

        // Verify stock was deducted
        verify(flashSaleItemRepo).deductStock(1L, 1);

        // Verify order was created
        verify(orderRepo, times(2)).save(any(Order.class));

        // Verify purchase record was created
        verify(flashSalePurchaseRepo).save(any(FlashSalePurchase.class));

        // Verify native SQL was used to override total
        verify(entityManager).flush();
        verify(entityManager).createNativeQuery(contains("UPDATE"));
        verify(nativeQuery).setParameter("flashTotal", 50.0);
        verify(nativeQuery).setParameter("orderId", 1L);
        verify(nativeQuery).executeUpdate();
    }

    @Test
    void reapplyFlashSaleTotal_success() {
        // Given
        FlashSalePurchase purchase = FlashSalePurchase.builder()
                .orderId(1L).flashPriceTotal(50.0).build();
        given(flashSalePurchaseRepo.findByOrderId(1L)).willReturn(Optional.of(purchase));
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(anyString(), any())).willReturn(nativeQuery);

        // When
        purchaseService.reapplyFlashSaleTotal(1L);

        // Then
        verify(entityManager).createNativeQuery(contains("UPDATE"));
        verify(nativeQuery).setParameter("flashTotal", 50.0);
        verify(nativeQuery).executeUpdate();
    }

    @Test
    void reapplyFlashSaleTotal_noFlashPurchase_noop() {
        // Given
        given(flashSalePurchaseRepo.findByOrderId(999L)).willReturn(Optional.empty());

        // When
        purchaseService.reapplyFlashSaleTotal(999L);

        // Then
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void isFlashSaleOrder_true() {
        // Given
        given(flashSalePurchaseRepo.findByOrderId(1L)).willReturn(Optional.of(
                FlashSalePurchase.builder().orderId(1L).build()
        ));

        // When/Then
        assertThat(purchaseService.isFlashSaleOrder(1L)).isTrue();
    }

    @Test
    void isFlashSaleOrder_false() {
        // Given
        given(flashSalePurchaseRepo.findByOrderId(999L)).willReturn(Optional.empty());

        // When/Then
        assertThat(purchaseService.isFlashSaleOrder(999L)).isFalse();
    }
}
