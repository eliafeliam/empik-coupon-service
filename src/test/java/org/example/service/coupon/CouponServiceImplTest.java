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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private CountryResolver countryResolver;

    @Mock
    private CouponMapper couponMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        coupon = Coupon.builder()
                .id(1L)
                .code("TEST")
                .maxUses(2)
                .currentUses(0)
                .country("PL")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void createCoupon_Success() {
        CreateCouponRequest createReq = new CreateCouponRequest();
        createReq.setCode("TEST");
        createReq.setMaxUses(5);
        createReq.setCountry("PL");

        when(couponRepository.existsByCodeIgnoreCase("TEST")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon c = invocation.getArgument(0);
            c.setId(2L);
            return c;
        });
        // настраиваем mapper: когда придёт любой Coupon, вернуть соответствующий DTO
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

        CouponResponse resp = couponService.createCoupon(createReq);

        assertNotNull(resp);
        assertEquals("TEST", resp.getCode());
        assertEquals("PL", resp.getCountry());
        assertEquals(0, resp.getCurrentUses());
        assertEquals(5, resp.getMaxUses());
        assertNotNull(resp.getCreatedAt());

        verify(couponRepository).save(argThat(c ->
                c.getCode().equals("TEST") &&
                        c.getCurrentUses() == 0 &&
                        c.getMaxUses() == 5 &&
                        c.getCountry().equals("PL")
        ));
    }

    @Test
    void createCoupon_ThrowsWhenCodeExists() {
        CreateCouponRequest createReq = new CreateCouponRequest();
        createReq.setCode("TEST");
        createReq.setMaxUses(1);
        createReq.setCountry("PL");

        when(couponRepository.existsByCodeIgnoreCase("TEST")).thenReturn(true);

        CouponCodeAlreadyExistsException ex = assertThrows(
                CouponCodeAlreadyExistsException.class,
                () -> couponService.createCoupon(createReq)
        );
        assertEquals("Coupon code already exists: TEST", ex.getMessage());

        verify(couponRepository).existsByCodeIgnoreCase("TEST");
        verifyNoMoreInteractions(couponRepository);
    }

    @Test
    void useCoupon_Success() {
        UseCouponRequest request = new UseCouponRequest("TEST", "1.2.3.4", "user123");
        when(couponRepository.findByCodeIgnoreCaseForUpdate("TEST"))
                .thenReturn(Optional.of(coupon));
        when(countryResolver.getCountry("1.2.3.4")).thenReturn("PL");
        when(couponUsageRepository.existsByCouponAndUserId(coupon, "user123"))
                .thenReturn(false);
        when(couponRepository.save(any(Coupon.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(couponUsageRepository.save(any(CouponUsage.class)))
                .thenAnswer(i -> i.getArgument(0));
        // настраиваем mapper
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

        InOrder inOrder = inOrder(couponRepository, countryResolver, couponUsageRepository);
        inOrder.verify(couponRepository).findByCodeIgnoreCaseForUpdate("TEST");
        inOrder.verify(countryResolver).getCountry("1.2.3.4");
        inOrder.verify(couponUsageRepository).existsByCouponAndUserId(coupon, "user123");
        inOrder.verify(couponRepository).save(argThat(c -> c.getCurrentUses() == 1));
        inOrder.verify(couponUsageRepository).save(any(CouponUsage.class));
    }

    @Test
    void useCoupon_ThrowsNotFound() {
        UseCouponRequest request = new UseCouponRequest("MISSING", "1.2.3.4", "user123");
        when(couponRepository.findByCodeIgnoreCaseForUpdate("MISSING"))
                .thenReturn(Optional.empty());

        CouponNotFoundException ex = assertThrows(
                CouponNotFoundException.class,
                () -> couponService.useCoupon(request)
        );
        assertEquals("Coupon not found: MISSING", ex.getMessage());

        verify(couponRepository).findByCodeIgnoreCaseForUpdate("MISSING");
        verifyNoMoreInteractions(countryResolver, couponUsageRepository, couponRepository);
    }

    @Test
    void useCoupon_ThrowsUseLimitExceeded() {
        coupon.setCurrentUses(1);
        coupon.setMaxUses(1);
        UseCouponRequest request = new UseCouponRequest("TEST", "1.2.3.4", "user123");

        when(couponRepository.findByCodeIgnoreCaseForUpdate("TEST"))
                .thenReturn(Optional.of(coupon));

        CouponUseLimitExceededException ex = assertThrows(
                CouponUseLimitExceededException.class,
                () -> couponService.useCoupon(request)
        );
        assertEquals("Coupon use limit exceeded for: TEST", ex.getMessage());

        verify(couponRepository).findByCodeIgnoreCaseForUpdate("TEST");
        verifyNoMoreInteractions(countryResolver, couponUsageRepository, couponRepository);
    }

    @Test
    void useCoupon_ThrowsAlreadyUsedBySameUser() {
        when(couponRepository.findByCodeIgnoreCaseForUpdate("TEST"))
                .thenReturn(Optional.of(coupon));
        when(countryResolver.getCountry("1.2.3.4")).thenReturn("PL");
        when(couponUsageRepository.existsByCouponAndUserId(coupon, "user123"))
                .thenReturn(true);

        UseCouponRequest request = new UseCouponRequest("TEST", "1.2.3.4", "user123");
        CouponAlreadyUsedByUserException ex = assertThrows(
                CouponAlreadyUsedByUserException.class,
                () -> couponService.useCoupon(request)
        );
        assertEquals("User user123 has already used coupon: TEST", ex.getMessage());

        verify(couponRepository).findByCodeIgnoreCaseForUpdate("TEST");
        verify(countryResolver).getCountry("1.2.3.4");
        verify(couponUsageRepository).existsByCouponAndUserId(coupon, "user123");
        verifyNoMoreInteractions(couponRepository, countryResolver, couponUsageRepository);
    }

    @Test
    void useCoupon_ThrowsCountryNotAllowed() {
        when(couponRepository.findByCodeIgnoreCaseForUpdate("TEST"))
                .thenReturn(Optional.of(coupon));
        when(countryResolver.getCountry("1.2.3.4")).thenReturn("US");

        UseCouponRequest request = new UseCouponRequest("TEST", "1.2.3.4", "user123");
        CouponCountryNotAllowedException ex = assertThrows(
                CouponCountryNotAllowedException.class,
                () -> couponService.useCoupon(request)
        );
        assertEquals("Coupon 'TEST' not valid in country: US", ex.getMessage());

        verify(couponRepository).findByCodeIgnoreCaseForUpdate("TEST");
        verify(countryResolver).getCountry("1.2.3.4");
        verifyNoMoreInteractions(couponRepository, countryResolver, couponUsageRepository);
    }
}
