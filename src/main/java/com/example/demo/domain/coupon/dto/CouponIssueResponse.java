package com.example.demo.domain.coupon.dto;

public record CouponIssueResponse(Long couponIssueId) {

    public static CouponIssueResponse of(Long couponIssueId) {
        return new CouponIssueResponse(couponIssueId);
    }
}
