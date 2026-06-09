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
                @Index(name = "fsi_campaign_idx", columnList = "campaign_id"),
                @Index(name = "fsi_product_idx", columnList = "product_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_product", columnNames = {"campaign_id", "product_id"})
        }
)
public class FlashSaleItem {

    @Id
    @Column(name = "flash_sale_item_id", updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flash_sale_item_seq")
    @SequenceGenerator(name = "flash_sale_item_seq", allocationSize = 0)
    private long flashSaleItemId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private FlashSaleCampaign campaign;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Flash price is required")
    @Min(value = 0, message = "Flash price must be >= 0")
    @Column(name = "flash_price", nullable = false)
    private double flashPrice;

    @Min(value = 0, message = "Total stock must be >= 0")
    @Column(name = "total_stock", nullable = false)
    private int totalStock;

    @Min(value = 0, message = "Available stock must be >= 0")
    @Column(name = "available_stock", nullable = false)
    private int availableStock;

    @Min(value = 1, message = "Per-user limit must be >= 1")
    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit;

    @Column(name = "create_dt", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "update_dt")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "FlashSaleItem{" +
                "flashSaleItemId=" + flashSaleItemId +
                ", flashPrice=" + flashPrice +
                ", totalStock=" + totalStock +
                ", availableStock=" + availableStock +
                ", perUserLimit=" + perUserLimit +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
