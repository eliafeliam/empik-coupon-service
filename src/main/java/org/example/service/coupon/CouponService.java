package org.example.service.coupon;

import org.example.api.v1.model.request.CreateCouponRequest;
import org.example.api.v1.model.request.UseCouponRequest;
import org.example.api.v1.model.response.CouponResponse;

public interface CouponService {
    CouponResponse createCoupon(CreateCouponRequest request);
    CouponResponse useCoupon(UseCouponRequest request);
}