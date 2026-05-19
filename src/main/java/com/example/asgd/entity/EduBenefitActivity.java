package com.example.asgd.entity;

import java.time.LocalDateTime;

public record EduBenefitActivity(
        Long id,
        String activityCode,
        String activityName,
        String benefitType,
        String benefitValue,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer status
) {

    public boolean enabledAt(LocalDateTime now) {
        return Integer.valueOf(1).equals(status) && !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    public int memberDays() {
        return Integer.parseInt(benefitValue);
    }
}
