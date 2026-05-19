package com.example.asgd.service;

public interface MemberBenefitGrantClient {

    GrantResult grant(GrantRequest request);

    record GrantRequest(
            String grantOrderNo,
            long userId,
            String benefitType,
            int memberDays
    ) {
    }

    record GrantResult(
            boolean success,
            boolean retryable,
            String code,
            String message
    ) {

        public static GrantResult granted() {
            return new GrantResult(true, false, "SUCCESS", "Benefit granted");
        }

        public static GrantResult retryableFailure(String message) {
            return new GrantResult(false, true, "UNKNOWN", message);
        }
    }
}
