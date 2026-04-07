package com.example.demo.domain.coupon.repository;

import com.example.demo.domain.coupon.entity.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    Optional<CouponIssue> findByCouponIdAndMemberId(Long couponId, Long memberId);

    boolean existsByCouponIdAndMemberId(Long couponId, Long memberId);

}
