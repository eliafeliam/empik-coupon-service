package org.example.exception.api;

import org.springframework.http.HttpStatus;

public class CouponAlreadyUsedByUserException extends ApiException {
    public CouponAlreadyUsedByUserException(String userId, String code) {
        super("CouponAlreadyUsed", "User " + userId + " has already used coupon: " + code);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
