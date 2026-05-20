# Coupon Claim Design

## Context

The project already contains an education member benefit claim flow, but this feature will add a separate generic coupon claim module. The first version focuses on one core business rule: a user can claim a given coupon only once, and coupon stock must not be oversold under concurrent requests.

## Goals

- Add a generic coupon claim API independent of the education benefit module.
- Enforce one successful claim per `(couponId, userId)`.
- Decrement coupon stock atomically and never allow stock to go below zero.
- Return stable, business-friendly statuses for success, duplicate claim, sold out, unavailable coupon, and invalid request.
- Keep the module small enough to extend later with coupon types, user eligibility, expiration policies, and claim history queries.

## Non-Goals

- No authentication integration in the first version; `userId` is supplied by the request body.
- No external coupon issuing platform integration.
- No coupon usage, redemption, refund, or expiration job behavior.
- No migration framework changes beyond updating the existing schema file used by this project.

## API

### Claim Coupon

`POST /api/coupon/claim`

Request:

```json
{
  "couponId": 10001,
  "userId": 123456
}
```

Response:

```json
{
  "status": "SUCCESS",
  "couponId": 10001,
  "userId": 123456,
  "message": "Coupon claimed"
}
```

Status values:

- `SUCCESS`: claim succeeded in this request.
- `ALREADY_CLAIMED`: the same user has already claimed the coupon.
- `SOLD_OUT`: the coupon exists and is claimable, but stock is exhausted.
- `UNAVAILABLE`: the coupon does not exist, is disabled, or is outside the claim window.
- `INVALID_REQUEST`: required request fields are missing or invalid.

## Data Model

Add a `coupon` table:

- `id`: primary key.
- `coupon_name`: display name for test and future API output.
- `total_stock`: initial stock.
- `available_stock`: remaining claimable stock.
- `status`: enabled or disabled.
- `start_time`, `end_time`: claim window.
- `create_time`, `update_time`: audit timestamps.

Add a `coupon_receive_record` table:

- `id`: primary key.
- `coupon_id`: claimed coupon id.
- `user_id`: claiming user id.
- `claim_status`: first version stores successful claims.
- `claim_time`: time of claim.
- `create_time`, `update_time`: audit timestamps.

Constraints and indexes:

- `coupon.available_stock` is decremented only with an atomic conditional update.
- Unique index `uk_coupon_receive_coupon_user(coupon_id, user_id)` prevents duplicate successful claims for the same user and coupon.
- Index `idx_coupon_receive_user_id(user_id)` supports future claim history queries.

## Components

- `CouponClaimController`: thin REST endpoint for `POST /api/coupon/claim`.
- `CouponClaimService`: business interface.
- `CouponClaimServiceImpl`: validates requests, checks coupon availability, handles duplicate claims, performs transactional stock deduction and record creation.
- `CouponClaimMapper`: persistence abstraction for coupon lookup, atomic stock deduction, receive record insert, and receive record lookup.
- DTOs:
  - `CouponClaimRequest`
  - `CouponClaimResponse`
  - `CouponClaimStatus`
- Entities:
  - `Coupon`
  - `CouponReceiveRecord`

## Claim Flow

1. Validate that `couponId` and `userId` are present and positive.
2. Load the coupon and verify it is enabled and within its claim window.
3. Check whether `(couponId, userId)` already has a receive record.
4. In a transaction, execute an atomic stock decrement:

   ```sql
   UPDATE coupon
   SET available_stock = available_stock - 1
   WHERE id = ? AND available_stock > 0
   ```

5. If the update affects zero rows, return `SOLD_OUT`.
6. Insert a receive record for `(couponId, userId)`.
7. If the insert hits the unique key because of a concurrent duplicate request, roll back the transaction and return `ALREADY_CLAIMED` after reloading the existing record.
8. Return `SUCCESS`.

## Concurrency Guarantees

Stock safety comes from the database atomic conditional update, not from a separate read-then-write stock check. If many requests race for the last units of stock, only requests whose update affects one row can proceed to create a receive record.

Duplicate claim safety comes from the unique `(coupon_id, user_id)` constraint. If the same user submits concurrent requests, at most one transaction can insert the receive record. Any transaction that deducted stock but then loses the unique-key race must roll back, so its stock deduction is undone.

## Error Handling

- Missing or invalid `couponId` / `userId` returns `INVALID_REQUEST`.
- Coupon not found, disabled, not started, or expired returns `UNAVAILABLE`.
- Existing receive record returns `ALREADY_CLAIMED`.
- No stock update row returns `SOLD_OUT`.
- Duplicate-key race during insert is treated as `ALREADY_CLAIMED`, not as a server error.

The controller should keep HTTP status `200 OK` for business outcomes, matching the existing project style. Validation failures may also return a response DTO instead of throwing framework validation errors, to keep the first version consistent and simple.

## Tests

Add service tests for:

- Successful first claim decrements stock and creates a receive record.
- Repeated claim by the same user returns `ALREADY_CLAIMED` without decrementing stock again.
- Claim returns `SOLD_OUT` when stock is zero.
- Disabled, missing, not-started, and expired coupons return `UNAVAILABLE`.
- Concurrent claims cannot exceed stock.
- Concurrent duplicate claims by the same user create one receive record and consume one stock unit.

Add controller tests for:

- `POST /api/coupon/claim` success response.
- Duplicate claim response.
- Invalid request response.
- Sold out response.

## Open Questions Resolved

- The first version uses a separate generic coupon module, not the existing education benefit claim module.
- A user can claim the same coupon only once.
- Coupon stock is controlled and must be concurrency-safe.
- The endpoint shape is `POST /api/coupon/claim` with `couponId` and `userId` in the request body.
