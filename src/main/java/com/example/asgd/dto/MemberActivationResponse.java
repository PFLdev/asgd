package com.example.asgd.dto;

public record MemberActivationResponse(
        ActivationStatus status,
        String message
) {

    public static MemberActivationResponse success() {
        return new MemberActivationResponse(ActivationStatus.SUCCESS, "Activation successful");
    }

    public static MemberActivationResponse alreadyActivated() {
        return new MemberActivationResponse(ActivationStatus.ALREADY_ACTIVATED, "Device already activated");
    }

    public static MemberActivationResponse modelNotEligible() {
        return new MemberActivationResponse(
                ActivationStatus.MODEL_NOT_ELIGIBLE,
                "Only education models can activate this membership"
        );
    }

    public static MemberActivationResponse campaignNotStarted() {
        return new MemberActivationResponse(ActivationStatus.CAMPAIGN_NOT_STARTED, "Campaign has not started");
    }

    public static MemberActivationResponse campaignEnded() {
        return new MemberActivationResponse(ActivationStatus.CAMPAIGN_ENDED, "Campaign has ended");
    }
}
