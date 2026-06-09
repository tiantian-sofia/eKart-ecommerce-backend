package com.vedasole.ekartecommercebackend.service.service_impl;

import com.vedasole.ekartecommercebackend.entity.FlashSale;
import com.vedasole.ekartecommercebackend.entity.FlashSaleItem;
import com.vedasole.ekartecommercebackend.entity.Product;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.payload.FlashSaleItemDto;
import com.vedasole.ekartecommercebackend.repository.FlashSaleItemRepo;
import com.vedasole.ekartecommercebackend.repository.FlashSaleRepo;
import com.vedasole.ekartecommercebackend.repository.ProductRepo;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSaleItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.vedasole.ekartecommercebackend.utility.AppConstant.RELATIONS.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FlashSaleItemServiceImpl implements FlashSaleItemService {

    private final FlashSaleItemRepo flashSaleItemRepo;
    private final FlashSaleRepo flashSaleRepo;
    private final ProductRepo productRepo;

    @Override
    public FlashSaleItemDto createFlashSaleItem(long flashSaleId, FlashSaleItemDto dto) {
        FlashSale flashSale = flashSaleRepo.findById(flashSaleId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE.getValue(), "id", flashSaleId));

        Product product = productRepo.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT.getValue(), "id", dto.getProductId()));

        FlashSaleItem item = FlashSaleItem.builder()
                .flashSale(flashSale)
                .product(product)
                .flashPrice(dto.getFlashPrice())
                .stock(dto.getStock())
                .limitPerCustomer(dto.getLimitPerCustomer())
                .build();

        FlashSaleItem saved = flashSaleItemRepo.save(item);
        return entityToDto(saved);
    }

    @Override
    public FlashSaleItemDto updateFlashSaleItem(long flashSaleId, long itemId, FlashSaleItemDto dto) {
        FlashSaleItem existing = flashSaleItemRepo
                .findByFlashSale_FlashSaleIdAndFlashSaleItemId(flashSaleId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE_ITEM.getValue(), "id", itemId));

        if (dto.getProductId() > 0 && dto.getProductId() != existing.getProduct().getProductId()) {
            Product product = productRepo.findById(dto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(PRODUCT.getValue(), "id", dto.getProductId()));
            existing.setProduct(product);
        }
        existing.setFlashPrice(dto.getFlashPrice());
        existing.setStock(dto.getStock());
        existing.setLimitPerCustomer(dto.getLimitPerCustomer());

        FlashSaleItem saved = flashSaleItemRepo.save(existing);
        return entityToDto(saved);
    }

    @Override
    public void deleteFlashSaleItem(long flashSaleId, long itemId) {
        FlashSaleItem existing = flashSaleItemRepo
                .findByFlashSale_FlashSaleIdAndFlashSaleItemId(flashSaleId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE_ITEM.getValue(), "id", itemId));
        flashSaleItemRepo.delete(existing);
    }

    @Override
    public FlashSaleItemDto getFlashSaleItem(long flashSaleId, long itemId) {
        FlashSaleItem item = flashSaleItemRepo
                .findByFlashSale_FlashSaleIdAndFlashSaleItemId(flashSaleId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE_ITEM.getValue(), "id", itemId));
        return entityToDto(item);
    }

    @Override
    public List<FlashSaleItemDto> getAllFlashSaleItems(long flashSaleId) {
        flashSaleRepo.findById(flashSaleId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE.getValue(), "id", flashSaleId));
        return flashSaleItemRepo.findAllByFlashSale_FlashSaleId(flashSaleId).stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    private FlashSaleItemDto entityToDto(FlashSaleItem entity) {
        return FlashSaleItemDto.builder()
                .flashSaleItemId(entity.getFlashSaleItemId())
                .flashSaleId(entity.getFlashSale().getFlashSaleId())
                .productId(entity.getProduct().getProductId())
                .productName(entity.getProduct().getName())
                .flashPrice(entity.getFlashPrice())
                .stock(entity.getStock())
                .limitPerCustomer(entity.getLimitPerCustomer())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
