package org.example.service;

import org.example.api.v1.model.request.UseCouponRequest;
import org.example.api.v1.model.response.CouponResponse;
import org.example.entity.Coupon;
import org.example.entity.CouponUsage;
import org.example.exception.api.CouponAlreadyUsedByUserException;
import org.example.exception.api.CouponCountryNotAllowedException;
import org.example.exception.api.CouponNotFoundException;
import org.example.exception.api.CouponUseLimitExceededException;
import org.example.repository.CouponRepository;
import org.example.repository.CouponUsageRepository;
import org.example.service.coupon.CouponServiceImpl;
import org.example.service.geo.CountryResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private CountryResolver countryResolver;

    @Mock
    private org.example.service.coupon.CouponMapper couponMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    @Test
    void shouldUseCouponSuccessfullyWhenAllConditionsMet() {
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("TEST")
                .maxUses(2)
                .currentUses(0)
                .country("PL")
                .createdAt(Instant.now())
                .build();
        UseCouponRequest request = new UseCouponRequest("TEST", "1.2.3.4", "user123");

        when(couponRepository.findByCodeIgnoreCaseForUpdate("TEST")).thenReturn(Optional.of(coupon));
        when(countryResolver.getCountry("1.2.3.4")).thenReturn("PL");
        when(couponUsageRepository.existsByCouponAndUserId(coupon, "user123")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(couponUsageRepository.save(any(CouponUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(couponMapper.toDto(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon c = invocation.getArgument(0);
            return new CouponResponse(
                    c.getCode(),
                    c.getCurrentUses(),
                    c.getMaxUses(),
                    c.getCreatedAt(),
                    c.getCountry()
            );
        });

        CouponResponse response = couponService.useCoupon(request);

        assertNotNull(response);
        assertEquals("TEST", response.getCode());
        assertEquals(1, response.getCurrentUses());
        assertEquals("PL", response.getCountry());

        verify(couponRepository).findByCodeIgnoreCaseForUpdate("TEST");
        verify(countryResolver).getCountry("1.2.3.4");
        verify(couponUsageRepository).existsByCouponAndUserId(coupon, "user123");
        verify(couponRepository).save(argThat(c -> c.getCurrentUses() == 1));
        verify(couponUsageRepository).save(any(CouponUsage.class));
    }

    @Test
    void shouldThrowCouponNotFoundExceptionWhenCouponDoesNotExist() {
        UseCouponRequest request = new UseCouponRequest("INVALID", "1.2.3.4", "user123");
        when(couponRepository.findByCodeIgnoreCaseForUpdate("INVALID")).thenReturn(Optional.empty());

        CouponNotFoundException ex = assertThrows(
                CouponNotFoundException.class,
                () -> couponService.useCoupon(request));
        assertEquals("Coupon not found: INVALID", ex.getMessage());

        verify(couponRepository).findByCodeIgnoreCaseForUpdate("INVALID");
        verifyNoMoreInteractions(countryResolver, couponUsageRepository, couponRepository);
    }

    @Test
    void shouldThrowCouponUseLimitExceededExceptionWhenMaxUsesReached() {
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("TEST")
                .maxUses(1)
                .currentUses(1)
                .country("PL")
                .build();
        UseCouponRequest request = new UseCouponRequest("TEST", "1.2.3.4", "user123");

        when(couponRepository.findByCodeIgnoreCaseForUpdate("TEST")).thenReturn(Optional.of(coupon));

        CouponUseLimitExceededException ex = assertThrows(
                CouponUseLimitExceededException.class,
                () -> couponService.useCoupon(request));
        assertEquals("Coupon use limit exceeded for: TEST", ex.getMessage());

        verify(couponRepository).findByCodeIgnoreCaseForUpdate("TEST");
        verifyNoMoreInteractions(countryResolver, couponUsageRepository, couponRepository);
    }

    @Test
    void shouldThrowCouponAlreadyUsedByUserExceptionWhenUserAlreadyUsedCoupon() {
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("TEST")
                .maxUses(2)
                .currentUses(0)
                .country("PL")
                .build();
        UseCouponRequest request = new UseCouponRequest("TEST", "1.2.3.4", "user123");

        when(couponRepository.findByCodeIgnoreCaseForUpdate("TEST")).thenReturn(Optional.of(coupon));
        when(countryResolver.getCountry("1.2.3.4")).thenReturn("PL");
        when(couponUsageRepository.existsByCouponAndUserId(coupon, "user123")).thenReturn(true);

        CouponAlreadyUsedByUserException ex = assertThrows(
                CouponAlreadyUsedByUserException.class,
                () -> couponService.useCoupon(request));
        assertEquals("User user123 has already used coupon: TEST", ex.getMessage());

        verify(couponRepository).findByCodeIgnoreCaseForUpdate("TEST");
        verify(countryResolver).getCountry("1.2.3.4");
        verify(couponUsageRepository).existsByCouponAndUserId(coupon, "user123");
        verifyNoMoreInteractions(couponRepository, countryResolver, couponUsageRepository);
    }

    @Test
    void shouldThrowCouponCountryNotAllowedExceptionWhenCountryDoesNotMatch() {
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("TEST")
                .maxUses(2)
                .currentUses(0)
                .country("PL")
                .build();
        UseCouponRequest request = new UseCouponRequest("TEST", "1.2.3.4", "user123");

        when(couponRepository.findByCodeIgnoreCaseForUpdate("TEST")).thenReturn(Optional.of(coupon));
        when(countryResolver.getCountry("1.2.3.4")).thenReturn("US");

        CouponCountryNotAllowedException ex = assertThrows(
                CouponCountryNotAllowedException.class,
                () -> couponService.useCoupon(request));
        assertEquals("Coupon 'TEST' not valid in country: US", ex.getMessage());

        verify(couponRepository).findByCodeIgnoreCaseForUpdate("TEST");
        verify(countryResolver).getCountry("1.2.3.4");
        verifyNoMoreInteractions(couponRepository, countryResolver, couponUsageRepository);
    }
}
