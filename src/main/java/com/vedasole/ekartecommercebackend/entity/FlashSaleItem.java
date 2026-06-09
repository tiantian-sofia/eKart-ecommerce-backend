package com.vedasole.ekartecommercebackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Validated
@Entity
@Table(
        name = "flash_sale_item",
        indexes = {
                @Index(name = "fsi_flash_sale_idx", columnList = "flash_sale_id"),
                @Index(name = "fsi_product_idx", columnList = "product_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_flash_sale_product", columnNames = {"flash_sale_id", "product_id"})
        }
)
public class FlashSaleItem {

    @Id
    @Column(name = "flash_sale_item_id", updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flash_sale_item_seq")
    @SequenceGenerator(name = "flash_sale_item_seq", allocationSize = 1)
    private long flashSaleItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flash_sale_id", nullable = false)
    private FlashSale flashSale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Flash price is required")
    @Min(value = 0, message = "Flash price must not be negative")
    @Column(name = "flash_price", nullable = false)
    private double flashPrice;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must not be negative")
    @Column(name = "stock", nullable = false)
    private int stock;

    @NotNull(message = "Per-customer limit is required")
    @Min(value = 1, message = "Per-customer limit must be at least 1")
    @Column(name = "limit_per_customer", nullable = false)
    private int limitPerCustomer;

    @Column(name = "create_dt", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "update_dt")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
