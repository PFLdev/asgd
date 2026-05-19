package com.example.asgd.entity;

import com.example.asgd.dto.EduBenefitGrantStatus;
import com.example.asgd.dto.EduBenefitReceiveStatus;

import java.time.LocalDateTime;

public record EduBenefitReceiveRecord(
        Long id,
        Long activityId,
        Long userId,
        String deviceIdHash,
        String sn,
        EduBenefitReceiveStatus receiveStatus,
        EduBenefitGrantStatus grantStatus,
        String grantOrderNo,
        LocalDateTime receiveTime,
        LocalDateTime successTime,
        String failCode,
        String failReason,
        Integer retryCount,
        LocalDateTime nextRetryTime,
        LocalDateTime expireTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    public EduBenefitReceiveRecord withGrantStatus(EduBenefitGrantStatus nextStatus, LocalDateTime updateTime) {
        return new EduBenefitReceiveRecord(
                id,
                activityId,
                userId,
                deviceIdHash,
                sn,
                receiveStatus,
                nextStatus,
                grantOrderNo,
                receiveTime,
                nextStatus == EduBenefitGrantStatus.SUCCESS ? updateTime : successTime,
                failCode,
                failReason,
                retryCount,
                nextRetryTime,
                expireTime,
                createTime,
                updateTime
        );
    }
}
