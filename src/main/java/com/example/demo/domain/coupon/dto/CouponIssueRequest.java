package com.example.demo.domain.coupon.dto;

public record CouponIssueRequest(
        Long couponId,
        Long memberId
) {
}
