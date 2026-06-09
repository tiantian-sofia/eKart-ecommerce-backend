package com.vedasole.ekartecommercebackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Validated
@Entity
@Table(
        name = "flash_sale_purchase",
        indexes = {
                @Index(name = "fsp_item_customer_idx", columnList = "flash_sale_item_id, customer_id"),
                @Index(name = "fsp_order_idx", columnList = "order_id")
        }
)
public class FlashSalePurchase {

    @Id
    @Column(updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flash_sale_purchase_seq")
    @SequenceGenerator(name = "flash_sale_purchase_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flash_sale_item_id", nullable = false)
    private FlashSaleItem flashSaleItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "order_id", nullable = false)
    private long orderId;

    @Column(name = "flash_price_total", nullable = false)
    private double flashPriceTotal;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime purchasedAt;
}
