package com.example.asgd.service.impl;

import com.example.asgd.dao.CouponClaimMapper;
import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;
import com.example.asgd.dto.CouponClaimStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponClaimServiceTransactionTest {

    @Autowired
    private CouponClaimServiceImpl couponClaimService;

    @Autowired
    private CouponClaimMapper couponClaimMapper;

    @Test
    void concurrentClaimsDoNotExceedStock() throws Exception {
        long couponId = 20001L;
        insertCoupon(couponId, 3);

        List<CouponClaimResponse> responses = runConcurrentClaims(couponId, List.of(
                900001L, 900002L, 900003L, 900004L, 900005L, 900006L
        ));

        assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.SUCCESS).hasSize(3);
        assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.SOLD_OUT).hasSize(3);
        assertThat(couponClaimMapper.findCouponById(couponId)).get().extracting("availableStock").isEqualTo(0);
    }

    @Test
    void concurrentDuplicateClaimsCreateOneRecordAndConsumeOneStock() throws Exception {
        long couponId = 20002L;
        insertCoupon(couponId, 5);

        List<CouponClaimResponse> responses = runConcurrentClaims(couponId, List.of(
                910001L, 910001L, 910001L, 910001L, 910001L
        ));

        assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.SUCCESS).hasSize(1);
        assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.ALREADY_CLAIMED).hasSize(4);
        assertThat(couponClaimMapper.findCouponById(couponId)).get().extracting("availableStock").isEqualTo(4);
    }

    private List<CouponClaimResponse> runConcurrentClaims(long couponId, List<Long> userIds) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(userIds.size());
        CountDownLatch ready = new CountDownLatch(userIds.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CouponClaimResponse>> futures = new ArrayList<>();

        for (Long userId : userIds) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return couponClaimService.claim(new CouponClaimRequest(couponId, userId));
            }));
        }

        ready.await();
        start.countDown();

        List<CouponClaimResponse> responses = new ArrayList<>();
        for (Future<CouponClaimResponse> future : futures) {
            responses.add(future.get());
        }
        executor.shutdown();
        return responses;
    }

    private void insertCoupon(long couponId, int stock) {
        couponClaimMapper.insertCouponForTest(
                couponId,
                "Concurrent Coupon " + couponId,
                stock,
                stock,
                1,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
