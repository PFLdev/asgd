package com.example.asgd.service;

import com.example.asgd.dto.EduBenefitClaimRequest;
import com.example.asgd.dto.EduBenefitClaimResponse;

public interface EduBenefitClaimService {

    EduBenefitClaimResponse getStatus(long activityId, String deviceId);

    EduBenefitClaimResponse claim(EduBenefitClaimRequest request);
}
