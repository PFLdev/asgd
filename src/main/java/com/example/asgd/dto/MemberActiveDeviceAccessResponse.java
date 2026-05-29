package com.example.asgd.dto;

public record MemberActiveDeviceAccessResponse(
        MemberActiveDeviceAccessStatus status,
        boolean allowed,
        Integer activeDeviceCount,
        String message
) {

    public static MemberActiveDeviceAccessResponse allowed(String message) {
        return new MemberActiveDeviceAccessResponse(
                MemberActiveDeviceAccessStatus.ALLOWED,
                true,
                null,
                message
        );
    }

    public static MemberActiveDeviceAccessResponse degradedAllowed() {
        return new MemberActiveDeviceAccessResponse(
                MemberActiveDeviceAccessStatus.DEGRADED_ALLOWED,
                true,
                null,
                "Member feature access allowed by fallback"
        );
    }

    public static MemberActiveDeviceAccessResponse limitExceeded(int maxActiveDevices) {
        return new MemberActiveDeviceAccessResponse(
                MemberActiveDeviceAccessStatus.LIMIT_EXCEEDED,
                false,
                maxActiveDevices,
                "Current account already has " + maxActiveDevices + " active member devices"
        );
    }

    public static MemberActiveDeviceAccessResponse temporarilyUnavailable() {
        return new MemberActiveDeviceAccessResponse(
                MemberActiveDeviceAccessStatus.TEMPORARILY_UNAVAILABLE,
                false,
                null,
                "Member feature access is temporarily unavailable"
        );
    }
}
