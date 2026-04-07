package com.example.demo.domain.member.entity;

import com.example.demo.domain.member.exception.InsufficientPointException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "member")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal point;

    public void deductPoint(BigDecimal amount) {
        if (this.point.compareTo(amount) < 0) {
            throw new InsufficientPointException(this.point, amount);
        }
        this.point = this.point.subtract(amount);
    }
}
