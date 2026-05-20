package com.example.asgd.service.impl;

import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;
import com.example.asgd.dto.CouponClaimStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponClaimServiceTransactionTest {

    private static final AtomicLong COUPON_IDS = new AtomicLong(System.nanoTime());

    @Autowired
    private CouponClaimServiceImpl couponClaimService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentClaimsDoNotExceedStock() throws Exception {
        long couponId = nextCouponId();

        try {
            insertCoupon(couponId, 3);

            List<CouponClaimResponse> responses = runConcurrentClaims(couponId, List.of(
                    900001L, 900002L, 900003L, 900004L, 900005L, 900006L
            ));

            assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.SUCCESS).hasSize(3);
            assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.SOLD_OUT).hasSize(3);
            assertThat(availableStock(couponId)).isEqualTo(0);
            assertThat(successRecordCount(couponId)).isEqualTo(3);
        } finally {
            deleteFixture(couponId);
        }
    }

    @Test
    void concurrentDuplicateClaimsCreateOneRecordAndConsumeOneStock() throws Exception {
        long couponId = nextCouponId();
        long userId = 910001L;

        try {
            insertCoupon(couponId, 5);

            List<CouponClaimResponse> responses = runConcurrentClaims(couponId, List.of(
                    userId, userId, userId, userId, userId
            ));

            assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.SUCCESS).hasSize(1);
            assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.ALREADY_CLAIMED).hasSize(4);
            assertThat(availableStock(couponId)).isEqualTo(4);
            assertThat(recordCount(couponId, userId)).isEqualTo(1);
        } finally {
            deleteFixture(couponId);
        }
    }

    private List<CouponClaimResponse> runConcurrentClaims(long couponId, List<Long> userIds) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(userIds.size());
        try {
            CountDownLatch ready = new CountDownLatch(userIds.size());
            CountDownLatch start = new CountDownLatch(1);
            List<Future<CouponClaimResponse>> futures = new ArrayList<>();

            for (Long userId : userIds) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for concurrent claim start signal");
                    }
                    return couponClaimService.claim(new CouponClaimRequest(couponId, userId));
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<CouponClaimResponse> responses = new ArrayList<>();
            for (Future<CouponClaimResponse> future : futures) {
                responses.add(future.get(5, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            executor.shutdownNow();
        }
    }

    private void insertCoupon(long couponId, int stock) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO coupon (
                            id, coupon_name, total_stock, available_stock, status,
                            start_time, end_time, create_time, update_time
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                couponId,
                "Concurrent Coupon " + couponId,
                stock,
                stock,
                1,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                now,
                now
        );
    }

    private int availableStock(long couponId) {
        return jdbcTemplate.queryForObject(
                "SELECT available_stock FROM coupon WHERE id = ?",
                Integer.class,
                couponId
        );
    }

    private int successRecordCount(long couponId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM coupon_receive_record WHERE coupon_id = ? AND claim_status = ?",
                Integer.class,
                couponId,
                CouponClaimStatus.SUCCESS.name()
        );
    }

    private int recordCount(long couponId, long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM coupon_receive_record WHERE coupon_id = ? AND user_id = ?",
                Integer.class,
                couponId,
                userId
        );
    }

    private void deleteFixture(long couponId) {
        jdbcTemplate.update("DELETE FROM coupon_receive_record WHERE coupon_id = ?", couponId);
        jdbcTemplate.update("DELETE FROM coupon WHERE id = ?", couponId);
    }

    private static long nextCouponId() {
        return COUPON_IDS.incrementAndGet();
    }
}
