package com.example.asgd.service.impl;

import com.example.asgd.dao.EduBenefitClaimMapper;
import com.example.asgd.dto.EduBenefitClaimRequest;
import com.example.asgd.dto.EduBenefitClaimResponse;
import com.example.asgd.dto.EduBenefitClaimStatus;
import com.example.asgd.dto.EduBenefitGrantStatus;
import com.example.asgd.dto.EduBenefitReceiveStatus;
import com.example.asgd.entity.EduBenefitActivity;
import com.example.asgd.entity.EduBenefitGrantTask;
import com.example.asgd.entity.EduBenefitReceiveRecord;
import com.example.asgd.service.EduBenefitCache;
import com.example.asgd.service.EduBenefitDistributedLockService;
import com.example.asgd.service.MemberBenefitGrantClient;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

class EduBenefitClaimServiceImplTest {

    @Test
    void claimCreatesReceiveRecordGrantTaskAndMarksSuccess() {
        FakeMapper mapper = new FakeMapper();
        EduBenefitClaimServiceImpl service = new EduBenefitClaimServiceImpl(
                mapper,
                new FakeCache(),
                new InlineLockService(),
                request -> MemberBenefitGrantClient.GrantResult.granted(),
                transactionTemplate(),
                fixedClock()
        );

        EduBenefitClaimResponse response = service.claim(new EduBenefitClaimRequest(
                10001L,
                "edu-device-service-001",
                123456L,
                "request-service-001"
        ));

        assertThat(response.status()).isEqualTo(EduBenefitClaimStatus.SUCCESS);
        assertThat(response.grantStatus()).isEqualTo(EduBenefitGrantStatus.SUCCESS);
        assertThat(response.grantOrderNo()).isNotBlank();
        assertThat(mapper.receiveRecords).hasSize(1);
        assertThat(mapper.grantTasks).hasSize(1);
        assertThat(mapper.receiveRecords.values().iterator().next().grantStatus()).isEqualTo(EduBenefitGrantStatus.SUCCESS);
    }

    @Test
    void claimReturnsExistingRecordWhenDeviceAlreadyClaimed() {
        FakeMapper mapper = new FakeMapper();
        mapper.existingRecord = new EduBenefitReceiveRecord(
                7L,
                10001L,
                123456L,
                "hash-existing",
                "edu-device-existing",
                EduBenefitReceiveStatus.RECEIVED,
                EduBenefitGrantStatus.SUCCESS,
                "EDU10001hash-existing",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                0,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        FakeCache cache = new FakeCache();
        EduBenefitClaimServiceImpl service = new EduBenefitClaimServiceImpl(
                mapper,
                cache,
                new InlineLockService(),
                request -> MemberBenefitGrantClient.GrantResult.granted(),
                transactionTemplate(),
                fixedClock()
        );

        EduBenefitClaimResponse response = service.claim(new EduBenefitClaimRequest(
                10001L,
                "edu-device-existing",
                999999L,
                "request-service-002"
        ));

        assertThat(response.status()).isEqualTo(EduBenefitClaimStatus.SUCCESS);
        assertThat(response.message()).isEqualTo("Device benefit already claimed");
        assertThat(mapper.receiveRecords).isEmpty();
    }

    @Test
    void statusReturnsNotEligibleWhenWhitelistMisses() {
        FakeMapper mapper = new FakeMapper();
        mapper.whitelistEnabled = false;
        EduBenefitClaimServiceImpl service = new EduBenefitClaimServiceImpl(
                mapper,
                new FakeCache(),
                new InlineLockService(),
                request -> MemberBenefitGrantClient.GrantResult.granted(),
                transactionTemplate(),
                fixedClock()
        );

        EduBenefitClaimResponse response = service.getStatus(10001L, "normal-device-service-001");

        assertThat(response.status()).isEqualTo(EduBenefitClaimStatus.NOT_ELIGIBLE);
        assertThat(response.eligible()).isFalse();
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-06-10T10:00:00Z"), ZoneId.of("Asia/Shanghai"));
    }

    private static TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new NoopTransactionManager());
    }

    private static class FakeMapper implements EduBenefitClaimMapper {

        private final Map<String, EduBenefitReceiveRecord> receiveRecords = new HashMap<>();
        private final Map<String, EduBenefitGrantTask> grantTasks = new HashMap<>();
        private EduBenefitReceiveRecord existingRecord;
        private boolean whitelistEnabled = true;

        @Override
        public Optional<EduBenefitActivity> findActivityById(long activityId) {
            return Optional.of(new EduBenefitActivity(
                    activityId,
                    "EDU_MEMBER_2026",
                    "Education Member 2026",
                    "EDU_MEMBER",
                    "365",
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                    1
            ));
        }

        @Override
        public int existsValidNewDevice(String deviceIdHash) {
            return whitelistEnabled ? 1 : 0;
        }

        @Override
        public Optional<EduBenefitReceiveRecord> findReceiveRecord(long activityId, String deviceIdHash) {
            return Optional.ofNullable(existingRecord);
        }

        @Override
        public int insertReceiveRecord(EduBenefitReceiveRecord record) {
            if (existingRecord != null) {
                throw new DuplicateKeyException("duplicate device");
            }
            receiveRecords.put(record.grantOrderNo(), record);
            existingRecord = record;
            return 1;
        }

        @Override
        public int insertGrantTask(EduBenefitGrantTask task) {
            grantTasks.put(task.grantOrderNo(), task);
            return 1;
        }

        @Override
        public int markGrantProcessing(String grantOrderNo, LocalDateTime updateTime) {
            return 1;
        }

        @Override
        public int markGrantSuccess(String grantOrderNo, LocalDateTime successTime) {
            EduBenefitReceiveRecord record = receiveRecords.get(grantOrderNo);
            if (record != null) {
                receiveRecords.put(grantOrderNo, record.withGrantStatus(EduBenefitGrantStatus.SUCCESS, successTime));
            }
            return 1;
        }

        @Override
        public int markGrantRetrying(String grantOrderNo, String failReason, LocalDateTime nextRetryTime, LocalDateTime updateTime) {
            return 1;
        }
    }

    private static class FakeCache implements EduBenefitCache {

        @Override
        public boolean mightBeValidDevice(String deviceIdHash) {
            return true;
        }

        @Override
        public boolean hasValidDeviceCache(String deviceIdHash) {
            return false;
        }

        @Override
        public void cacheValidDevice(String deviceIdHash) {
        }

        @Override
        public void cacheInvalidDevice(String deviceIdHash) {
        }

        @Override
        public boolean mightBeClaimed(long activityId, String deviceIdHash) {
            return false;
        }

        @Override
        public boolean hasClaimedCache(long activityId, String deviceIdHash) {
            return false;
        }

        @Override
        public void cacheClaimed(long activityId, String deviceIdHash) {
        }
    }

    private static class InlineLockService implements EduBenefitDistributedLockService {

        @Override
        public <T> T executeWithLock(String lockKey, Callable<T> callback) {
            try {
                return callback.call();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static class NoopTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
