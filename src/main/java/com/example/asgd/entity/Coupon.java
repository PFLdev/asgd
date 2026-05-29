package com.example.asgd.entity;

import java.time.LocalDateTime;
// 代金券
public record Coupon(
        Long id,
        String couponName,
        Integer totalStock,
        Integer availableStock,
        Integer status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    public boolean enabledAt(LocalDateTime now) {
        return Integer.valueOf(1).equals(status) && !now.isBefore(startTime) && !now.isAfter(endTime);
    }
}
