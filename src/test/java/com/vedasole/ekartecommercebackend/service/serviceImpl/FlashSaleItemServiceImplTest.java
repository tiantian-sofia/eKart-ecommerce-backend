package com.vedasole.ekartecommercebackend.service.serviceImpl;

import com.vedasole.ekartecommercebackend.entity.Category;
import com.vedasole.ekartecommercebackend.entity.FlashSale;
import com.vedasole.ekartecommercebackend.entity.FlashSaleItem;
import com.vedasole.ekartecommercebackend.entity.Product;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.payload.FlashSaleItemDto;
import com.vedasole.ekartecommercebackend.repository.FlashSaleItemRepo;
import com.vedasole.ekartecommercebackend.repository.FlashSaleRepo;
import com.vedasole.ekartecommercebackend.repository.ProductRepo;
import com.vedasole.ekartecommercebackend.service.service_impl.FlashSaleItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlashSaleItemServiceImplTest {

    @Mock
    private FlashSaleItemRepo flashSaleItemRepo;
    @Mock
    private FlashSaleRepo flashSaleRepo;
    @Mock
    private ProductRepo productRepo;

    private FlashSaleItemServiceImpl flashSaleItemService;

    private FlashSale flashSale;
    private Product product;
    private FlashSaleItem flashSaleItem;

    @BeforeEach
    void setUp() {
        flashSaleItemService = new FlashSaleItemServiceImpl(flashSaleItemRepo, flashSaleRepo, productRepo);

        Category category = Category.builder()
                .categoryId(1L)
                .name("Test Category")
                .active(true)
                .build();

        flashSale = FlashSale.builder()
                .flashSaleId(1L)
                .name("Summer Sale")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        product = Product.builder()
                .productId(1L)
                .name("Test Product")
                .image("img.jpg")
                .sku("SKU-001")
                .desc("Desc")
                .price(100.0)
                .discount(0.0)
                .qtyInStock(50)
                .category(category)
                .build();

        flashSaleItem = FlashSaleItem.builder()
                .flashSaleItemId(1L)
                .flashSale(flashSale)
                .product(product)
                .flashPrice(50.0)
                .stock(10)
                .limitPerCustomer(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createFlashSaleItem_success() {
        // Given
        given(flashSaleRepo.findById(1L)).willReturn(Optional.of(flashSale));
        given(productRepo.findById(1L)).willReturn(Optional.of(product));
        given(flashSaleItemRepo.save(any(FlashSaleItem.class))).willReturn(flashSaleItem);

        FlashSaleItemDto dto = FlashSaleItemDto.builder()
                .productId(1L)
                .flashPrice(50.0)
                .stock(10)
                .limitPerCustomer(2)
                .build();

        // When
        FlashSaleItemDto result = flashSaleItemService.createFlashSaleItem(1L, dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFlashPrice()).isEqualTo(50.0);
        assertThat(result.getStock()).isEqualTo(10);
        assertThat(result.getLimitPerCustomer()).isEqualTo(2);

        ArgumentCaptor<FlashSaleItem> captor = ArgumentCaptor.forClass(FlashSaleItem.class);
        verify(flashSaleItemRepo).save(captor.capture());
        assertThat(captor.getValue().getFlashSale()).isEqualTo(flashSale);
        assertThat(captor.getValue().getProduct()).isEqualTo(product);
    }

    @Test
    void createFlashSaleItem_flashSaleNotFound_throws() {
        // Given
        given(flashSaleRepo.findById(999L)).willReturn(Optional.empty());

        FlashSaleItemDto dto = FlashSaleItemDto.builder()
                .productId(1L)
                .flashPrice(50.0)
                .stock(10)
                .limitPerCustomer(2)
                .build();

        // When/Then
        assertThatThrownBy(() -> flashSaleItemService.createFlashSaleItem(999L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createFlashSaleItem_productNotFound_throws() {
        // Given
        given(flashSaleRepo.findById(1L)).willReturn(Optional.of(flashSale));
        given(productRepo.findById(999L)).willReturn(Optional.empty());

        FlashSaleItemDto dto = FlashSaleItemDto.builder()
                .productId(999L)
                .flashPrice(50.0)
                .stock(10)
                .limitPerCustomer(2)
                .build();

        // When/Then
        assertThatThrownBy(() -> flashSaleItemService.createFlashSaleItem(1L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateFlashSaleItem_success() {
        // Given
        given(flashSaleItemRepo.findByFlashSale_FlashSaleIdAndFlashSaleItemId(1L, 1L))
                .willReturn(Optional.of(flashSaleItem));
        given(flashSaleItemRepo.save(any(FlashSaleItem.class))).willReturn(flashSaleItem);

        FlashSaleItemDto dto = FlashSaleItemDto.builder()
                .productId(1L)
                .flashPrice(75.0)
                .stock(20)
                .limitPerCustomer(3)
                .build();

        // When
        FlashSaleItemDto result = flashSaleItemService.updateFlashSaleItem(1L, 1L, dto);

        // Then
        assertThat(result).isNotNull();
        verify(flashSaleItemRepo).save(any(FlashSaleItem.class));
    }

    @Test
    void deleteFlashSaleItem_success() {
        // Given
        given(flashSaleItemRepo.findByFlashSale_FlashSaleIdAndFlashSaleItemId(1L, 1L))
                .willReturn(Optional.of(flashSaleItem));

        // When
        flashSaleItemService.deleteFlashSaleItem(1L, 1L);

        // Then
        verify(flashSaleItemRepo).delete(flashSaleItem);
    }

    @Test
    void deleteFlashSaleItem_notFound_throws() {
        // Given
        given(flashSaleItemRepo.findByFlashSale_FlashSaleIdAndFlashSaleItemId(1L, 999L))
                .willReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> flashSaleItemService.deleteFlashSaleItem(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getFlashSaleItem_success() {
        // Given
        given(flashSaleItemRepo.findByFlashSale_FlashSaleIdAndFlashSaleItemId(1L, 1L))
                .willReturn(Optional.of(flashSaleItem));

        // When
        FlashSaleItemDto result = flashSaleItemService.getFlashSaleItem(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFlashSaleItemId()).isEqualTo(1L);
        assertThat(result.getFlashPrice()).isEqualTo(50.0);
    }

    @Test
    void getAllFlashSaleItems_returnsList() {
        // Given
        given(flashSaleRepo.findById(1L)).willReturn(Optional.of(flashSale));
        given(flashSaleItemRepo.findAllByFlashSale_FlashSaleId(1L)).willReturn(List.of(flashSaleItem));

        // When
        List<FlashSaleItemDto> result = flashSaleItemService.getAllFlashSaleItems(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFlashSaleItemId()).isEqualTo(1L);
    }

    @Test
    void getAllFlashSaleItems_flashSaleNotFound_throws() {
        // Given
        given(flashSaleRepo.findById(999L)).willReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> flashSaleItemService.getAllFlashSaleItems(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
