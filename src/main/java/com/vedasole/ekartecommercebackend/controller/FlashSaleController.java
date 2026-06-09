package com.vedasole.ekartecommercebackend.controller;

import com.vedasole.ekartecommercebackend.payload.ApiResponse;
import com.vedasole.ekartecommercebackend.payload.FlashSaleDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleItemDto;
import com.vedasole.ekartecommercebackend.payload.FlashSalePurchaseRequest;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSaleItemService;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSalePurchaseService;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Validated
@RestController
@RequestMapping("/api/v1/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;
    private final FlashSaleItemService flashSaleItemService;
    private final FlashSalePurchaseService flashSalePurchaseService;

    // ============ Admin: Flash Sale CRUD ============

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<FlashSaleDto>> createFlashSale(
            @Valid @RequestBody FlashSaleDto flashSaleDto
    ) {
        FlashSaleDto created = flashSaleService.createFlashSale(flashSaleDto);
        EntityModel<FlashSaleDto> model = EntityModel.of(
                created,
                linkTo(methodOn(this.getClass()).getFlashSale(created.getFlashSaleId())).withSelfRel(),
                linkTo(methodOn(this.getClass()).getAllFlashSales()).withRel("flashSales")
        );
        return new ResponseEntity<>(model, HttpStatus.CREATED);
    }

    @PutMapping("/{flashSaleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<FlashSaleDto>> updateFlashSale(
            @PathVariable long flashSaleId,
            @Valid @RequestBody FlashSaleDto flashSaleDto
    ) {
        FlashSaleDto updated = flashSaleService.updateFlashSale(flashSaleDto, flashSaleId);
        EntityModel<FlashSaleDto> model = EntityModel.of(
                updated,
                linkTo(methodOn(this.getClass()).getFlashSale(flashSaleId)).withSelfRel(),
                linkTo(methodOn(this.getClass()).getAllFlashSales()).withRel("flashSales")
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{flashSaleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteFlashSale(@PathVariable long flashSaleId) {
        flashSaleService.deleteFlashSale(flashSaleId);
        return ResponseEntity.ok(new ApiResponse("Flash sale deleted successfully", true));
    }

    @GetMapping("/{flashSaleId}")
    public ResponseEntity<EntityModel<FlashSaleDto>> getFlashSale(@PathVariable long flashSaleId) {
        FlashSaleDto flashSale = flashSaleService.getFlashSale(flashSaleId);
        EntityModel<FlashSaleDto> model = EntityModel.of(
                flashSale,
                linkTo(methodOn(this.getClass()).getFlashSale(flashSaleId)).withSelfRel(),
                linkTo(methodOn(this.getClass()).getAllFlashSales()).withRel("flashSales"),
                linkTo(methodOn(this.getClass()).getAllFlashSaleItems(flashSaleId)).withRel("flashSaleItems")
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping
    public ResponseEntity<CollectionModel<FlashSaleDto>> getAllFlashSales() {
        List<FlashSaleDto> flashSales = flashSaleService.getAllFlashSales();
        CollectionModel<FlashSaleDto> model = CollectionModel.of(
                flashSales,
                linkTo(methodOn(this.getClass()).getAllFlashSales()).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/active")
    public ResponseEntity<CollectionModel<FlashSaleDto>> getActiveFlashSales() {
        List<FlashSaleDto> flashSales = flashSaleService.getActiveFlashSales();
        CollectionModel<FlashSaleDto> model = CollectionModel.of(
                flashSales,
                linkTo(methodOn(this.getClass()).getActiveFlashSales()).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<CollectionModel<FlashSaleDto>> getUpcomingFlashSales() {
        List<FlashSaleDto> flashSales = flashSaleService.getUpcomingFlashSales();
        CollectionModel<FlashSaleDto> model = CollectionModel.of(
                flashSales,
                linkTo(methodOn(this.getClass()).getUpcomingFlashSales()).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    // ============ Admin: Flash Sale Item CRUD ============

    @PostMapping("/{flashSaleId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<FlashSaleItemDto>> createFlashSaleItem(
            @PathVariable long flashSaleId,
            @Valid @RequestBody FlashSaleItemDto flashSaleItemDto
    ) {
        FlashSaleItemDto created = flashSaleItemService.createFlashSaleItem(flashSaleId, flashSaleItemDto);
        EntityModel<FlashSaleItemDto> model = EntityModel.of(
                created,
                linkTo(methodOn(this.getClass()).getFlashSaleItem(flashSaleId, created.getFlashSaleItemId())).withSelfRel(),
                linkTo(methodOn(this.getClass()).getAllFlashSaleItems(flashSaleId)).withRel("flashSaleItems")
        );
        return new ResponseEntity<>(model, HttpStatus.CREATED);
    }

    @PutMapping("/{flashSaleId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<FlashSaleItemDto>> updateFlashSaleItem(
            @PathVariable long flashSaleId,
            @PathVariable long itemId,
            @Valid @RequestBody FlashSaleItemDto flashSaleItemDto
    ) {
        FlashSaleItemDto updated = flashSaleItemService.updateFlashSaleItem(flashSaleId, itemId, flashSaleItemDto);
        EntityModel<FlashSaleItemDto> model = EntityModel.of(
                updated,
                linkTo(methodOn(this.getClass()).getFlashSaleItem(flashSaleId, itemId)).withSelfRel(),
                linkTo(methodOn(this.getClass()).getAllFlashSaleItems(flashSaleId)).withRel("flashSaleItems")
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{flashSaleId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteFlashSaleItem(
            @PathVariable long flashSaleId,
            @PathVariable long itemId
    ) {
        flashSaleItemService.deleteFlashSaleItem(flashSaleId, itemId);
        return ResponseEntity.ok(new ApiResponse("Flash sale item deleted successfully", true));
    }

    @GetMapping("/{flashSaleId}/items/{itemId}")
    public ResponseEntity<EntityModel<FlashSaleItemDto>> getFlashSaleItem(
            @PathVariable long flashSaleId,
            @PathVariable long itemId
    ) {
        FlashSaleItemDto item = flashSaleItemService.getFlashSaleItem(flashSaleId, itemId);
        EntityModel<FlashSaleItemDto> model = EntityModel.of(
                item,
                linkTo(methodOn(this.getClass()).getFlashSaleItem(flashSaleId, itemId)).withSelfRel(),
                linkTo(methodOn(this.getClass()).getAllFlashSaleItems(flashSaleId)).withRel("flashSaleItems")
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/{flashSaleId}/items")
    public ResponseEntity<CollectionModel<FlashSaleItemDto>> getAllFlashSaleItems(
            @PathVariable long flashSaleId
    ) {
        List<FlashSaleItemDto> items = flashSaleItemService.getAllFlashSaleItems(flashSaleId);
        CollectionModel<FlashSaleItemDto> model = CollectionModel.of(
                items,
                linkTo(methodOn(this.getClass()).getAllFlashSaleItems(flashSaleId)).withSelfRel(),
                linkTo(methodOn(this.getClass()).getFlashSale(flashSaleId)).withRel("flashSale")
        );
        return ResponseEntity.ok(model);
    }

    // ============ User: Flash Sale Purchase ============

    @PostMapping("/{saleId}/items/{itemId}/purchase")
    public ResponseEntity<ApiResponse> purchaseFlashSaleItem(
            @PathVariable long saleId,
            @PathVariable long itemId,
            @Valid @RequestBody FlashSalePurchaseRequest request
    ) {
        String checkoutUrl = flashSalePurchaseService.purchaseFlashSaleItem(
                saleId, itemId, request.getQuantity(), request.getCustomerId()
        );
        return new ResponseEntity<>(
                new ApiResponse("Flash sale checkout session created: " + checkoutUrl, true),
                HttpStatus.CREATED
        );
    }
}
