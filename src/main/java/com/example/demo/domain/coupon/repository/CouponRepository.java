package com.example.demo.domain.coupon.repository;

import com.example.demo.domain.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * 재고를 원자적으로 1 감소.
     * stock > 0 조건으로 초과 발급 방지.
     * @return 실제로 업데이트된 행 수 (1: 성공, 0: 재고 소진)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.stock = c.stock - 1, c.version = c.version + 1 WHERE c.id = :id AND c.stock > 0")
    int decrementStock(@Param("id") Long id);
}
