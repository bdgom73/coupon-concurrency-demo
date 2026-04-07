package com.example.demo.domain.coupon.entity;

import com.example.demo.domain.coupon.exception.CouponOutOfStockException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "coupon")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Coupon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal cost;

    @Column(nullable = false)
    private Integer stock;

    @Version
    private Long version;

    public void decreaseStock() {
        if (this.stock <= 0) {
            throw new CouponOutOfStockException();
        }
        this.stock--;
    }
}
