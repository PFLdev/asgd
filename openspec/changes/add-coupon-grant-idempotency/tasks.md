## 1. Persistence Model

- [ ] 1.1 Add `grant_id` columns and lookup/unique indexes to `src/main/resources/db/schema.sql` for receive records and grant tasks
- [ ] 1.2 Update `EduBenefitReceiveRecord` and `EduBenefitGrantTask` to carry `grantId`
- [ ] 1.3 Update `EduBenefitClaimMapper` constructor mappings, inserts, and lookup queries for `grant_id`
- [ ] 1.4 Add a mapper method to find an existing receive record by `grant_id`

## 2. Claim Service Behavior

- [ ] 2.1 Validate that `EduBenefitClaimRequest.requestId` is non-blank before creating claim data
- [ ] 2.2 Check for an existing record by grant id before entering the create path
- [ ] 2.3 Replay existing responses for exact duplicate activity, user, and device claims without calling `MemberBenefitGrantClient`
- [ ] 2.4 Reject conflicting grant-id reuse for different activity, user, or device values
- [ ] 2.5 Persist grant id when creating the receive record and grant task
- [ ] 2.6 Handle duplicate-key races by reloading the existing record and applying replay/conflict rules

## 3. Tests

- [ ] 3.1 Add service tests for first claim persistence of grant id
- [ ] 3.2 Add service tests for duplicate successful, processing, retrying/unknown, and failed grant replay
- [ ] 3.3 Add service tests for conflicting grant-id reuse across activity, user, and device
- [ ] 3.4 Add a duplicate insert race test that verifies reload and replay/conflict behavior
- [ ] 3.5 Update controller or integration tests for missing/blank request id rejection

## 4. Verification

- [ ] 4.1 Run `mvn test`
- [ ] 4.2 Run `openspec status --change "add-coupon-grant-idempotency"` and confirm the change remains apply-ready
