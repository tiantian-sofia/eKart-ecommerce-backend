package com.vedasole.ekartecommercebackend.service.service_interface;

import com.vedasole.ekartecommercebackend.payload.FlashSaleItemDto;

import java.util.List;

public interface FlashSaleItemService {

    FlashSaleItemDto createFlashSaleItem(long flashSaleId, FlashSaleItemDto dto);
    FlashSaleItemDto updateFlashSaleItem(long flashSaleId, long itemId, FlashSaleItemDto dto);
    void deleteFlashSaleItem(long flashSaleId, long itemId);
    FlashSaleItemDto getFlashSaleItem(long flashSaleId, long itemId);
    List<FlashSaleItemDto> getAllFlashSaleItems(long flashSaleId);
}
