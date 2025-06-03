package org.example.exception.api;

import org.springframework.http.HttpStatus;

public class CouponUseLimitExceededException extends ApiException {
    public CouponUseLimitExceededException(String code) {
        super("CouponUseLimitExceeded", "Coupon use limit exceeded for: " + code);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}