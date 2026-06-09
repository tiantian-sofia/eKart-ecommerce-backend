package com.vedasole.ekartecommercebackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Validated
@Entity
@Table(
        name = "flash_sale",
        indexes = {
                @Index(name = "flash_sale_time_idx", columnList = "start_time, end_time")
        }
)
public class FlashSale {

    @Id
    @Column(updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flash_sale_seq")
    @SequenceGenerator(name = "flash_sale_seq", allocationSize = 1)
    private long flashSaleId;

    @NotNull(message = "Flash sale name is required")
    @NotBlank(message = "Flash sale name cannot be blank")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "flashSale", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FlashSaleItem> flashSaleItems = new ArrayList<>();

    @Column(name = "create_dt", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "update_dt")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Returns true if the flash sale is currently active (now is between startTime and endTime).
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startTime) && now.isBefore(endTime);
    }

    /**
     * Returns true if the flash sale has not started yet.
     */
    public boolean isUpcoming() {
        return LocalDateTime.now().isBefore(startTime);
    }

    /**
     * Returns true if the flash sale has already ended.
     */
    public boolean isExpired() {
        return !LocalDateTime.now().isBefore(endTime);
    }
}
