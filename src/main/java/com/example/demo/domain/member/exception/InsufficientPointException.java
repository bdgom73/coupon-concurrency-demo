package com.example.demo.domain.member.exception;

import java.math.BigDecimal;

public class InsufficientPointException extends RuntimeException {
    public InsufficientPointException(BigDecimal current, BigDecimal required) {
        super("포인트가 부족합니다. 현재: " + current + ", 필요: " + required);
    }
}
