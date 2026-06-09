package com.vedasole.ekartecommercebackend.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.server.core.Relation;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Relation(itemRelation = "flashSaleItem", collectionRelation = "flashSaleItems")
public class FlashSaleItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long flashSaleItemId;

    private long flashSaleId;

    @NotNull(message = "Product ID is required")
    @Min(value = 1, message = "Product ID must be positive")
    private long productId;

    private String productName;

    @NotNull(message = "Flash price is required")
    @Min(value = 0, message = "Flash price must not be negative")
    private double flashPrice;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must not be negative")
    private int stock;

    @NotNull(message = "Per-customer limit is required")
    @Min(value = 1, message = "Per-customer limit must be at least 1")
    private int limitPerCustomer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
