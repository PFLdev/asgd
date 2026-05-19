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
import com.example.asgd.service.EduBenefitClaimService;
import com.example.asgd.service.EduBenefitDistributedLockService;
import com.example.asgd.service.MemberBenefitGrantClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class EduBenefitClaimServiceImpl implements EduBenefitClaimService {

    private final EduBenefitClaimMapper claimMapper;
    private final EduBenefitCache benefitCache;
    private final EduBenefitDistributedLockService lockService;
    private final MemberBenefitGrantClient grantClient;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public EduBenefitClaimServiceImpl(
            EduBenefitClaimMapper claimMapper,
            EduBenefitCache benefitCache,
            EduBenefitDistributedLockService lockService,
            MemberBenefitGrantClient grantClient,
            TransactionTemplate transactionTemplate,
            Clock clock
    ) {
        this.claimMapper = claimMapper;
        this.benefitCache = benefitCache;
        this.lockService = lockService;
        this.grantClient = grantClient;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public EduBenefitClaimResponse getStatus(long activityId, String deviceId) {
        String deviceIdHash = hashDeviceId(deviceId);
        Optional<EduBenefitActivity> activity = claimMapper.findActivityById(activityId);
        if (activity.isEmpty() || !activity.get().enabledAt(now())) {
            return EduBenefitClaimResponse.closed("Activity is unavailable");
        }
        if (!isEligibleDevice(deviceIdHash)) {
            return EduBenefitClaimResponse.notEligible("Device is not in new-device whitelist");
        }
        if (benefitCache.mightBeClaimed(activityId, deviceIdHash)
                && benefitCache.hasClaimedCache(activityId, deviceIdHash)) {
            return EduBenefitClaimResponse.success(null, "Device benefit already claimed");
        }
        return claimMapper.findReceiveRecord(activityId, deviceIdHash)
                .map(this::responseFromRecord)
                .orElseGet(EduBenefitClaimResponse::notClaimed);
    }

    @Override
    public EduBenefitClaimResponse claim(EduBenefitClaimRequest request) {
        String deviceIdHash = hashDeviceId(request.deviceId());
        LocalDateTime currentTime = now();
        Optional<EduBenefitActivity> activity = claimMapper.findActivityById(request.activityId());
        if (activity.isEmpty() || !activity.get().enabledAt(currentTime)) {
            return EduBenefitClaimResponse.closed("Activity is unavailable");
        }
        if (!isEligibleDevice(deviceIdHash)) {
            return EduBenefitClaimResponse.notEligible("Device is not in new-device whitelist");
        }
        if (benefitCache.mightBeClaimed(request.activityId(), deviceIdHash)
                && benefitCache.hasClaimedCache(request.activityId(), deviceIdHash)) {
            return claimMapper.findReceiveRecord(request.activityId(), deviceIdHash)
                    .map(record -> EduBenefitClaimResponse.success(
                            record.grantOrderNo(),
                            "Device benefit already claimed"
                    ))
                    .orElseGet(() -> EduBenefitClaimResponse.success(null, "Device benefit already claimed"));
        }

        EduBenefitReceiveRecord record = lockService.executeWithLock(
                "edu:benefit:claim:lock:" + request.activityId() + ":" + deviceIdHash,
                () -> createReceiveRecordAndGrantTask(
                        request,
                        deviceIdHash,
                        activity.get(),
                        currentTime
                )
        );
        if (record.grantStatus() == EduBenefitGrantStatus.SUCCESS) {
            return EduBenefitClaimResponse.success(record.grantOrderNo(), "Device benefit already claimed");
        }
        return grantOutsideLock(record, activity.get());
    }

    public EduBenefitReceiveRecord createReceiveRecordAndGrantTask(
            EduBenefitClaimRequest request,
            String deviceIdHash,
            EduBenefitActivity activity,
            LocalDateTime currentTime
    ) {
        return transactionTemplate.execute(status -> {
            Optional<EduBenefitReceiveRecord> existing = claimMapper.findReceiveRecord(
                    request.activityId(),
                    deviceIdHash
            );
            if (existing.isPresent()) {
                return existing.get();
            }

            String grantOrderNo = "EDU" + request.activityId() + currentTime.toLocalDate()
                    + deviceIdHash.substring(0, 12);
            EduBenefitReceiveRecord record = new EduBenefitReceiveRecord(
                    null,
                    request.activityId(),
                    request.miAccountId(),
                    deviceIdHash,
                    request.deviceId(),
                    EduBenefitReceiveStatus.RECEIVED,
                    EduBenefitGrantStatus.NEW,
                    grantOrderNo,
                    currentTime,
                    null,
                    null,
                    null,
                    0,
                    null,
                    currentTime.plusMinutes(5),
                    currentTime,
                    currentTime
            );
            try {
                claimMapper.insertReceiveRecord(record);
            } catch (DuplicateKeyException ex) {
                return claimMapper.findReceiveRecord(request.activityId(), deviceIdHash)
                        .orElseThrow(() -> ex);
            }

            EduBenefitReceiveRecord persistedRecord = claimMapper.findReceiveRecord(request.activityId(), deviceIdHash)
                    .orElse(record);
            claimMapper.insertGrantTask(new EduBenefitGrantTask(
                    null,
                    grantOrderNo,
                    persistedRecord.id() == null ? 0L : persistedRecord.id(),
                    request.activityId(),
                    request.miAccountId(),
                    deviceIdHash,
                    activity.benefitType(),
                    activity.memberDays(),
                    EduBenefitGrantStatus.NEW,
                    0,
                    null,
                    "{\"requestId\":\"" + request.requestId() + "\"}",
                    null,
                    null,
                    currentTime,
                    currentTime
            ));
            return persistedRecord;
        });
    }

    private EduBenefitClaimResponse grantOutsideLock(EduBenefitReceiveRecord record, EduBenefitActivity activity) {
        LocalDateTime currentTime = now();
        claimMapper.markGrantProcessing(record.grantOrderNo(), currentTime);
        MemberBenefitGrantClient.GrantResult result = grantClient.grant(new MemberBenefitGrantClient.GrantRequest(
                record.grantOrderNo(),
                record.userId(),
                activity.benefitType(),
                activity.memberDays()
        ));
        if (result.success()) {
            claimMapper.markGrantSuccess(record.grantOrderNo(), currentTime);
            benefitCache.cacheClaimed(record.activityId(), record.deviceIdHash());
            return EduBenefitClaimResponse.success(record.grantOrderNo(), "Benefit granted");
        }
        claimMapper.markGrantRetrying(
                record.grantOrderNo(),
                result.message(),
                currentTime.plusMinutes(1),
                currentTime
        );
        return EduBenefitClaimResponse.retrying(record.grantOrderNo(), "Benefit grant is processing");
    }

    private EduBenefitClaimResponse responseFromRecord(EduBenefitReceiveRecord record) {
        if (record.grantStatus() == EduBenefitGrantStatus.SUCCESS) {
            return EduBenefitClaimResponse.success(record.grantOrderNo(), "Device benefit already claimed");
        }
        if (record.grantStatus() == EduBenefitGrantStatus.FAILED) {
            return new EduBenefitClaimResponse(
                    EduBenefitClaimStatus.CLOSED,
                    EduBenefitGrantStatus.FAILED,
                    record.grantOrderNo(),
                    true,
                    "Benefit grant failed"
            );
        }
        return EduBenefitClaimResponse.processing(record.grantOrderNo(), "Benefit grant is processing");
    }

    private boolean isEligibleDevice(String deviceIdHash) {
        if (!benefitCache.mightBeValidDevice(deviceIdHash)) {
            return claimMapper.existsValidNewDevice(deviceIdHash) > 0;
        }
        if (benefitCache.hasValidDeviceCache(deviceIdHash)) {
            return true;
        }
        boolean eligible = claimMapper.existsValidNewDevice(deviceIdHash) > 0;
        if (eligible) {
            benefitCache.cacheValidDevice(deviceIdHash);
        } else {
            benefitCache.cacheInvalidDevice(deviceIdHash);
        }
        return eligible;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String hashDeviceId(String deviceId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(deviceId.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
