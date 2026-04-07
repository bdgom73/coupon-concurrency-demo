package com.example.demo.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Coupon
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_001", "존재하지 않는 쿠폰입니다."),
    COUPON_OUT_OF_STOCK(HttpStatus.CONFLICT, "COUPON_002", "쿠폰 재고가 소진되었습니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "COUPON_003", "이미 발급된 쿠폰입니다."),
    COUPON_ISSUE_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "COUPON_004", "동시 요청 혼잡으로 발급에 실패했습니다. 다시 시도해주세요."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "존재하지 않는 회원입니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "MEMBER_002", "포인트가 부족합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode()           { return code; }
    public String getMessage()        { return message; }
}
