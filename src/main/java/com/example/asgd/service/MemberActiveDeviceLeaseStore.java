package com.example.asgd.service;

public interface MemberActiveDeviceLeaseStore {

    LeaseAcquireResult acquire(
            String userId,
            String deviceIdHash,
            String sessionId,
            long nowEpochMillis,
            long leaseTtlMillis,
            int maxActiveDevices
    );

    void release(String userId, String deviceIdHash);
}
