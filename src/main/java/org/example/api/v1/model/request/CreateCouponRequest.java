package org.example.api.v1.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {
    @NotBlank(message = "Code must not be blank")
    private String code;

    @Positive(message = "Max uses must be positive")
    private Integer maxUses;

    @NotBlank(message = "Country must not be blank")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a 2-letter ISO code")
    private String country;
}