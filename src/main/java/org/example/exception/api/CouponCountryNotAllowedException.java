package org.example.exception.api;

import org.springframework.http.HttpStatus;

public class CouponCountryNotAllowedException extends ApiException {
    public CouponCountryNotAllowedException(String code, String country) {
        super("CouponCountryNotAllowed", "Coupon '" + code + "' not valid in country: " + country);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
