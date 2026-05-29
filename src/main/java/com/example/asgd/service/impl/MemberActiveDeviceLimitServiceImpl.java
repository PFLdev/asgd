package com.example.asgd.service.impl;

import com.example.asgd.config.MemberActiveDeviceLimitProperties;
import com.example.asgd.dto.MemberActiveDeviceAccessRequest;
import com.example.asgd.dto.MemberActiveDeviceAccessResponse;
import com.example.asgd.dto.MemberActiveDeviceAccessStatus;
import com.example.asgd.dto.MemberActiveDeviceAuditEventType;
import com.example.asgd.service.LeaseAcquireResult;
import com.example.asgd.service.MemberActiveDeviceAuditRecorder;
import com.example.asgd.service.MemberActiveDeviceLeaseStore;
import com.example.asgd.service.MemberActiveDeviceLimitService;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class MemberActiveDeviceLimitServiceImpl implements MemberActiveDeviceLimitService {

    private final MemberActiveDeviceLeaseStore leaseStore;
    private final MemberActiveDeviceAuditRecorder auditRecorder;
    private final MemberActiveDeviceLimitProperties properties;
    private final Clock clock;

    public MemberActiveDeviceLimitServiceImpl(
            MemberActiveDeviceLeaseStore leaseStore,
            MemberActiveDeviceAuditRecorder auditRecorder,
            MemberActiveDeviceLimitProperties properties,
            Clock clock
    ) {
        this.leaseStore = leaseStore;
        this.auditRecorder = auditRecorder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public MemberActiveDeviceAccessResponse acquire(MemberActiveDeviceAccessRequest request) {
        if (!properties.isEnabled()) {
            return MemberActiveDeviceAccessResponse.allowed("Member active device limit is disabled");
        }

        String deviceIdHash = MemberActiveDeviceIdHasher.hash(request.deviceId());
        try {
            LeaseAcquireResult result = leaseStore.acquire(
                    request.userId(),
                    deviceIdHash,
                    request.sessionId(),
                    clock.millis(),
                    properties.getLeaseTtl().toMillis(),
                    properties.getMaxActiveDevices()
            );
            return responseFromResult(request, deviceIdHash, result);
        } catch (RuntimeException ex) {
            auditRecorder.record(
                    MemberActiveDeviceAuditEventType.REDIS_UNAVAILABLE,
                    request.userId(),
                    deviceIdHash,
                    request.sessionId(),
                    properties.failOpen()
                            ? MemberActiveDeviceAccessStatus.DEGRADED_ALLOWED
                            : MemberActiveDeviceAccessStatus.TEMPORARILY_UNAVAILABLE
            );
            if (properties.failOpen()) {
                return MemberActiveDeviceAccessResponse.degradedAllowed();
            }
            return MemberActiveDeviceAccessResponse.temporarilyUnavailable();
        }
    }

    @Override
    public MemberActiveDeviceAccessResponse release(MemberActiveDeviceAccessRequest request) {
        String deviceIdHash = MemberActiveDeviceIdHasher.hash(request.deviceId());
        leaseStore.release(request.userId(), deviceIdHash);
        auditRecorder.record(
                MemberActiveDeviceAuditEventType.RELEASED,
                request.userId(),
                deviceIdHash,
                request.sessionId(),
                MemberActiveDeviceAccessStatus.ALLOWED
        );
        return MemberActiveDeviceAccessResponse.allowed("Member device lease released");
    }

    private MemberActiveDeviceAccessResponse responseFromResult(
            MemberActiveDeviceAccessRequest request,
            String deviceIdHash,
            LeaseAcquireResult result
    ) {
        if (result.status() == LeaseAcquireResult.LeaseAcquireStatus.LIMIT_EXCEEDED) {
            auditRecorder.record(
                    MemberActiveDeviceAuditEventType.REJECTED_LIMIT_EXCEEDED,
                    request.userId(),
                    deviceIdHash,
                    request.sessionId(),
                    MemberActiveDeviceAccessStatus.LIMIT_EXCEEDED
            );
            return MemberActiveDeviceAccessResponse.limitExceeded(properties.getMaxActiveDevices());
        }

        MemberActiveDeviceAuditEventType eventType = result.status() == LeaseAcquireResult.LeaseAcquireStatus.ACQUIRED
                ? MemberActiveDeviceAuditEventType.ACQUIRED
                : MemberActiveDeviceAuditEventType.REFRESHED;
        auditRecorder.record(
                eventType,
                request.userId(),
                deviceIdHash,
                request.sessionId(),
                MemberActiveDeviceAccessStatus.ALLOWED
        );
        return new MemberActiveDeviceAccessResponse(
                MemberActiveDeviceAccessStatus.ALLOWED,
                true,
                result.activeDeviceCount(),
                "Member feature access allowed"
        );
    }
}
