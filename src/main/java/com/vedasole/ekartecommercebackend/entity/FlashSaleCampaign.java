package com.vedasole.ekartecommercebackend.entity;

import com.vedasole.ekartecommercebackend.utility.AppConstant.FlashSaleStatus;
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
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Validated
@Entity
@Table(
        name = "flash_sale_campaign",
        indexes = {
                @Index(name = "fsc_start_end_idx", columnList = "start_time, end_time"),
                @Index(name = "fsc_status_idx", columnList = "status")
        }
)
public class FlashSaleCampaign {

    @Id
    @Column(name = "campaign_id", updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flash_sale_campaign_seq")
    @SequenceGenerator(name = "flash_sale_campaign_seq", allocationSize = 0)
    private long campaignId;

    @NotBlank(message = "Campaign name cannot be blank")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FlashSaleStatus status;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FlashSaleItem> items;

    @Column(name = "create_dt", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "update_dt")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void validateTimeRange() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }

    @Override
    public String toString() {
        return "FlashSaleCampaign{" +
                "campaignId=" + campaignId +
                ", name='" + name + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
