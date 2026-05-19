package com.example.asgd.service.impl;

import com.example.asgd.dao.EduBenefitClaimMapper;
import com.example.asgd.dto.EduBenefitClaimRequest;
import com.example.asgd.entity.EduBenefitActivity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class EduBenefitClaimServiceTransactionTest {

    @Autowired
    private EduBenefitClaimServiceImpl claimService;

    @Autowired
    private EduBenefitClaimMapper claimMapper;

    @Test
    void rollsBackReceiveRecordWhenGrantTaskInsertFails() {
        String deviceIdHash = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        EduBenefitClaimRequest request = new EduBenefitClaimRequest(
                10001L,
                "tx-rollback-device",
                123456L,
                "tx-rollback-request"
        );
        EduBenefitActivity activityWithInvalidGrantTask = new EduBenefitActivity(
                10001L,
                "EDU_MEMBER_2026",
                "Education Member 2026",
                null,
                "365",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                1
        );

        assertThatThrownBy(() -> claimService.createReceiveRecordAndGrantTask(
                request,
                deviceIdHash,
                activityWithInvalidGrantTask,
                LocalDateTime.of(2026, 6, 10, 18, 0)
        )).isInstanceOf(RuntimeException.class);

        assertThat(claimMapper.findReceiveRecord(10001L, deviceIdHash)).isEmpty();
    }
}
