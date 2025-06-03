package org.example.api.v1.controller;

import jakarta.validation.Valid;
import org.example.api.v1.model.request.CreateCouponRequest;
import org.example.api.v1.model.request.UseCouponRequest;
import org.example.api.v1.model.response.CouponResponse;
import org.example.service.coupon.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/coupons")
@SuppressWarnings("unused")
public class CouponController {

    private final CouponService couponService;

    @Autowired
    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> createCoupon(@RequestBody @Valid CreateCouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @PostMapping("/{code}/redeem")
    public ResponseEntity<CouponResponse> redeemCoupon(@PathVariable String code, @RequestBody @Valid UseCouponRequest request) {
        request.setCode(code);
        return ResponseEntity.ok(couponService.useCoupon(request));
    }
}