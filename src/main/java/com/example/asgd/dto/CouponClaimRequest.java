package com.example.asgd.dto;

public record CouponClaimRequest(
        Long couponId,
        Long userId
) {
}
