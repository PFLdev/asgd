package com.example.asgd.service.impl;

import com.example.asgd.config.ActivationCampaignProperties;
import com.example.asgd.dao.MemberActivationMapper;
import com.example.asgd.dto.MemberActivationRequest;
import com.example.asgd.dto.MemberActivationResponse;
import com.example.asgd.service.MemberActivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class MemberActivationServiceImpl implements MemberActivationService {

    @Autowired
    private MemberActivationMapper memberActivationMapper;

    @Autowired
    private Clock clock;

    @Autowired
    private ActivationCampaignProperties properties;

    private LocalDateTime campaignStart;
    private LocalDateTime campaignEnd;

    public MemberActivationServiceImpl() {
    }

    public MemberActivationServiceImpl(
            MemberActivationMapper memberActivationMapper,
            Clock clock,
            LocalDateTime campaignStart,
            LocalDateTime campaignEnd
    ) {
        this.memberActivationMapper = memberActivationMapper;
        this.clock = clock;
        this.campaignStart = campaignStart;
        this.campaignEnd = campaignEnd;
    }

    @Override
    @Transactional
    public MemberActivationResponse activate(MemberActivationRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(getCampaignStart())) {
            return MemberActivationResponse.campaignNotStarted();
        }
        if (now.isAfter(getCampaignEnd())) {
            return MemberActivationResponse.campaignEnded();
        }
        if (memberActivationMapper.existsEducationModel(request.modelCode()) == 0) {
            return MemberActivationResponse.modelNotEligible();
        }

        try {
            if (memberActivationMapper.existsActivation(request.deviceId()) > 0) {
                return MemberActivationResponse.alreadyActivated();
            }
            memberActivationMapper.insertActivation(
                    request.deviceId(),
                    request.modelCode(),
                    request.userId(),
                    now
            );
            return MemberActivationResponse.success();
        } catch (DuplicateKeyException ex) {
            return MemberActivationResponse.alreadyActivated();
        }
    }

    private LocalDateTime getCampaignStart() {
        return campaignStart != null ? campaignStart : properties.start();
    }

    private LocalDateTime getCampaignEnd() {
        return campaignEnd != null ? campaignEnd : properties.end();
    }
}
