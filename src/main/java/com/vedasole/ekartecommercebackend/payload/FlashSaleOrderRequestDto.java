package com.vedasole.ekartecommercebackend.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FlashSaleOrderRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long flashSaleItemId;

    private long customerId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be >= 1")
    private int quantity;

    private long addressId;
}
