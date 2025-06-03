package org.example.service.coupon;

import org.example.api.v1.model.request.CreateCouponRequest;
import org.example.api.v1.model.request.UseCouponRequest;
import org.example.api.v1.model.response.CouponResponse;
import org.example.entity.Coupon;
import org.example.entity.CouponUsage;
import org.example.exception.api.CouponAlreadyUsedByUserException;
import org.example.exception.api.CouponCodeAlreadyExistsException;
import org.example.exception.api.CouponCountryNotAllowedException;
import org.example.exception.api.CouponNotFoundException;
import org.example.exception.api.CouponUseLimitExceededException;
import org.example.repository.CouponRepository;
import org.example.repository.CouponUsageRepository;
import org.example.service.geo.CountryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CouponServiceImpl implements CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponServiceImpl.class);

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CountryResolver countryResolver;
    private final CouponMapper couponMapper;

    public CouponServiceImpl(
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            CountryResolver countryResolver,
            CouponMapper couponMapper
    ) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.countryResolver = countryResolver;
        this.couponMapper = couponMapper;
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new CouponCodeAlreadyExistsException(normalizedCode);
        }

        Coupon coupon = Coupon.builder()
                .code(normalizedCode)
                .maxUses(request.getMaxUses())
                .country(request.getCountry().trim().toUpperCase())
                .currentUses(0)
                .createdAt(Instant.now())
                .build();

        couponRepository.save(coupon);
        logger.info("Coupon created: {}", coupon.getCode());

        return couponMapper.toDto(coupon);
    }

    @Override
    @Transactional
    public CouponResponse useCoupon(UseCouponRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase();
        logger.info("User {} attempts to use coupon {} from IP {}", request.getUserId(), normalizedCode, request.getIpAddress());

        Coupon coupon = couponRepository.findByCodeIgnoreCaseForUpdate(normalizedCode)
                .orElseThrow(() -> new CouponNotFoundException(normalizedCode));

        if (coupon.getCurrentUses() >= coupon.getMaxUses()) {
            throw new CouponUseLimitExceededException(coupon.getCode());
        }

        String userCountry = countryResolver.getCountry(request.getIpAddress());
        if (!coupon.getCountry().equalsIgnoreCase(userCountry)) {
            throw new CouponCountryNotAllowedException(coupon.getCode(), userCountry);
        }

        if (couponUsageRepository.existsByCouponAndUserId(coupon, request.getUserId())) {
            throw new CouponAlreadyUsedByUserException(request.getUserId(), coupon.getCode());
        }

        coupon.setCurrentUses(coupon.getCurrentUses() + 1);
        couponRepository.save(coupon);

        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .userId(request.getUserId())
                .build();
        couponUsageRepository.save(usage);

        return couponMapper.toDto(coupon);
    }
}
