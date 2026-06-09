package com.vedasole.ekartecommercebackend.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FlashSalePurchaseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private long flashSaleItemId;
    private long customerId;
    private int quantity;
    private long orderId;
    private double flashPriceTotal;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime purchasedAt;
}
