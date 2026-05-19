package com.example.asgd.controller;

import com.example.asgd.dto.EduBenefitClaimRequest;
import com.example.asgd.dto.EduBenefitClaimResponse;
import com.example.asgd.service.EduBenefitClaimService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/edu/member/claim")
public class EduBenefitClaimController {

    private final EduBenefitClaimService claimService;

    public EduBenefitClaimController(EduBenefitClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping("/status")
    public EduBenefitClaimResponse getStatus(
            @RequestParam long activityId,
            @RequestParam String deviceId
    ) {
        return claimService.getStatus(activityId, deviceId);
    }

    @PostMapping
    public EduBenefitClaimResponse claim(@RequestBody EduBenefitClaimRequest request) {
        return claimService.claim(request);
    }
}
