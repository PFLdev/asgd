package com.example.asgd.service.impl;

import com.example.asgd.dto.MemberActiveDeviceAccessStatus;
import com.example.asgd.dto.MemberActiveDeviceAuditEventType;
import com.example.asgd.service.MemberActiveDeviceAuditRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingMemberActiveDeviceAuditRecorder implements MemberActiveDeviceAuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(LoggingMemberActiveDeviceAuditRecorder.class);

    @Override
    public void record(
            MemberActiveDeviceAuditEventType eventType,
            String userId,
            String deviceIdHash,
            String sessionId,
            MemberActiveDeviceAccessStatus status
    ) {
        log.info(
                "memberActiveDevice eventType={} userId={} deviceIdHash={} sessionId={} status={}",
                eventType,
                userId,
                deviceIdHash,
                sessionId,
                status
        );
    }
}
