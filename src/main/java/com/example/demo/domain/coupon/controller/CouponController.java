package com.example.demo.domain.coupon.controller;

import com.example.demo.domain.coupon.dto.CouponIssueRequest;
import com.example.demo.domain.coupon.dto.CouponIssueResponse;
import com.example.demo.domain.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/api/coupon")
    public ResponseEntity<CouponIssueResponse> issueCoupon(
            @RequestParam Long couponId,
            @RequestParam Long memberId
    ) {
        log.info("쿠폰 발급 요청 — couponId={}, memberId={}", couponId, memberId);
        CouponIssueResponse response = couponService.issueCoupon(new CouponIssueRequest(couponId, memberId));
        return ResponseEntity.ok(response);
    }
}
