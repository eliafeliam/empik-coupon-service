package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "coupon_usages", uniqueConstraints = @UniqueConstraint(columnNames = {"coupon_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    @PrePersist
    @SuppressWarnings("unused")
    public void prePersist() {
        this.usedAt = Instant.now();
    }
}