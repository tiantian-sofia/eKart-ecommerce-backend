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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Relation(itemRelation = "flashSaleItem", collectionRelation = "flashSaleItems")
public class FlashSaleItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long flashSaleItemId;

    private long campaignId;

    private long productId;

    @NotNull(message = "Flash price is required")
    @Min(value = 0, message = "Flash price must be >= 0")
    private double flashPrice;

    @Min(value = 0, message = "Total stock must be >= 0")
    private int totalStock;

    private int availableStock;

    @Min(value = 1, message = "Per-user limit must be >= 1")
    private int perUserLimit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
