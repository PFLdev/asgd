package com.example.asgd.service.impl;

import com.example.asgd.dao.CouponClaimMapper;
import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;
import com.example.asgd.dto.CouponClaimStatus;
import com.example.asgd.entity.Coupon;
import com.example.asgd.entity.CouponReceiveRecord;
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

import static org.assertj.core.api.Assertions.assertThat;

class CouponClaimServiceImplTest {

    @Test
    void claimCreatesReceiveRecordAndDecrementsStock() {
        FakeMapper mapper = new FakeMapper();
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(mapper), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.SUCCESS);
        assertThat(mapper.availableStock).isEqualTo(9);
        assertThat(mapper.receiveRecords).containsKey("10001:123456");
    }

    @Test
    void claimReturnsAlreadyClaimedWithoutDecrementingStockAgain() {
        FakeMapper mapper = new FakeMapper();
        mapper.receiveRecords.put("10001:123456", record(10001L, 123456L));
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(mapper), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.ALREADY_CLAIMED);
        assertThat(mapper.availableStock).isEqualTo(10);
    }

    @Test
    void claimReturnsSoldOutWhenStockCannotBeDecremented() {
        FakeMapper mapper = new FakeMapper();
        mapper.availableStock = 0;
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(mapper), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.SOLD_OUT);
        assertThat(mapper.receiveRecords).isEmpty();
    }

    @Test
    void claimReturnsUnavailableForDisabledCoupon() {
        FakeMapper mapper = new FakeMapper();
        mapper.couponStatus = 0;
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(mapper), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.UNAVAILABLE);
        assertThat(mapper.receiveRecords).isEmpty();
    }

    @Test
    void claimReturnsInvalidRequestForMissingFields() {
        FakeMapper mapper = new FakeMapper();
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(mapper), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(null, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.INVALID_REQUEST);
        assertThat(mapper.findCouponCalls).isZero();
    }

    @Test
    void duplicateKeyDuringInsertReturnsAlreadyClaimedAndTransactionRollsBack() {
        FakeMapper mapper = new FakeMapper();
        mapper.throwDuplicateOnInsert = true;
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(mapper), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.ALREADY_CLAIMED);
        assertThat(mapper.availableStock).isEqualTo(10);
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-06-10T10:00:00Z"), ZoneId.of("Asia/Shanghai"));
    }

    private static TransactionTemplate transactionTemplate(FakeMapper mapper) {
        return new TransactionTemplate(new RollbackAwareTransactionManager(mapper));
    }

    private static CouponReceiveRecord record(long couponId, long userId) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 18, 0);
        return new CouponReceiveRecord(1L, couponId, userId, CouponClaimStatus.SUCCESS, now, now, now);
    }

    private static class FakeMapper implements CouponClaimMapper {
        private final Map<String, CouponReceiveRecord> receiveRecords = new HashMap<>();
        private int availableStock = 10;
        private int couponStatus = 1;
        private int findCouponCalls;
        private boolean throwDuplicateOnInsert;

        @Override
        public Optional<Coupon> findCouponById(long couponId) {
            findCouponCalls++;
            return Optional.of(new Coupon(
                    couponId,
                    "Spring Coupon 2026",
                    10,
                    availableStock,
                    couponStatus,
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 1, 1, 0, 0)
            ));
        }

        @Override
        public Optional<CouponReceiveRecord> findReceiveRecord(long couponId, long userId) {
            return Optional.ofNullable(receiveRecords.get(couponId + ":" + userId));
        }

        @Override
        public int decrementStock(long couponId, LocalDateTime updateTime) {
            if (availableStock <= 0) {
                return 0;
            }
            availableStock--;
            return 1;
        }

        @Override
        public int insertReceiveRecord(CouponReceiveRecord record) {
            if (throwDuplicateOnInsert) {
                receiveRecords.put(record.couponId() + ":" + record.userId(), record);
                throw new DuplicateKeyException("duplicate coupon user");
            }
            receiveRecords.put(record.couponId() + ":" + record.userId(), record);
            return 1;
        }
    }

    private static class RollbackAwareTransactionManager implements PlatformTransactionManager {
        private final FakeMapper mapper;
        private int snapshotAvailableStock;
        private Map<String, CouponReceiveRecord> snapshotReceiveRecords = new HashMap<>();

        private RollbackAwareTransactionManager(FakeMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            snapshotAvailableStock = mapper.availableStock;
            snapshotReceiveRecords = new HashMap<>(mapper.receiveRecords);
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
            mapper.availableStock = snapshotAvailableStock;
            mapper.receiveRecords.clear();
            mapper.receiveRecords.putAll(snapshotReceiveRecords);
        }
    }
}
