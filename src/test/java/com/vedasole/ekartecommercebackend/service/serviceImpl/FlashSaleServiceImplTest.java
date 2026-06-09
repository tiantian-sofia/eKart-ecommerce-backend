package com.vedasole.ekartecommercebackend.service.serviceImpl;

import com.vedasole.ekartecommercebackend.entity.FlashSale;
import com.vedasole.ekartecommercebackend.exception.APIException;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.payload.FlashSaleDto;
import com.vedasole.ekartecommercebackend.repository.FlashSaleRepo;
import com.vedasole.ekartecommercebackend.service.service_impl.FlashSaleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlashSaleServiceImplTest {

    @Mock
    private FlashSaleRepo flashSaleRepo;

    private final ModelMapper modelMapper = new ModelMapper();

    private FlashSaleServiceImpl flashSaleService;

    @BeforeEach
    void setUp() {
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        flashSaleService = new FlashSaleServiceImpl(flashSaleRepo, modelMapper);
    }

    @Test
    void createFlashSale_success() {
        // Given
        FlashSaleDto dto = FlashSaleDto.builder()
                .name("Summer Sale")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();

        FlashSale savedEntity = FlashSale.builder()
                .flashSaleId(1L)
                .name("Summer Sale")
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(flashSaleRepo.save(any(FlashSale.class))).willReturn(savedEntity);

        // When
        FlashSaleDto result = flashSaleService.createFlashSale(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Summer Sale");
        verify(flashSaleRepo).save(any(FlashSale.class));
    }

    @Test
    void createFlashSale_endBeforeStart_throws() {
        // Given
        FlashSaleDto dto = FlashSaleDto.builder()
                .name("Bad Sale")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        // When/Then
        assertThatThrownBy(() -> flashSaleService.createFlashSale(dto))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void updateFlashSale_success() {
        // Given
        long flashSaleId = 1L;
        FlashSale existing = FlashSale.builder()
                .flashSaleId(flashSaleId)
                .name("Old Name")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(flashSaleRepo.findById(flashSaleId)).willReturn(Optional.of(existing));

        FlashSaleDto dto = FlashSaleDto.builder()
                .name("New Name")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(3))
                .build();

        given(flashSaleRepo.save(any(FlashSale.class))).willReturn(existing);

        // When
        FlashSaleDto result = flashSaleService.updateFlashSale(dto, flashSaleId);

        // Then
        assertThat(result).isNotNull();
        verify(flashSaleRepo).save(any(FlashSale.class));
    }

    @Test
    void updateFlashSale_notFound_throws() {
        // Given
        long flashSaleId = 999L;
        given(flashSaleRepo.findById(flashSaleId)).willReturn(Optional.empty());

        FlashSaleDto dto = FlashSaleDto.builder()
                .name("Test")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();

        // When/Then
        assertThatThrownBy(() -> flashSaleService.updateFlashSale(dto, flashSaleId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteFlashSale_success() {
        // Given
        long flashSaleId = 1L;
        FlashSale existing = FlashSale.builder().flashSaleId(flashSaleId).build();
        given(flashSaleRepo.findById(flashSaleId)).willReturn(Optional.of(existing));

        // When
        flashSaleService.deleteFlashSale(flashSaleId);

        // Then
        verify(flashSaleRepo).delete(existing);
    }

    @Test
    void deleteFlashSale_notFound_throws() {
        // Given
        long flashSaleId = 999L;
        given(flashSaleRepo.findById(flashSaleId)).willReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> flashSaleService.deleteFlashSale(flashSaleId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getFlashSale_success() {
        // Given
        long flashSaleId = 1L;
        FlashSale flashSale = FlashSale.builder()
                .flashSaleId(flashSaleId)
                .name("Test Sale")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .build();
        given(flashSaleRepo.findById(flashSaleId)).willReturn(Optional.of(flashSale));

        // When
        FlashSaleDto result = flashSaleService.getFlashSale(flashSaleId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Sale");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void getFlashSale_notFound_throws() {
        // Given
        long flashSaleId = 999L;
        given(flashSaleRepo.findById(flashSaleId)).willReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> flashSaleService.getFlashSale(flashSaleId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActiveFlashSales_returnsList() {
        // Given
        FlashSale active1 = FlashSale.builder()
                .flashSaleId(1L)
                .name("Active 1")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        given(flashSaleRepo.findActiveFlashSales(any(LocalDateTime.class)))
                .willReturn(List.of(active1));

        // When
        List<FlashSaleDto> result = flashSaleService.getActiveFlashSales();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Active 1");
    }

    @Test
    void getUpcomingFlashSales_returnsList() {
        // Given
        FlashSale upcoming = FlashSale.builder()
                .flashSaleId(2L)
                .name("Upcoming")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();
        given(flashSaleRepo.findUpcomingFlashSales(any(LocalDateTime.class)))
                .willReturn(List.of(upcoming));

        // When
        List<FlashSaleDto> result = flashSaleService.getUpcomingFlashSales();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Upcoming");
    }
}
