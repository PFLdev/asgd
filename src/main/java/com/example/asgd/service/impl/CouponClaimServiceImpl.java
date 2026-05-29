package com.example.asgd.service.impl;

import com.example.asgd.dao.CouponClaimMapper;
import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;
import com.example.asgd.dto.CouponClaimStatus;
import com.example.asgd.entity.Coupon;
import com.example.asgd.entity.CouponReceiveRecord;
import com.example.asgd.service.CouponClaimService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CouponClaimServiceImpl implements CouponClaimService {

    private final CouponClaimMapper couponClaimMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public CouponClaimServiceImpl(
            CouponClaimMapper couponClaimMapper,
            TransactionTemplate transactionTemplate,
            Clock clock
    ) {
        this.couponClaimMapper = couponClaimMapper;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public CouponClaimResponse claim(CouponClaimRequest request) {
        if (request == null || !positive(request.couponId()) || !positive(request.userId())) {
            return CouponClaimResponse.invalidRequest(
                    request == null ? null : request.couponId(),
                    request == null ? null : request.userId()
            );
        }

        LocalDateTime currentTime = now();
        Optional<Coupon> coupon = couponClaimMapper.findCouponById(request.couponId());
        if (coupon.isEmpty() || !coupon.get().enabledAt(currentTime)) {
            return CouponClaimResponse.unavailable(request.couponId(), request.userId());
        }
        if (couponClaimMapper.findReceiveRecord(request.couponId(), request.userId()).isPresent()) {
            return CouponClaimResponse.alreadyClaimed(request.couponId(), request.userId());
        }

        try {
            return transactionTemplate.execute(status -> claimInTransaction(request, currentTime));
        } catch (DuplicateKeyException ex) {
            return couponClaimMapper.findReceiveRecord(request.couponId(), request.userId())
                    .map(record -> CouponClaimResponse.alreadyClaimed(record.couponId(), record.userId()))
                    .orElseGet(() -> CouponClaimResponse.alreadyClaimed(request.couponId(), request.userId()));
        }
    }

    private CouponClaimResponse claimInTransaction(CouponClaimRequest request, LocalDateTime currentTime) {
        if (couponClaimMapper.findReceiveRecord(request.couponId(), request.userId()).isPresent()) {
            return CouponClaimResponse.alreadyClaimed(request.couponId(), request.userId());
        }

        int updatedRows = couponClaimMapper.decrementStock(request.couponId(), currentTime);
        if (updatedRows == 0) {
            return CouponClaimResponse.soldOut(request.couponId(), request.userId());
        }

        couponClaimMapper.insertReceiveRecord(new CouponReceiveRecord(
                null,
                request.couponId(),
                request.userId(),
                CouponClaimStatus.SUCCESS,
                currentTime,
                currentTime,
                currentTime
        ));
        return CouponClaimResponse.success(request.couponId(), request.userId());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }
}
