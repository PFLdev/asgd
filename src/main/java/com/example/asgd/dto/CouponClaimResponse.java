package com.example.asgd.dto;

public record CouponClaimResponse(
        CouponClaimStatus status,
        Long couponId,
        Long userId,
        String message
) {

    public static CouponClaimResponse success(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.SUCCESS, couponId, userId, "Coupon claimed");
    }

    public static CouponClaimResponse alreadyClaimed(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.ALREADY_CLAIMED, couponId, userId, "Coupon already claimed");
    }

    public static CouponClaimResponse soldOut(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.SOLD_OUT, couponId, userId, "Coupon sold out");
    }

    public static CouponClaimResponse unavailable(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.UNAVAILABLE, couponId, userId, "Coupon is unavailable");
    }

    public static CouponClaimResponse invalidRequest(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.INVALID_REQUEST, couponId, userId, "Invalid coupon claim request");
    }
}
