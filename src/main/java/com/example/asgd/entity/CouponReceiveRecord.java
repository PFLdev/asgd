package com.example.asgd.entity;

import com.example.asgd.dto.CouponClaimStatus;

import java.time.LocalDateTime;

public record CouponReceiveRecord(
        Long id,
        Long couponId,
        Long userId,
        CouponClaimStatus claimStatus,
        LocalDateTime claimTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
