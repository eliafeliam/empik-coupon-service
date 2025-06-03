package org.example.service.coupon;

import org.mapstruct.Mapper;
import org.example.api.v1.model.response.CouponResponse;
import org.example.entity.Coupon;

@Mapper(componentModel = "spring")
public interface CouponMapper {
    CouponResponse toDto(Coupon coupon);
}
