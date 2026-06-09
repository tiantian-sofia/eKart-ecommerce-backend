package com.vedasole.ekartecommercebackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
        name = "flash_sale_purchase_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_flash_item_customer",
                        columnNames = {"flash_sale_item_id", "customer_id"}
                )
        }
)
public class FlashSalePurchaseRecord {

    @Id
    @Column(name = "record_id", updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flash_sale_purchase_record_seq")
    @SequenceGenerator(name = "flash_sale_purchase_record_seq", allocationSize = 0)
    private long recordId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "flash_sale_item_id", nullable = false)
    private FlashSaleItem flashSaleItem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Min(value = 0, message = "Quantity purchased must be >= 0")
    @Column(name = "quantity_purchased", nullable = false)
    private int quantityPurchased;

    @Column(name = "create_dt", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "update_dt")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "FlashSalePurchaseRecord{" +
                "recordId=" + recordId +
                ", quantityPurchased=" + quantityPurchased +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
