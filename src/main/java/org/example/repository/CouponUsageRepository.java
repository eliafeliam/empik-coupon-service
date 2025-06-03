package org.example.repository;

import org.example.entity.Coupon;
import org.example.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM CouponUsage u WHERE u.coupon = :coupon AND u.userId = :userId")
    boolean existsByCouponAndUserId(@Param("coupon") Coupon coupon, @Param("userId") String userId);
}