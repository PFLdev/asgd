## Context

The education benefit claim flow validates activity and device eligibility, creates an `edu_benefit_receive_record` plus `edu_benefit_grant_task` inside a transaction, then calls `MemberBenefitGrantClient` outside the distributed lock. The request already carries `requestId`, but that value is only embedded in grant-task JSON, so it cannot be queried or constrained as an idempotency key.

The current database protects one claim per `(activity_id, device_id_hash)` and one task per `grant_order_no`. That prevents many duplicate device claims, but it does not make duplicate submissions by the same caller id a first-class contract, and it does not detect conflicting reuse of an id across different claim parameters.

## Goals / Non-Goals

**Goals:**
- Make caller-provided coupon grant ids idempotent for claim creation and downstream grant attempts.
- Persist the idempotency key in queryable columns on both receive records and grant tasks.
- Replay the existing response for exact duplicate submissions without creating extra rows or calling the downstream grant client again when the existing grant has already reached a terminal or in-progress state.
- Detect and reject conflicting grant-id reuse across different activity, user, or device values.
- Preserve existing activity, device whitelist, cache, lock, and grant status behavior.

**Non-Goals:**
- Replace the existing `grant_order_no` format or downstream grant contract.
- Add asynchronous retry workers or change retry scheduling semantics.
- Introduce a new external idempotency service.
- Backfill historical rows beyond a lightweight nullable-column migration path.

## Decisions

1. Treat `EduBenefitClaimRequest.requestId` as the coupon grant id.
   - Rationale: the field already exists at the API boundary and is currently included in grant-task metadata, so using it avoids a breaking request DTO rename.
   - Alternative considered: add a new `grantId` field. That would make the contract clearer but require clients to send another value and would leave ambiguous behavior for the existing `requestId`.

2. Add a dedicated `grant_id` column with uniqueness to receive records and grant tasks.
   - Rationale: queryable persistence makes duplicate detection, conflict checks, and database-level protection explicit.
   - Alternative considered: parse `request_body` JSON for `requestId`. That keeps the schema unchanged but is brittle, slower, and cannot be protected with a normal unique constraint.

3. Lookup by grant id before creating a new receive record.
   - Rationale: the fastest duplicate path should return the existing record and avoid the lock, transaction, and downstream call when possible.
   - Alternative considered: only rely on the existing activity/device lookup. That misses grant-id conflicts and does not document idempotency as an API capability.

4. Use the existing distributed lock key for activity/device creation and add database constraints as the final concurrency guard.
   - Rationale: current locking already serializes the primary claim path, while unique keys handle races across processes or lock failures.
   - Alternative considered: lock by grant id only. That would protect duplicate request ids but weaken the existing one-benefit-per-device path.

5. Reuse `responseFromRecord` for duplicate replay.
   - Rationale: repeated requests should see the current persisted grant state: success returns success, failed returns failed, and non-terminal statuses return processing.
   - Alternative considered: return a special duplicate status. That would expand the API surface without improving grant safety.

## Risks / Trade-offs

- [Risk] Existing records lack `grant_id` after migration -> Mitigation: add nullable columns first or populate new rows only, and make service logic tolerate missing grant ids on legacy records.
- [Risk] Reusing `requestId` as grant id may surprise clients that treated it as diagnostic only -> Mitigation: validate it as required for claim calls and document that identical values replay the original claim.
- [Risk] Duplicate requests while the first downstream grant call is still running may still observe `PROCESSING` -> Mitigation: status updates already mark processing before calling the downstream client, so duplicate callers receive a stable in-progress response instead of triggering another grant.
- [Risk] Database uniqueness can surface as `DuplicateKeyException` during races -> Mitigation: catch duplicate insert races, reload by grant id or activity/device, and apply the same replay/conflict rules.

## Migration Plan

1. Add `grant_id` columns to `edu_benefit_receive_record` and `edu_benefit_grant_task`.
2. Add indexes/unique constraints for grant-id lookup. If existing deployments contain rows, stage the uniqueness change after backfill or keep the constraint scoped to non-null values where supported.
3. Update mappers and entities to read/write grant id.
4. Update service flow to validate request id, find existing grant id records, enforce conflict checks, and pass through duplicate replay.
5. Update tests for first claim, exact duplicate replay, conflicting grant-id reuse, and duplicate insert races.

Rollback is to stop using the new column in service code while leaving the additive schema columns in place. No destructive data rollback is required.

## Open Questions

- Should blank or missing `requestId` be rejected at the controller validation layer, or should the service return an existing response style for invalid requests?
- Should the unique grant-id constraint be global, or scoped by activity for operational recovery if two activities accidentally receive the same external id?
