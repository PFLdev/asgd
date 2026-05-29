package com.example.asgd.service.impl;

import com.example.asgd.service.LeaseAcquireResult;
import com.example.asgd.service.MemberActiveDeviceLeaseStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisMemberActiveDeviceLeaseStore implements MemberActiveDeviceLeaseStore {

    private static final String ACTIVE_DEVICES_KEY_PREFIX = "member:active_devices:";
    private static final String ACTIVE_DEVICE_DETAIL_KEY_PREFIX = "member:active_device:";

    private static final DefaultRedisScript<List> ACQUIRE_SCRIPT = new DefaultRedisScript<>("""
            local activeKey = KEYS[1]
            local detailKey = KEYS[2]
            local deviceHash = ARGV[1]
            local sessionId = ARGV[2]
            local nowMillis = tonumber(ARGV[3])
            local leaseTtlMillis = tonumber(ARGV[4])
            local maxActiveDevices = tonumber(ARGV[5])
            local expiredBefore = nowMillis - leaseTtlMillis

            redis.call('ZREMRANGEBYSCORE', activeKey, '-inf', expiredBefore)

            if redis.call('ZSCORE', activeKey, deviceHash) then
                redis.call('ZADD', activeKey, nowMillis, deviceHash)
                redis.call('HSET', detailKey, 'sessionId', sessionId, 'lastSeenAt', nowMillis, 'leaseUntil', nowMillis + leaseTtlMillis)
                redis.call('PEXPIRE', detailKey, leaseTtlMillis)
                redis.call('PEXPIRE', activeKey, leaseTtlMillis)
                return {2, redis.call('ZCARD', activeKey)}
            end

            local activeCount = redis.call('ZCARD', activeKey)
            if activeCount < maxActiveDevices then
                redis.call('ZADD', activeKey, nowMillis, deviceHash)
                redis.call('HSET', detailKey, 'sessionId', sessionId, 'lastSeenAt', nowMillis, 'leaseUntil', nowMillis + leaseTtlMillis)
                redis.call('PEXPIRE', detailKey, leaseTtlMillis)
                redis.call('PEXPIRE', activeKey, leaseTtlMillis)
                return {1, redis.call('ZCARD', activeKey)}
            end

            return {3, activeCount}
            """, List.class);

    private final StringRedisTemplate redisTemplate;

    public RedisMemberActiveDeviceLeaseStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LeaseAcquireResult acquire(
            String userId,
            String deviceIdHash,
            String sessionId,
            long nowEpochMillis,
            long leaseTtlMillis,
            int maxActiveDevices
    ) {
        List<?> result = redisTemplate.execute(
                ACQUIRE_SCRIPT,
                List.of(activeDevicesKey(userId), activeDeviceDetailKey(userId, deviceIdHash)),
                deviceIdHash,
                sessionId == null ? "" : sessionId,
                String.valueOf(nowEpochMillis),
                String.valueOf(leaseTtlMillis),
                String.valueOf(maxActiveDevices)
        );
        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Redis active-device script returned no result");
        }

        int code = toInt(result.get(0));
        int activeCount = toInt(result.get(1));
        return switch (code) {
            case 1 -> LeaseAcquireResult.acquired(activeCount);
            case 2 -> LeaseAcquireResult.refreshed(activeCount);
            case 3 -> LeaseAcquireResult.limitExceeded(activeCount);
            default -> throw new IllegalStateException("Unexpected Redis active-device script result: " + code);
        };
    }

    @Override
    public void release(String userId, String deviceIdHash) {
        redisTemplate.opsForZSet().remove(activeDevicesKey(userId), deviceIdHash);
        redisTemplate.delete(activeDeviceDetailKey(userId, deviceIdHash));
    }

    private static String activeDevicesKey(String userId) {
        return ACTIVE_DEVICES_KEY_PREFIX + userId;
    }

    private static String activeDeviceDetailKey(String userId, String deviceIdHash) {
        return ACTIVE_DEVICE_DETAIL_KEY_PREFIX + userId + ":" + deviceIdHash;
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
