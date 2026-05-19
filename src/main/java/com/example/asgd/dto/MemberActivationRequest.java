package com.example.asgd.dto;

public record MemberActivationRequest(
        String deviceId,
        String modelCode,
        String userId
) {
}
