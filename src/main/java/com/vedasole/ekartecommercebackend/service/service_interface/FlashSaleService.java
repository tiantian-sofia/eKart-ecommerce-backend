package com.vedasole.ekartecommercebackend.service.service_interface;

import com.vedasole.ekartecommercebackend.payload.FlashSaleCampaignDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleItemDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleOrderRequestDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleOrderResponseDto;

import java.util.List;

public interface FlashSaleService {

    // Campaign CRUD
    FlashSaleCampaignDto createCampaign(FlashSaleCampaignDto dto);
    FlashSaleCampaignDto updateCampaign(long campaignId, FlashSaleCampaignDto dto);
    void deleteCampaign(long campaignId);
    FlashSaleCampaignDto getCampaign(long campaignId);
    List<FlashSaleCampaignDto> getAllCampaigns();
    List<FlashSaleCampaignDto> getActiveCampaigns();

    // Item CRUD
    FlashSaleItemDto addItemToCampaign(long campaignId, FlashSaleItemDto dto);
    FlashSaleItemDto updateItem(long flashSaleItemId, FlashSaleItemDto dto);
    void removeItem(long flashSaleItemId);
    List<FlashSaleItemDto> getItemsByCampaign(long campaignId);

    // Order placement
    FlashSaleOrderResponseDto placeFlashSaleOrder(FlashSaleOrderRequestDto requestDto);
}
