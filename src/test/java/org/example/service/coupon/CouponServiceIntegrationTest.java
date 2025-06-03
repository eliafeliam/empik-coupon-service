package org.example.service.coupon;

import org.example.api.v1.model.request.CreateCouponRequest;
import org.example.api.v1.model.request.UseCouponRequest;
import org.example.api.v1.model.response.CouponResponse;
import org.example.exception.api.CouponAlreadyUsedByUserException;
import org.example.exception.api.CouponCountryNotAllowedException;
import org.example.exception.api.CouponUseLimitExceededException;
import org.example.service.geo.GeoIpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Sql("/clear-database.sql")
class CouponServiceIntegrationTest {

    @SuppressWarnings("unused")
    @Autowired
    private CouponService couponService;

    @SuppressWarnings("unused")
    @MockBean
    private GeoIpService geoIpService;

    @SuppressWarnings("unused")
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        when(geoIpService.getCountry("1.2.3.4")).thenReturn("PL");
        when(geoIpService.getCountry("2.2.2.2")).thenReturn("US");
    }

    @Test
    void createCouponAndUseOnce_HappyPath() {
        CreateCouponRequest createReq = new CreateCouponRequest();
        createReq.setCode("TEST1");
        createReq.setMaxUses(2);
        createReq.setCountry("PL");
        CouponResponse created = couponService.createCoupon(createReq);

        assertNotNull(created);
        assertEquals("TEST1", created.getCode());
        assertEquals(0, created.getCurrentUses());

        UseCouponRequest useReq = new UseCouponRequest("TEST1", "1.2.3.4", "user123");
        CouponResponse used = couponService.useCoupon(useReq);
        assertEquals(1, used.getCurrentUses());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer usageCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM coupon_usages WHERE coupon_id = (SELECT id FROM coupons WHERE code = ?) AND user_id = ?",
                Integer.class, "TEST1", "user123");
        assertEquals(1, usageCount);
    }

    @Test
    void secondUseWithinLimit_Success() {
        CreateCouponRequest createReq = new CreateCouponRequest();
        createReq.setCode("TEST2");
        createReq.setMaxUses(2);
        createReq.setCountry("PL");
        couponService.createCoupon(createReq);

        UseCouponRequest useReq1 = new UseCouponRequest("TEST2", "1.2.3.4", "user123");
        couponService.useCoupon(useReq1);

        UseCouponRequest useReq2 = new UseCouponRequest("TEST2", "1.2.3.4", "user456");
        CouponResponse used = couponService.useCoupon(useReq2);
        assertEquals(2, used.getCurrentUses());
    }

    @Test
    void thirdUse_ThrowsUseLimitExceeded() {
        CreateCouponRequest createReq = new CreateCouponRequest();
        createReq.setCode("TEST3");
        createReq.setMaxUses(2);
        createReq.setCountry("PL");
        couponService.createCoupon(createReq);

        UseCouponRequest useReq1 = new UseCouponRequest("TEST3", "1.2.3.4", "user123");
        couponService.useCoupon(useReq1);

        UseCouponRequest useReq2 = new UseCouponRequest("TEST3", "1.2.3.4", "user456");
        couponService.useCoupon(useReq2);

        UseCouponRequest useReq3 = new UseCouponRequest("TEST3", "1.2.3.4", "user789");
        CouponUseLimitExceededException ex = assertThrows(
                CouponUseLimitExceededException.class,
                () -> couponService.useCoupon(useReq3));
        assertEquals("Coupon use limit exceeded for: TEST3", ex.getMessage());
    }

    @Test
    void sameUserSecondTime_ThrowsAlreadyUsed() {
        CreateCouponRequest createReq = new CreateCouponRequest();
        createReq.setCode("TEST4");
        createReq.setMaxUses(2);
        createReq.setCountry("PL");
        couponService.createCoupon(createReq);

        UseCouponRequest useReq1 = new UseCouponRequest("TEST4", "1.2.3.4", "user123");
        couponService.useCoupon(useReq1);

        UseCouponRequest useReq2 = new UseCouponRequest("TEST4", "1.2.3.4", "user123");
        CouponAlreadyUsedByUserException ex = assertThrows(
                CouponAlreadyUsedByUserException.class,
                () -> couponService.useCoupon(useReq2));
        assertEquals("User user123 has already used coupon: TEST4", ex.getMessage());
    }

    @Test
    void useFromWrongCountry_ThrowsCountryNotAllowed() {
        CreateCouponRequest createReq = new CreateCouponRequest();
        createReq.setCode("TEST5");
        createReq.setMaxUses(2);
        createReq.setCountry("PL");
        couponService.createCoupon(createReq);

        UseCouponRequest useReq = new UseCouponRequest("TEST5", "2.2.2.2", "userABC");
        CouponCountryNotAllowedException ex = assertThrows(
                CouponCountryNotAllowedException.class,
                () -> couponService.useCoupon(useReq));
        assertEquals("Coupon 'TEST5' not valid in country: US", ex.getMessage());
    }
}