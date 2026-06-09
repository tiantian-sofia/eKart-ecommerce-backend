package com.vedasole.ekartecommercebackend.service.serviceImpl;

import com.vedasole.ekartecommercebackend.entity.*;
import com.vedasole.ekartecommercebackend.exception.APIException;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.payload.FlashSaleCampaignDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleItemDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleOrderRequestDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleOrderResponseDto;
import com.vedasole.ekartecommercebackend.repository.*;
import com.vedasole.ekartecommercebackend.service.service_impl.FlashSaleServiceImpl;
import com.vedasole.ekartecommercebackend.utility.AppConstant.FlashSaleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlashSaleServiceImplTest {

    @Mock private FlashSaleCampaignRepo campaignRepo;
    @Mock private FlashSaleItemRepo flashSaleItemRepo;
    @Mock private FlashSalePurchaseRecordRepo purchaseRecordRepo;
    @Mock private ProductRepo productRepo;
    @Mock private CustomerRepo customerRepo;
    @Mock private AddressRepo addressRepo;
    @Mock private OrderRepo orderRepo;

    private final ModelMapper modelMapper = new ModelMapper();
    private FlashSaleServiceImpl flashSaleService;

    private FlashSaleCampaign activeCampaign;
    private FlashSaleItem flashSaleItem;
    private Product product;
    private Customer customer;
    private Address address;

    @BeforeEach
    void setUp() {
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        flashSaleService = new FlashSaleServiceImpl(
                campaignRepo, flashSaleItemRepo, purchaseRecordRepo,
                productRepo, customerRepo, addressRepo, orderRepo, modelMapper
        );

        Category category = Category.builder()
                .categoryId(1L).name("Electronics").active(true).build();
        product = Product.builder()
                .productId(1L).name("Phone").image("img.png").sku("ELE-Phone-des")
                .price(1000.0).discount(0).qtyInStock(100).category(category).build();

        // Campaign window: far past to far future so now() is always within range
        activeCampaign = FlashSaleCampaign.builder()
                .campaignId(1L).name("Summer Sale")
                .startTime(LocalDateTime.now().minusDays(100))
                .endTime(LocalDateTime.now().plusDays(100))
                .status(FlashSaleStatus.ACTIVE)
                .build();

        flashSaleItem = FlashSaleItem.builder()
                .flashSaleItemId(1L).campaign(activeCampaign).product(product)
                .flashPrice(500.0).totalStock(10).availableStock(10).perUserLimit(2)
                .build();

        User user = User.builder().userId(1L).email("test@test.com")
                .password("pass").role(com.vedasole.ekartecommercebackend.utility.AppConstant.Role.USER).build();
        customer = Customer.builder().customerId(1L).firstName("John").lastName("Doe")
                .phoneNumber("1234567890").email("test@test.com").user(user).build();

        address = Address.builder().addressId(1L).addLine1("123 St").city("City")
                .state("State").country("Country").postalCode(100001).build();
    }

    // ==================== Campaign CRUD ====================

    @Test
    void createCampaign_shouldSaveAndReturnDto() {
        FlashSaleCampaignDto dto = FlashSaleCampaignDto.builder()
                .name("Summer Sale")
                .startTime(activeCampaign.getStartTime())
                .endTime(activeCampaign.getEndTime())
                .build();
        given(campaignRepo.save(any(FlashSaleCampaign.class))).willReturn(activeCampaign);

        FlashSaleCampaignDto result = flashSaleService.createCampaign(dto);

        assertThat(result.getName()).isEqualTo("Summer Sale");
        assertThat(result.getStatus()).isEqualTo(FlashSaleStatus.ACTIVE);
        verify(campaignRepo).save(any(FlashSaleCampaign.class));
    }

    @Test
    void getCampaign_shouldThrowWhenNotFound() {
        given(campaignRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flashSaleService.getCampaign(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== Order Placement - Happy Path ====================

    @Test
    void placeOrder_shouldSucceedWhenAllConditionsMet() {
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(1).addressId(1L).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));
        given(customerRepo.findById(1L)).willReturn(Optional.of(customer));
        given(purchaseRecordRepo.findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(1L, 1L))
                .willReturn(Optional.empty());
        given(addressRepo.findById(1L)).willReturn(Optional.of(address));
        given(orderRepo.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(100L);
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        FlashSaleOrderResponseDto result = flashSaleService.placeFlashSaleOrder(request);

        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.getFlashPrice()).isEqualTo(500.0);
        assertThat(result.getQuantity()).isEqualTo(1);
        assertThat(result.getTotalAmount()).isEqualTo(500.0);
        assertThat(result.getProductName()).isEqualTo("Phone");
    }

    // ==================== Time Window Checks ====================

    @Test
    void placeOrder_shouldThrowWhenCampaignNotStarted() {
        // Campaign starts in the future
        activeCampaign.setStartTime(LocalDateTime.now().plusDays(1));
        activeCampaign.setEndTime(LocalDateTime.now().plusDays(2));

        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(1).addressId(1L).build();
        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));

        assertThatThrownBy(() -> flashSaleService.placeFlashSaleOrder(request))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Flash sale is not active");

        verify(orderRepo, never()).save(any());
    }

    @Test
    void placeOrder_shouldThrowWhenCampaignEnded() {
        // Campaign ended in the past
        activeCampaign.setStartTime(LocalDateTime.now().minusDays(3));
        activeCampaign.setEndTime(LocalDateTime.now().minusDays(1));

        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(1).addressId(1L).build();
        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));

        assertThatThrownBy(() -> flashSaleService.placeFlashSaleOrder(request))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Flash sale is not active");

        verify(orderRepo, never()).save(any());
    }

    // ==================== Stock Checks ====================

    @Test
    void placeOrder_shouldThrowWhenInsufficientStock() {
        flashSaleItem.setAvailableStock(2);
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(3).addressId(1L).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));

        assertThatThrownBy(() -> flashSaleService.placeFlashSaleOrder(request))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Insufficient flash sale stock");

        verify(orderRepo, never()).save(any());
    }

    @Test
    void placeOrder_shouldThrowWhenStockIsZero() {
        flashSaleItem.setAvailableStock(0);
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(1).addressId(1L).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));

        assertThatThrownBy(() -> flashSaleService.placeFlashSaleOrder(request))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Insufficient flash sale stock");
    }

    @Test
    void placeOrder_shouldDecrementStockCorrectly() {
        flashSaleItem.setAvailableStock(5);
        flashSaleItem.setPerUserLimit(5);
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(3).addressId(1L).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));
        given(customerRepo.findById(1L)).willReturn(Optional.of(customer));
        given(purchaseRecordRepo.findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(1L, 1L))
                .willReturn(Optional.empty());
        given(addressRepo.findById(1L)).willReturn(Optional.of(address));
        given(orderRepo.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(100L);
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        flashSaleService.placeFlashSaleOrder(request);

        ArgumentCaptor<FlashSaleItem> captor = ArgumentCaptor.forClass(FlashSaleItem.class);
        verify(flashSaleItemRepo).save(captor.capture());
        assertThat(captor.getValue().getAvailableStock()).isEqualTo(2);
    }

    // ==================== Per-User Limit Checks ====================

    @Test
    void placeOrder_shouldThrowWhenPerUserLimitExceeded() {
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(1).addressId(1L).build();

        FlashSalePurchaseRecord existingRecord = FlashSalePurchaseRecord.builder()
                .recordId(1L).flashSaleItem(flashSaleItem).customer(customer)
                .quantityPurchased(2).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));
        given(customerRepo.findById(1L)).willReturn(Optional.of(customer));
        given(purchaseRecordRepo.findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(1L, 1L))
                .willReturn(Optional.of(existingRecord));

        assertThatThrownBy(() -> flashSaleService.placeFlashSaleOrder(request))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Per-user purchase limit exceeded");

        verify(orderRepo, never()).save(any());
    }

    @Test
    void placeOrder_shouldSucceedWhenExactlyAtPerUserLimit() {
        // perUserLimit = 2, already purchased 0, requesting 2
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(2).addressId(1L).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));
        given(customerRepo.findById(1L)).willReturn(Optional.of(customer));
        given(purchaseRecordRepo.findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(1L, 1L))
                .willReturn(Optional.empty());
        given(addressRepo.findById(1L)).willReturn(Optional.of(address));
        given(orderRepo.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(100L);
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        FlashSaleOrderResponseDto result = flashSaleService.placeFlashSaleOrder(request);

        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getTotalAmount()).isEqualTo(1000.0);
    }

    // ==================== Purchase Record ====================

    @Test
    void placeOrder_shouldCreateNewPurchaseRecordForFirstTimeBuyer() {
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(1).addressId(1L).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));
        given(customerRepo.findById(1L)).willReturn(Optional.of(customer));
        given(purchaseRecordRepo.findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(1L, 1L))
                .willReturn(Optional.empty());
        given(addressRepo.findById(1L)).willReturn(Optional.of(address));
        given(orderRepo.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(100L);
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        flashSaleService.placeFlashSaleOrder(request);

        ArgumentCaptor<FlashSalePurchaseRecord> captor = ArgumentCaptor.forClass(FlashSalePurchaseRecord.class);
        verify(purchaseRecordRepo).save(captor.capture());
        assertThat(captor.getValue().getQuantityPurchased()).isEqualTo(1);
        assertThat(captor.getValue().getCustomer()).isEqualTo(customer);
    }

    @Test
    void placeOrder_shouldUpdateExistingPurchaseRecordForRepeatBuyer() {
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(1).addressId(1L).build();

        FlashSalePurchaseRecord existingRecord = FlashSalePurchaseRecord.builder()
                .recordId(1L).flashSaleItem(flashSaleItem).customer(customer)
                .quantityPurchased(1).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));
        given(customerRepo.findById(1L)).willReturn(Optional.of(customer));
        given(purchaseRecordRepo.findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(1L, 1L))
                .willReturn(Optional.of(existingRecord));
        given(addressRepo.findById(1L)).willReturn(Optional.of(address));
        given(orderRepo.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(100L);
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        flashSaleService.placeFlashSaleOrder(request);

        ArgumentCaptor<FlashSalePurchaseRecord> captor = ArgumentCaptor.forClass(FlashSalePurchaseRecord.class);
        verify(purchaseRecordRepo).save(captor.capture());
        assertThat(captor.getValue().getQuantityPurchased()).isEqualTo(2);
    }

    // ==================== Not Found ====================

    @Test
    void placeOrder_shouldThrowWhenFlashSaleItemNotFound() {
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(999L).customerId(1L).quantity(1).addressId(1L).build();

        given(flashSaleItemRepo.findByIdForUpdate(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flashSaleService.placeFlashSaleOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void placeOrder_shouldThrowWhenCustomerNotFound() {
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(999L).quantity(1).addressId(1L).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));

        assertThatThrownBy(() -> flashSaleService.placeFlashSaleOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void placeOrder_shouldThrowWhenAddressNotFound() {
        FlashSaleOrderRequestDto request = FlashSaleOrderRequestDto.builder()
                .flashSaleItemId(1L).customerId(1L).quantity(1).addressId(999L).build();

        given(flashSaleItemRepo.findByIdForUpdate(1L)).willReturn(Optional.of(flashSaleItem));
        given(customerRepo.findById(1L)).willReturn(Optional.of(customer));
        given(purchaseRecordRepo.findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(1L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> flashSaleService.placeFlashSaleOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== Item CRUD ====================

    @Test
    void addItemToCampaign_shouldSetAvailableStockEqualToTotalStock() {
        FlashSaleItemDto dto = FlashSaleItemDto.builder()
                .productId(1L).flashPrice(500.0).totalStock(20).perUserLimit(2).build();

        given(campaignRepo.findById(1L)).willReturn(Optional.of(activeCampaign));
        given(productRepo.findById(1L)).willReturn(Optional.of(product));
        given(flashSaleItemRepo.save(any(FlashSaleItem.class))).willAnswer(inv -> {
            FlashSaleItem item = inv.getArgument(0);
            item.setFlashSaleItemId(1L);
            return item;
        });

        flashSaleService.addItemToCampaign(1L, dto);

        ArgumentCaptor<FlashSaleItem> captor = ArgumentCaptor.forClass(FlashSaleItem.class);
        verify(flashSaleItemRepo).save(captor.capture());
        assertThat(captor.getValue().getAvailableStock()).isEqualTo(20);
        assertThat(captor.getValue().getTotalStock()).isEqualTo(20);
    }

    @Test
    void addItemToCampaign_shouldThrowWhenCampaignNotFound() {
        FlashSaleItemDto dto = FlashSaleItemDto.builder().productId(1L).build();
        given(campaignRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flashSaleService.addItemToCampaign(999L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItemToCampaign_shouldThrowWhenProductNotFound() {
        FlashSaleItemDto dto = FlashSaleItemDto.builder().productId(999L).build();
        given(campaignRepo.findById(1L)).willReturn(Optional.of(activeCampaign));
        given(productRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flashSaleService.addItemToCampaign(1L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
