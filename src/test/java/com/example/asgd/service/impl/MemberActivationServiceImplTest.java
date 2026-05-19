package com.example.asgd.service.impl;

import com.example.asgd.dao.MemberActivationMapper;
import com.example.asgd.dto.ActivationStatus;
import com.example.asgd.dto.MemberActivationRequest;
import com.example.asgd.dto.MemberActivationResponse;
import com.example.asgd.entity.MemberActivationRecord;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberActivationServiceImplTest {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 6, 30, 23, 59, 59);

    @Test
    void activateSucceedsForEducationModelDuringCampaign() {
        FakeMemberActivationMapper mapper = new FakeMemberActivationMapper(true, false);
        MemberActivationServiceImpl service = new MemberActivationServiceImpl(
                mapper,
                fixedClock("2026-06-10T10:00:00Z"),
                START_TIME,
                END_TIME
        );

        MemberActivationResponse response = service.activate(new MemberActivationRequest(
                "device-001",
                "EDU-PAD-2026",
                "user-001"
        ));

        assertThat(response.status()).isEqualTo(ActivationStatus.SUCCESS);
        assertThat(response.message()).isEqualTo("Activation successful");
        assertThat(mapper.insertCalled).isTrue();
    }

    @Test
    void activateRejectsDuplicateDevice() {
        FakeMemberActivationMapper mapper = new FakeMemberActivationMapper(true, true);
        MemberActivationServiceImpl service = new MemberActivationServiceImpl(
                mapper,
                fixedClock("2026-06-10T10:00:00Z"),
                START_TIME,
                END_TIME
        );

        MemberActivationResponse response = service.activate(new MemberActivationRequest(
                "device-001",
                "EDU-PAD-2026",
                "user-001"
        ));

        assertThat(response.status()).isEqualTo(ActivationStatus.ALREADY_ACTIVATED);
        assertThat(response.message()).isEqualTo("Device already activated");
    }

    @Test
    void activateReturnsAlreadyActivatedWhenRecordExists() {
        FakeMemberActivationMapper mapper = new FakeMemberActivationMapper(true, false);
        mapper.activationExists = true;
        MemberActivationServiceImpl service = new MemberActivationServiceImpl(
                mapper,
                fixedClock("2026-06-10T10:00:00Z"),
                START_TIME,
                END_TIME
        );

        MemberActivationResponse response = service.activate(new MemberActivationRequest(
                "device-existing",
                "EDU-PAD-2026",
                "user-existing"
        ));

        assertThat(response.status()).isEqualTo(ActivationStatus.ALREADY_ACTIVATED);
        assertThat(mapper.insertCalled).isFalse();
    }

    @Test
    void activatePropagatesUnexpectedDatabaseInsertFailure() {
        FakeMemberActivationMapper mapper = new FakeMemberActivationMapper(true, false);
        mapper.failUnexpectedly = true;
        MemberActivationServiceImpl service = new MemberActivationServiceImpl(
                mapper,
                fixedClock("2026-06-10T10:00:00Z"),
                START_TIME,
                END_TIME
        );

        assertThatThrownBy(() -> service.activate(new MemberActivationRequest(
                "device-db-fail",
                "EDU-PAD-2026",
                "user-db-fail"
        ))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void activateRejectsNonEducationModel() {
        FakeMemberActivationMapper mapper = new FakeMemberActivationMapper(false, false);
        MemberActivationServiceImpl service = new MemberActivationServiceImpl(
                mapper,
                fixedClock("2026-06-10T10:00:00Z"),
                START_TIME,
                END_TIME
        );

        MemberActivationResponse response = service.activate(new MemberActivationRequest(
                "device-002",
                "NORMAL-PAD-2026",
                "user-002"
        ));

        assertThat(response.status()).isEqualTo(ActivationStatus.MODEL_NOT_ELIGIBLE);
        assertThat(response.message()).isEqualTo("Only education models can activate this membership");
        assertThat(mapper.insertCalled).isFalse();
    }

    @Test
    void activateRejectsBeforeCampaignStarts() {
        FakeMemberActivationMapper mapper = new FakeMemberActivationMapper(true, false);
        MemberActivationServiceImpl service = new MemberActivationServiceImpl(
                mapper,
                fixedClock("2026-05-31T15:59:59Z"),
                START_TIME,
                END_TIME
        );

        MemberActivationResponse response = service.activate(new MemberActivationRequest(
                "device-003",
                "EDU-PAD-2026",
                "user-003"
        ));

        assertThat(response.status()).isEqualTo(ActivationStatus.CAMPAIGN_NOT_STARTED);
        assertThat(mapper.insertCalled).isFalse();
    }

    @Test
    void activateRejectsAfterCampaignEnds() {
        FakeMemberActivationMapper mapper = new FakeMemberActivationMapper(true, false);
        MemberActivationServiceImpl service = new MemberActivationServiceImpl(
                mapper,
                fixedClock("2026-06-30T16:00:00Z"),
                START_TIME,
                END_TIME
        );

        MemberActivationResponse response = service.activate(new MemberActivationRequest(
                "device-004",
                "EDU-PAD-2026",
                "user-004"
        ));

        assertThat(response.status()).isEqualTo(ActivationStatus.CAMPAIGN_ENDED);
        assertThat(mapper.insertCalled).isFalse();
    }

    private static Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Shanghai"));
    }

    private static class FakeMemberActivationMapper implements MemberActivationMapper {

        private final boolean educationModel;
        private final boolean duplicateDevice;
        private boolean insertCalled;
        private boolean failUnexpectedly;
        private boolean activationExists;

        private FakeMemberActivationMapper(boolean educationModel, boolean duplicateDevice) {
            this.educationModel = educationModel;
            this.duplicateDevice = duplicateDevice;
        }

        @Override
        public int existsEducationModel(String modelCode) {
            return educationModel ? 1 : 0;
        }

        @Override
        public int existsActivation(String deviceId) {
            return activationExists ? 1 : 0;
        }

        @Override
        public int insertActivation(String deviceId, String modelCode, String userId, LocalDateTime activatedAt) {
            insertCalled = true;
            if (failUnexpectedly) {
                throw new IllegalStateException("database unavailable");
            }
            if (duplicateDevice) {
                throw new DuplicateKeyException("device already activated");
            }
            return 1;
        }

        @Override
        public List<MemberActivationRecord> listActivationRecordsForExport(
                String modelCode,
                LocalDateTime startTime,
                LocalDateTime endTime,
                long lastId,
                int limit
        ) {
            return List.of();
        }
    }

}
