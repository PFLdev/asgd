package com.example.asgd.service;

import com.example.asgd.dto.MemberActiveDeviceAccessStatus;
import com.example.asgd.dto.MemberActiveDeviceAuditEventType;

public interface MemberActiveDeviceAuditRecorder {

    void record(
            MemberActiveDeviceAuditEventType eventType,
            String userId,
            String deviceIdHash,
            String sessionId,
            MemberActiveDeviceAccessStatus status
    );
}
