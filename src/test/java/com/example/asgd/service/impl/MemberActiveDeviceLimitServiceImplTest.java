package com.example.asgd.service.impl;

import com.example.asgd.config.MemberActiveDeviceLimitProperties;
import com.example.asgd.dto.MemberActiveDeviceAccessRequest;
import com.example.asgd.dto.MemberActiveDeviceAccessStatus;
import com.example.asgd.dto.MemberActiveDeviceAuditEventType;
import com.example.asgd.service.LeaseAcquireResult;
import com.example.asgd.service.MemberActiveDeviceAuditRecorder;
import com.example.asgd.service.MemberActiveDeviceLeaseStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MemberActiveDeviceLimitServiceImplTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-05-29T10:00:00Z"));

    @Test
    void repeatedDeviceRefreshesLeaseAndRemainsAllowed() {
        InMemoryMemberActiveDeviceLeaseStore leaseStore = new InMemoryMemberActiveDeviceLeaseStore();
        RecordingAuditRecorder auditRecorder = new RecordingAuditRecorder();
        MemberActiveDeviceLimitServiceImpl service = newService(leaseStore, auditRecorder, failureOpenProperties());

        MemberActiveDeviceAccessRequest request = new MemberActiveDeviceAccessRequest(
                "user-001",
                "device-001",
                "session-001"
        );

        assertThat(service.acquire(request).status()).isEqualTo(MemberActiveDeviceAccessStatus.ALLOWED);
        clock.advance(Duration.ofMinutes(1));
        assertThat(service.acquire(request).status()).isEqualTo(MemberActiveDeviceAccessStatus.ALLOWED);

        assertThat(leaseStore.activeCount("user-001")).isEqualTo(1);
        assertThat(auditRecorder.events).contains("ACQUIRED", "REFRESHED");
    }

    @Test
    void newDeviceCanAcquireWhenLessThanThreeDevicesAreActive() {
        InMemoryMemberActiveDeviceLeaseStore leaseStore = new InMemoryMemberActiveDeviceLeaseStore();
        MemberActiveDeviceLimitServiceImpl service = newService(
                leaseStore,
                new RecordingAuditRecorder(),
                failureOpenProperties()
        );

        assertThat(service.acquire(request("user-001", "device-001")).status())
                .isEqualTo(MemberActiveDeviceAccessStatus.ALLOWED);
        assertThat(service.acquire(request("user-001", "device-002")).status())
                .isEqualTo(MemberActiveDeviceAccessStatus.ALLOWED);
        assertThat(service.acquire(request("user-001", "device-003")).status())
                .isEqualTo(MemberActiveDeviceAccessStatus.ALLOWED);

        assertThat(leaseStore.activeCount("user-001")).isEqualTo(3);
    }

    @Test
    void fourthDeviceIsRejectedWhenThreeDevicesAreActive() {
        InMemoryMemberActiveDeviceLeaseStore leaseStore = new InMemoryMemberActiveDeviceLeaseStore();
        RecordingAuditRecorder auditRecorder = new RecordingAuditRecorder();
        MemberActiveDeviceLimitServiceImpl service = newService(leaseStore, auditRecorder, failureOpenProperties());

        service.acquire(request("user-001", "device-001"));
        service.acquire(request("user-001", "device-002"));
        service.acquire(request("user-001", "device-003"));

        assertThat(service.acquire(request("user-001", "device-004")).status())
                .isEqualTo(MemberActiveDeviceAccessStatus.LIMIT_EXCEEDED);
        assertThat(leaseStore.activeCount("user-001")).isEqualTo(3);
        assertThat(auditRecorder.events).contains("REJECTED_LIMIT_EXCEEDED");
    }

    @Test
    void concurrentNewDevicesOnlyAllowOneRemainingSlot() throws Exception {
        InMemoryMemberActiveDeviceLeaseStore leaseStore = new InMemoryMemberActiveDeviceLeaseStore();
        MemberActiveDeviceLimitServiceImpl service = newService(
                leaseStore,
                new RecordingAuditRecorder(),
                failureOpenProperties()
        );
        service.acquire(request("user-001", "device-001"));
        service.acquire(request("user-001", "device-002"));

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<MemberActiveDeviceAccessStatus> statuses = new ArrayList<>();
        for (int i = 3; i <= 6; i++) {
            String deviceId = "device-00" + i;
            executorService.submit(() -> {
                await(start);
                MemberActiveDeviceAccessStatus status = service.acquire(request("user-001", deviceId)).status();
                synchronized (statuses) {
                    statuses.add(status);
                }
            });
        }

        start.countDown();
        executorService.shutdown();
        assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(statuses).filteredOn(MemberActiveDeviceAccessStatus.ALLOWED::equals).hasSize(1);
        assertThat(statuses).filteredOn(MemberActiveDeviceAccessStatus.LIMIT_EXCEEDED::equals).hasSize(3);
        assertThat(leaseStore.activeCount("user-001")).isEqualTo(3);
    }

    @Test
    void expiredDeviceDoesNotCountAgainstLimit() {
        InMemoryMemberActiveDeviceLeaseStore leaseStore = new InMemoryMemberActiveDeviceLeaseStore();
        MemberActiveDeviceLimitServiceImpl service = newService(
                leaseStore,
                new RecordingAuditRecorder(),
                failureOpenProperties()
        );

        service.acquire(request("user-001", "device-001"));
        service.acquire(request("user-001", "device-002"));
        service.acquire(request("user-001", "device-003"));
        clock.advance(Duration.ofMinutes(11));

        assertThat(service.acquire(request("user-001", "device-004")).status())
                .isEqualTo(MemberActiveDeviceAccessStatus.ALLOWED);
        assertThat(leaseStore.activeCount("user-001")).isEqualTo(1);
    }

    @Test
    void redisFailureCanFailOpenOrFailClosed() {
        RecordingAuditRecorder failOpenRecorder = new RecordingAuditRecorder();
        MemberActiveDeviceLimitServiceImpl failOpenService = newService(
                new FailingMemberActiveDeviceLeaseStore(),
                failOpenRecorder,
                failureOpenProperties()
        );
        assertThat(failOpenService.acquire(request("user-001", "device-001")).status())
                .isEqualTo(MemberActiveDeviceAccessStatus.DEGRADED_ALLOWED);
        assertThat(failOpenRecorder.events).contains("REDIS_UNAVAILABLE");

        RecordingAuditRecorder failClosedRecorder = new RecordingAuditRecorder();
        MemberActiveDeviceLimitServiceImpl failClosedService = newService(
                new FailingMemberActiveDeviceLeaseStore(),
                failClosedRecorder,
                failureClosedProperties()
        );
        assertThat(failClosedService.acquire(request("user-001", "device-001")).status())
                .isEqualTo(MemberActiveDeviceAccessStatus.TEMPORARILY_UNAVAILABLE);
        assertThat(failClosedRecorder.events).contains("REDIS_UNAVAILABLE");
    }

    private MemberActiveDeviceLimitServiceImpl newService(
            MemberActiveDeviceLeaseStore leaseStore,
            MemberActiveDeviceAuditRecorder auditRecorder,
            MemberActiveDeviceLimitProperties properties
    ) {
        return new MemberActiveDeviceLimitServiceImpl(leaseStore, auditRecorder, properties, clock);
    }

    private static MemberActiveDeviceAccessRequest request(String userId, String deviceId) {
        return new MemberActiveDeviceAccessRequest(userId, deviceId, "session-" + deviceId);
    }

    private static MemberActiveDeviceLimitProperties failureOpenProperties() {
        return new MemberActiveDeviceLimitProperties(true, 3, Duration.ofMinutes(10), "fail-open");
    }

    private static MemberActiveDeviceLimitProperties failureClosedProperties() {
        return new MemberActiveDeviceLimitProperties(true, 3, Duration.ofMinutes(10), "fail-closed");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static final class InMemoryMemberActiveDeviceLeaseStore implements MemberActiveDeviceLeaseStore {
        private final Map<String, Map<String, Long>> devicesByUser = new HashMap<>();

        @Override
        public synchronized LeaseAcquireResult acquire(
                String userId,
                String deviceIdHash,
                String sessionId,
                long nowEpochMillis,
                long leaseTtlMillis,
                int maxActiveDevices
        ) {
            Map<String, Long> devices = devicesByUser.computeIfAbsent(userId, ignored -> new HashMap<>());
            long expiredBefore = nowEpochMillis - leaseTtlMillis;
            devices.entrySet().removeIf(entry -> entry.getValue() <= expiredBefore);
            if (devices.containsKey(deviceIdHash)) {
                devices.put(deviceIdHash, nowEpochMillis);
                return LeaseAcquireResult.refreshed(devices.size());
            }
            if (devices.size() < maxActiveDevices) {
                devices.put(deviceIdHash, nowEpochMillis);
                return LeaseAcquireResult.acquired(devices.size());
            }
            return LeaseAcquireResult.limitExceeded(devices.size());
        }

        @Override
        public synchronized void release(String userId, String deviceIdHash) {
            Map<String, Long> devices = devicesByUser.get(userId);
            if (devices != null) {
                devices.remove(deviceIdHash);
            }
        }

        private synchronized int activeCount(String userId) {
            return devicesByUser.getOrDefault(userId, Map.of()).size();
        }
    }

    private static final class FailingMemberActiveDeviceLeaseStore implements MemberActiveDeviceLeaseStore {
        @Override
        public LeaseAcquireResult acquire(
                String userId,
                String deviceIdHash,
                String sessionId,
                long nowEpochMillis,
                long leaseTtlMillis,
                int maxActiveDevices
        ) {
            throw new IllegalStateException("Redis unavailable");
        }

        @Override
        public void release(String userId, String deviceIdHash) {
            throw new IllegalStateException("Redis unavailable");
        }
    }

    private static final class RecordingAuditRecorder implements MemberActiveDeviceAuditRecorder {
        private final List<String> events = new ArrayList<>();

        @Override
        public void record(
                MemberActiveDeviceAuditEventType eventType,
                String userId,
                String deviceIdHash,
                String sessionId,
                MemberActiveDeviceAccessStatus status
        ) {
            events.add(eventType.name());
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
