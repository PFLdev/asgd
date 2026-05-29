## Why

Coupon/member benefit granting currently relies on generated grant order numbers and local status updates, but there is no explicit contract for reusing a caller-provided idempotency key across repeated grant requests. Retries, duplicate client submissions, or ambiguous downstream grant results can therefore risk duplicate grant attempts or inconsistent responses.

## What Changes

- Add idempotent coupon grant behavior keyed by a stable grant id supplied with the claim request.
- Persist the grant id with the receive record and grant task so repeated requests can resolve to the same grant order and status.
- Return the existing grant outcome for duplicate requests instead of creating a second receive record, grant task, or downstream grant call.
- Reject conflicting reuse of the same grant id when it points at a different activity, user, or device.
- Preserve existing eligibility, activity-window, locking, and grant-status semantics.

## Capabilities

### New Capabilities
- `coupon-grant-idempotency`: Defines idempotent coupon/member benefit grant behavior by grant id, including duplicate replay, conflict handling, and persistence expectations.

### Modified Capabilities

## Impact

- Affected API contract: `EduBenefitClaimRequest` / `/api/edu/benefit/claim` claim behavior.
- Affected persistence: `edu_benefit_receive_record`, `edu_benefit_grant_task`, schema indexes/constraints, and mapper queries.
- Affected business logic: `EduBenefitClaimServiceImpl` duplicate detection, grant task creation, and downstream `MemberBenefitGrantClient` invocation.
- Affected tests: service tests for duplicate replay and conflict cases, controller tests for request validation/response behavior, and schema-backed transaction tests.
