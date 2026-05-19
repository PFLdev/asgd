package com.example.asgd.entity;

import com.example.asgd.dto.EduBenefitGrantStatus;

import java.time.LocalDateTime;

public record EduBenefitGrantTask(
        Long id,
        String grantOrderNo,
        Long receiveRecordId,
        Long activityId,
        Long userId,
        String deviceIdHash,
        String benefitType,
        Integer memberDays,
        EduBenefitGrantStatus status,
        Integer retryCount,
        LocalDateTime nextRetryTime,
        String requestBody,
        String responseBody,
        String failReason,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
