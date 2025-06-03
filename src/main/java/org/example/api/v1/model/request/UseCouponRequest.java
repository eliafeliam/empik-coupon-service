package org.example.api.v1.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UseCouponRequest {
    @NotBlank(message = "Code must not be blank")
    private String code;

    @NotBlank(message = "IP address must not be blank")
    private String ipAddress;

    @NotBlank(message = "User ID must not be blank")
    private String userId;
}