package com.vedasole.ekartecommercebackend.service.service_interface;

import com.vedasole.ekartecommercebackend.payload.FlashSaleDto;

import java.util.List;

public interface FlashSaleService {

    FlashSaleDto createFlashSale(FlashSaleDto flashSaleDto);
    FlashSaleDto updateFlashSale(FlashSaleDto flashSaleDto, long flashSaleId);
    void deleteFlashSale(long flashSaleId);
    FlashSaleDto getFlashSale(long flashSaleId);
    List<FlashSaleDto> getAllFlashSales();
    List<FlashSaleDto> getActiveFlashSales();
    List<FlashSaleDto> getUpcomingFlashSales();
}
