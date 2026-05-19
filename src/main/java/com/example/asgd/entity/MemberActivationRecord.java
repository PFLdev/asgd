package com.example.asgd.entity;

import java.time.LocalDateTime;

public record MemberActivationRecord(
        Long id,
        String deviceId,
        String modelCode,
        String userId,
        LocalDateTime activatedAt
) {
}
