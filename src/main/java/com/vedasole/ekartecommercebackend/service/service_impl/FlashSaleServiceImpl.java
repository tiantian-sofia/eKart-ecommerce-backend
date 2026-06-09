package com.vedasole.ekartecommercebackend.service.service_impl;

import com.vedasole.ekartecommercebackend.entity.*;
import com.vedasole.ekartecommercebackend.exception.APIException;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.payload.FlashSaleCampaignDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleItemDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleOrderRequestDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleOrderResponseDto;
import com.vedasole.ekartecommercebackend.repository.*;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSaleService;
import com.vedasole.ekartecommercebackend.utility.AppConstant.FlashSaleStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.vedasole.ekartecommercebackend.utility.AppConstant.OrderStatus.ORDER_CREATED;
import static com.vedasole.ekartecommercebackend.utility.AppConstant.RELATIONS;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FlashSaleServiceImpl implements FlashSaleService {

    private final FlashSaleCampaignRepo campaignRepo;
    private final FlashSaleItemRepo flashSaleItemRepo;
    private final FlashSalePurchaseRecordRepo purchaseRecordRepo;
    private final ProductRepo productRepo;
    private final CustomerRepo customerRepo;
    private final AddressRepo addressRepo;
    private final OrderRepo orderRepo;
    private final ModelMapper modelMapper;

    // ==================== Campaign CRUD ====================

    @Override
    public FlashSaleCampaignDto createCampaign(FlashSaleCampaignDto dto) {
        FlashSaleCampaign campaign = FlashSaleCampaign.builder()
                .name(dto.getName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(dto.getStatus() != null ? dto.getStatus() : FlashSaleStatus.PENDING)
                .build();
        FlashSaleCampaign saved = campaignRepo.save(campaign);
        return campaignToDto(saved);
    }

    @Override
    public FlashSaleCampaignDto updateCampaign(long campaignId, FlashSaleCampaignDto dto) {
        FlashSaleCampaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        RELATIONS.FLASH_SALE_CAMPAIGN.getValue(), "id", campaignId));
        campaign.setName(dto.getName());
        campaign.setStartTime(dto.getStartTime());
        campaign.setEndTime(dto.getEndTime());
        if (dto.getStatus() != null) {
            campaign.setStatus(dto.getStatus());
        }
        return campaignToDto(campaignRepo.save(campaign));
    }

    @Override
    public void deleteCampaign(long campaignId) {
        FlashSaleCampaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        RELATIONS.FLASH_SALE_CAMPAIGN.getValue(), "id", campaignId));
        campaignRepo.delete(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public FlashSaleCampaignDto getCampaign(long campaignId) {
        FlashSaleCampaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        RELATIONS.FLASH_SALE_CAMPAIGN.getValue(), "id", campaignId));
        return campaignToDto(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleCampaignDto> getAllCampaigns() {
        return campaignRepo.findAll().stream().map(this::campaignToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleCampaignDto> getActiveCampaigns() {
        return campaignRepo.findActiveCampaigns(LocalDateTime.now())
                .stream().map(this::campaignToDto).toList();
    }

    // ==================== Item CRUD ====================

    @Override
    public FlashSaleItemDto addItemToCampaign(long campaignId, FlashSaleItemDto dto) {
        FlashSaleCampaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        RELATIONS.FLASH_SALE_CAMPAIGN.getValue(), "id", campaignId));
        Product product = productRepo.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        RELATIONS.PRODUCT.getValue(), "id", dto.getProductId()));

        FlashSaleItem item = FlashSaleItem.builder()
                .campaign(campaign)
                .product(product)
                .flashPrice(dto.getFlashPrice())
                .totalStock(dto.getTotalStock())
                .availableStock(dto.getTotalStock())
                .perUserLimit(dto.getPerUserLimit())
                .build();
        FlashSaleItem saved = flashSaleItemRepo.save(item);
        return itemToDto(saved);
    }

    @Override
    public FlashSaleItemDto updateItem(long flashSaleItemId, FlashSaleItemDto dto) {
        FlashSaleItem item = flashSaleItemRepo.findById(flashSaleItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        RELATIONS.FLASH_SALE_ITEM.getValue(), "id", flashSaleItemId));
        item.setFlashPrice(dto.getFlashPrice());
        item.setTotalStock(dto.getTotalStock());
        item.setAvailableStock(dto.getAvailableStock());
        item.setPerUserLimit(dto.getPerUserLimit());
        return itemToDto(flashSaleItemRepo.save(item));
    }

    @Override
    public void removeItem(long flashSaleItemId) {
        FlashSaleItem item = flashSaleItemRepo.findById(flashSaleItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        RELATIONS.FLASH_SALE_ITEM.getValue(), "id", flashSaleItemId));
        flashSaleItemRepo.delete(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleItemDto> getItemsByCampaign(long campaignId) {
        return flashSaleItemRepo.findByCampaign_CampaignId(campaignId)
                .stream().map(this::itemToDto).toList();
    }

    // ==================== Flash Sale Order Placement ====================

    @Override
    public FlashSaleOrderResponseDto placeFlashSaleOrder(FlashSaleOrderRequestDto requestDto) {
        // 1. Acquire pessimistic lock on the flash sale item
        FlashSaleItem item = flashSaleItemRepo.findByIdForUpdate(requestDto.getFlashSaleItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        RELATIONS.FLASH_SALE_ITEM.getValue(), "id", requestDto.getFlashSaleItemId()));

        FlashSaleCampaign campaign = item.getCampaign();
        int quantity = requestDto.getQuantity();

        // 2. Time window check
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(campaign.getStartTime()) || !now.isBefore(campaign.getEndTime())) {
            throw new APIException("Flash sale is not active", HttpStatus.BAD_REQUEST);
        }

        // 3. Stock check
        if (item.getAvailableStock() < quantity) {
            throw new APIException("Insufficient flash sale stock", HttpStatus.CONFLICT);
        }

        // 4. Per-user limit check
        Customer customer = customerRepo.findById(requestDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        RELATIONS.CUSTOMER.getValue(), "id", requestDto.getCustomerId()));

        FlashSalePurchaseRecord record = purchaseRecordRepo
                .findByFlashSaleItem_FlashSaleItemIdAndCustomer_CustomerId(
                        item.getFlashSaleItemId(), customer.getCustomerId())
                .orElse(null);

        int alreadyPurchased = record != null ? record.getQuantityPurchased() : 0;
        if (alreadyPurchased + quantity > item.getPerUserLimit()) {
            throw new APIException("Per-user purchase limit exceeded", HttpStatus.BAD_REQUEST);
        }

        // 5. Decrement stock
        item.setAvailableStock(item.getAvailableStock() - quantity);
        flashSaleItemRepo.save(item);

        // 6. Update or create purchase record
        if (record != null) {
            record.setQuantityPurchased(alreadyPurchased + quantity);
        } else {
            record = FlashSalePurchaseRecord.builder()
                    .flashSaleItem(item)
                    .customer(customer)
                    .quantityPurchased(quantity)
                    .build();
        }
        purchaseRecordRepo.save(record);

        // 7. Create order
        Address address = addressRepo.findById(requestDto.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address", "id", requestDto.getAddressId()));

        double flashTotal = item.getFlashPrice() * quantity;
        Order order = new Order(customer, new ArrayList<>(), address, flashTotal, ORDER_CREATED);
        Order savedOrder = orderRepo.save(order);

        OrderItem orderItem = new OrderItem(savedOrder, item.getProduct(), quantity);
        savedOrder.setOrderItems(new ArrayList<>(List.of(orderItem)));
        orderRepo.save(savedOrder);

        log.debug("Flash sale order created: orderId={}, flashPrice={}, qty={}, total={}",
                savedOrder.getOrderId(), item.getFlashPrice(), quantity, flashTotal);

        // 8. Build response
        return FlashSaleOrderResponseDto.builder()
                .orderId(savedOrder.getOrderId())
                .flashSaleItemId(item.getFlashSaleItemId())
                .productName(item.getProduct().getName())
                .flashPrice(item.getFlashPrice())
                .quantity(quantity)
                .totalAmount(flashTotal)
                .orderStatus(savedOrder.getOrderStatus().getName())
                .createdAt(savedOrder.getCreatedAt())
                .build();
    }

    // ==================== Mapping Helpers ====================

    private FlashSaleCampaignDto campaignToDto(FlashSaleCampaign campaign) {
        FlashSaleCampaignDto dto = modelMapper.map(campaign, FlashSaleCampaignDto.class);
        if (campaign.getItems() != null) {
            dto.setItems(campaign.getItems().stream().map(this::itemToDto).toList());
        }
        return dto;
    }

    private FlashSaleItemDto itemToDto(FlashSaleItem item) {
        FlashSaleItemDto dto = modelMapper.map(item, FlashSaleItemDto.class);
        dto.setCampaignId(item.getCampaign().getCampaignId());
        dto.setProductId(item.getProduct().getProductId());
        return dto;
    }
}
