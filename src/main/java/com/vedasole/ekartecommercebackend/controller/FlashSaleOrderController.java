package com.vedasole.ekartecommercebackend.controller;

import com.vedasole.ekartecommercebackend.payload.FlashSaleOrderRequestDto;
import com.vedasole.ekartecommercebackend.payload.FlashSaleOrderResponseDto;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/flash-sales/orders")
@RequiredArgsConstructor
public class FlashSaleOrderController {

    private final FlashSaleService flashSaleService;

    @PostMapping
    public ResponseEntity<EntityModel<FlashSaleOrderResponseDto>> placeOrder(
            @RequestBody @Valid FlashSaleOrderRequestDto requestDto
    ) {
        FlashSaleOrderResponseDto response = flashSaleService.placeFlashSaleOrder(requestDto);
        return ResponseEntity.ok(EntityModel.of(response));
    }
}
