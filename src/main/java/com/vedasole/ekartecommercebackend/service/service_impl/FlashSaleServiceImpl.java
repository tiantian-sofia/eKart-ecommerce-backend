package com.vedasole.ekartecommercebackend.service.service_impl;

import com.vedasole.ekartecommercebackend.entity.FlashSale;
import com.vedasole.ekartecommercebackend.exception.APIException;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.payload.FlashSaleDto;
import com.vedasole.ekartecommercebackend.repository.FlashSaleRepo;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.vedasole.ekartecommercebackend.utility.AppConstant.RELATIONS.FLASH_SALE;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FlashSaleServiceImpl implements FlashSaleService {

    private final FlashSaleRepo flashSaleRepo;
    private final ModelMapper modelMapper;

    @Override
    public FlashSaleDto createFlashSale(FlashSaleDto dto) {
        validateTimeRange(dto.getStartTime(), dto.getEndTime());
        FlashSale flashSale = dtoToEntity(dto);
        FlashSale saved = flashSaleRepo.save(flashSale);
        return entityToDto(saved);
    }

    @Override
    public FlashSaleDto updateFlashSale(FlashSaleDto dto, long flashSaleId) {
        FlashSale existing = flashSaleRepo.findById(flashSaleId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE.getValue(), "id", flashSaleId));
        validateTimeRange(dto.getStartTime(), dto.getEndTime());
        existing.setName(dto.getName());
        existing.setStartTime(dto.getStartTime());
        existing.setEndTime(dto.getEndTime());
        FlashSale saved = flashSaleRepo.save(existing);
        return entityToDto(saved);
    }

    @Override
    public void deleteFlashSale(long flashSaleId) {
        FlashSale existing = flashSaleRepo.findById(flashSaleId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE.getValue(), "id", flashSaleId));
        flashSaleRepo.delete(existing);
    }

    @Override
    public FlashSaleDto getFlashSale(long flashSaleId) {
        FlashSale flashSale = flashSaleRepo.findById(flashSaleId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE.getValue(), "id", flashSaleId));
        return entityToDto(flashSale);
    }

    @Override
    public List<FlashSaleDto> getAllFlashSales() {
        return flashSaleRepo.findAll().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlashSaleDto> getActiveFlashSales() {
        return flashSaleRepo.findActiveFlashSales(LocalDateTime.now()).stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlashSaleDto> getUpcomingFlashSales() {
        return flashSaleRepo.findUpcomingFlashSales(LocalDateTime.now()).stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new APIException("End time must be after start time", HttpStatus.BAD_REQUEST);
        }
    }

    private FlashSale dtoToEntity(FlashSaleDto dto) {
        return modelMapper.map(dto, FlashSale.class);
    }

    private FlashSaleDto entityToDto(FlashSale entity) {
        FlashSaleDto dto = modelMapper.map(entity, FlashSaleDto.class);
        dto.setFlashSaleId(entity.getFlashSaleId());
        dto.setActive(entity.isActive());
        return dto;
    }
}
