package com.example.asgd.dto;

public record MemberActiveDeviceAccessRequest(
        String userId,
        String deviceId,
        String sessionId
) {
}
