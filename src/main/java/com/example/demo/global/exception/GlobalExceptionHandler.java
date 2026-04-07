package com.example.demo.global.exception;

import com.example.demo.domain.coupon.exception.CouponAlreadyIssuedException;
import com.example.demo.domain.coupon.exception.CouponNotFoundException;
import com.example.demo.domain.coupon.exception.CouponOutOfStockException;
import com.example.demo.domain.member.exception.InsufficientPointException;
import com.example.demo.domain.member.exception.MemberNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCouponNotFound(CouponNotFoundException e) {
        log.warn(e.getMessage());
        return ResponseEntity
                .status(ErrorCode.COUPON_NOT_FOUND.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.COUPON_NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(CouponOutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleCouponOutOfStock(CouponOutOfStockException e) {
        log.warn(e.getMessage());
        return ResponseEntity
                .status(ErrorCode.COUPON_OUT_OF_STOCK.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.COUPON_OUT_OF_STOCK));
    }

    @ExceptionHandler(CouponAlreadyIssuedException.class)
    public ResponseEntity<ErrorResponse> handleCouponAlreadyIssued(CouponAlreadyIssuedException e) {
        log.warn(e.getMessage());
        return ResponseEntity
                .status(ErrorCode.COUPON_ALREADY_ISSUED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.COUPON_ALREADY_ISSUED, e.getMessage()));
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException e) {
        log.warn(e.getMessage());
        return ResponseEntity
                .status(ErrorCode.MEMBER_NOT_FOUND.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.MEMBER_NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(InsufficientPointException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPoint(InsufficientPointException e) {
        log.warn(e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INSUFFICIENT_POINT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INSUFFICIENT_POINT, e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        log.warn("IllegalStateException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.COUPON_ISSUE_TIMEOUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.COUPON_ISSUE_TIMEOUT));
    }
}
