package com.vedasole.ekartecommercebackend.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FlashSaleOrderResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long orderId;
    private long flashSaleItemId;
    private String productName;
    private double flashPrice;
    private int quantity;
    private double totalAmount;
    private String orderStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
