package org.example.api.v1.model.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private String code;
    private Integer currentUses;
    private Integer maxUses;
    private Instant createdAt;
    private String country;
}