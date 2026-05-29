package com.example.asgd.service;

public record LeaseAcquireResult(
        LeaseAcquireStatus status,
        int activeDeviceCount
) {

    public static LeaseAcquireResult acquired(int activeDeviceCount) {
        return new LeaseAcquireResult(LeaseAcquireStatus.ACQUIRED, activeDeviceCount);
    }

    public static LeaseAcquireResult refreshed(int activeDeviceCount) {
        return new LeaseAcquireResult(LeaseAcquireStatus.REFRESHED, activeDeviceCount);
    }

    public static LeaseAcquireResult limitExceeded(int activeDeviceCount) {
        return new LeaseAcquireResult(LeaseAcquireStatus.LIMIT_EXCEEDED, activeDeviceCount);
    }

    public enum LeaseAcquireStatus {
        ACQUIRED,
        REFRESHED,
        LIMIT_EXCEEDED
    }
}
