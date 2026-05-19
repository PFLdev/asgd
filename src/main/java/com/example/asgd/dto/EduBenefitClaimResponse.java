package com.example.asgd.dto;

public record EduBenefitClaimResponse(
        EduBenefitClaimStatus status,
        EduBenefitGrantStatus grantStatus,
        String grantOrderNo,
        boolean eligible,
        String message
) {

    public static EduBenefitClaimResponse notClaimed() {
        return new EduBenefitClaimResponse(
                EduBenefitClaimStatus.NOT_CLAIMED,
                EduBenefitGrantStatus.INIT,
                null,
                true,
                "Device benefit can be claimed"
        );
    }

    public static EduBenefitClaimResponse notEligible(String message) {
        return new EduBenefitClaimResponse(
                EduBenefitClaimStatus.NOT_ELIGIBLE,
                EduBenefitGrantStatus.INIT,
                null,
                false,
                message
        );
    }

    public static EduBenefitClaimResponse closed(String message) {
        return new EduBenefitClaimResponse(
                EduBenefitClaimStatus.CLOSED,
                EduBenefitGrantStatus.INIT,
                null,
                false,
                message
        );
    }

    public static EduBenefitClaimResponse processing(String grantOrderNo, String message) {
        return new EduBenefitClaimResponse(
                EduBenefitClaimStatus.PROCESSING,
                EduBenefitGrantStatus.PROCESSING,
                grantOrderNo,
                true,
                message
        );
    }

    public static EduBenefitClaimResponse success(String grantOrderNo, String message) {
        return new EduBenefitClaimResponse(
                EduBenefitClaimStatus.SUCCESS,
                EduBenefitGrantStatus.SUCCESS,
                grantOrderNo,
                true,
                message
        );
    }

    public static EduBenefitClaimResponse retrying(String grantOrderNo, String message) {
        return new EduBenefitClaimResponse(
                EduBenefitClaimStatus.PROCESSING,
                EduBenefitGrantStatus.RETRYING,
                grantOrderNo,
                true,
                message
        );
    }
}
