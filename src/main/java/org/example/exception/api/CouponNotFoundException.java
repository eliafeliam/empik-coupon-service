package org.example.exception.api;

import org.springframework.http.HttpStatus;

public class CouponNotFoundException extends ApiException {
    public CouponNotFoundException(String code) {
        super("CouponNotFound", "Coupon not found: " + code);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}