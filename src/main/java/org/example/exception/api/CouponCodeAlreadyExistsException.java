package org.example.exception.api;

import org.springframework.http.HttpStatus;

public class CouponCodeAlreadyExistsException extends ApiException {
    public CouponCodeAlreadyExistsException(String code) {
        super("CouponCodeExists", "Coupon code already exists: " + code);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}