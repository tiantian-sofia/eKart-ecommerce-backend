package com.vedasole.ekartecommercebackend.controller;

import com.vedasole.ekartecommercebackend.payload.FlashSaleCampaignDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleItemDto;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/flash-sales/campaigns")
@RequiredArgsConstructor
public class FlashSaleCampaignController {

    private final FlashSaleService flashSaleService;

    // ==================== Campaign Endpoints ====================

    @PostMapping
    public ResponseEntity<EntityModel<FlashSaleCampaignDto>> createCampaign(
            @RequestBody @Valid FlashSaleCampaignDto dto
    ) {
        FlashSaleCampaignDto created = flashSaleService.createCampaign(dto);
        EntityModel<FlashSaleCampaignDto> model = EntityModel.of(
                created,
                linkTo(methodOn(this.getClass()).getCampaign(created.getCampaignId())).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<EntityModel<FlashSaleCampaignDto>> updateCampaign(
            @PathVariable long campaignId,
            @RequestBody @Valid FlashSaleCampaignDto dto
    ) {
        FlashSaleCampaignDto updated = flashSaleService.updateCampaign(campaignId, dto);
        EntityModel<FlashSaleCampaignDto> model = EntityModel.of(
                updated,
                linkTo(methodOn(this.getClass()).getCampaign(campaignId)).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable long campaignId) {
        flashSaleService.deleteCampaign(campaignId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<EntityModel<FlashSaleCampaignDto>> getCampaign(
            @PathVariable long campaignId
    ) {
        FlashSaleCampaignDto campaign = flashSaleService.getCampaign(campaignId);
        EntityModel<FlashSaleCampaignDto> model = EntityModel.of(
                campaign,
                linkTo(methodOn(this.getClass()).getCampaign(campaignId)).withSelfRel(),
                linkTo(methodOn(this.getClass()).getItems(campaignId)).withRel("items")
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping
    public ResponseEntity<CollectionModel<FlashSaleCampaignDto>> getAllCampaigns() {
        List<FlashSaleCampaignDto> campaigns = flashSaleService.getAllCampaigns();
        CollectionModel<FlashSaleCampaignDto> model = CollectionModel.of(
                campaigns,
                linkTo(methodOn(this.getClass()).getAllCampaigns()).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/active")
    public ResponseEntity<CollectionModel<FlashSaleCampaignDto>> getActiveCampaigns() {
        List<FlashSaleCampaignDto> campaigns = flashSaleService.getActiveCampaigns();
        CollectionModel<FlashSaleCampaignDto> model = CollectionModel.of(
                campaigns,
                linkTo(methodOn(this.getClass()).getActiveCampaigns()).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    // ==================== Item Endpoints ====================

    @PostMapping("/{campaignId}/items")
    public ResponseEntity<EntityModel<FlashSaleItemDto>> addItem(
            @PathVariable long campaignId,
            @RequestBody @Valid FlashSaleItemDto dto
    ) {
        FlashSaleItemDto created = flashSaleService.addItemToCampaign(campaignId, dto);
        EntityModel<FlashSaleItemDto> model = EntityModel.of(created);
        return ResponseEntity.ok(model);
    }

    @PutMapping("/{campaignId}/items/{itemId}")
    public ResponseEntity<EntityModel<FlashSaleItemDto>> updateItem(
            @PathVariable long campaignId,
            @PathVariable long itemId,
            @RequestBody @Valid FlashSaleItemDto dto
    ) {
        FlashSaleItemDto updated = flashSaleService.updateItem(itemId, dto);
        EntityModel<FlashSaleItemDto> model = EntityModel.of(updated);
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{campaignId}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable long campaignId,
            @PathVariable long itemId
    ) {
        flashSaleService.removeItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{campaignId}/items")
    public ResponseEntity<CollectionModel<FlashSaleItemDto>> getItems(
            @PathVariable long campaignId
    ) {
        List<FlashSaleItemDto> items = flashSaleService.getItemsByCampaign(campaignId);
        CollectionModel<FlashSaleItemDto> model = CollectionModel.of(items);
        return ResponseEntity.ok(model);
    }
}
