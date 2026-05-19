package com.example.asgd.dto;

public record EduBenefitClaimRequest(
        Long activityId,
        String deviceId,
        Long miAccountId,
        String requestId
) {
}
