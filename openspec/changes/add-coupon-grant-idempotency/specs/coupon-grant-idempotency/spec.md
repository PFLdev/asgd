## ADDED Requirements

### Requirement: Claim requests use a grant id as the idempotency key
The system SHALL require each coupon/member benefit claim request to include a non-blank caller-provided grant id.

#### Scenario: Missing grant id is rejected
- **WHEN** a claim request omits the grant id or sends it as blank
- **THEN** the system MUST reject the request without creating a receive record, grant task, or downstream grant call

#### Scenario: First request persists grant id
- **WHEN** an eligible device submits a valid claim request with a new grant id
- **THEN** the system MUST persist that grant id with both the receive record and the grant task

### Requirement: Duplicate grant id replays the existing claim
The system SHALL return the existing claim result when a request repeats a grant id with the same activity, user, and device values.

#### Scenario: Duplicate successful grant
- **WHEN** a claim request repeats the same grant id after the original claim succeeded
- **THEN** the system MUST return the existing success response and grant order number without creating another grant task or calling the downstream grant client

#### Scenario: Duplicate in-progress grant
- **WHEN** a claim request repeats the same grant id while the original claim is NEW, PROCESSING, RETRYING, or UNKNOWN
- **THEN** the system MUST return a processing response with the existing grant order number without creating another grant task or calling the downstream grant client

#### Scenario: Duplicate failed grant
- **WHEN** a claim request repeats the same grant id after the original claim failed
- **THEN** the system MUST return the existing failed response with the existing grant order number without creating another grant task or calling the downstream grant client

### Requirement: Conflicting grant id reuse is rejected
The system SHALL reject a request that reuses an existing grant id for a different claim identity.

#### Scenario: Grant id reused for different device
- **WHEN** a claim request uses a grant id already associated with another device for the same or another activity
- **THEN** the system MUST reject the request without creating another receive record, grant task, or downstream grant call

#### Scenario: Grant id reused for different user
- **WHEN** a claim request uses a grant id already associated with another user for the same device and activity
- **THEN** the system MUST reject the request without creating another receive record, grant task, or downstream grant call

#### Scenario: Grant id reused for different activity
- **WHEN** a claim request uses a grant id already associated with another activity
- **THEN** the system MUST reject the request without creating another receive record, grant task, or downstream grant call

### Requirement: Concurrent duplicate requests remain single-grant
The system SHALL preserve idempotency when duplicate claim requests with the same grant id arrive concurrently.

#### Scenario: Concurrent exact duplicates
- **WHEN** two eligible claim requests with the same grant id, activity, user, and device are processed at the same time
- **THEN** the system MUST persist at most one receive record and one grant task for that grant id

#### Scenario: Duplicate insert race
- **WHEN** a database unique constraint detects a concurrent insert for the same grant id
- **THEN** the system MUST reload the existing claim and apply duplicate replay or conflict rejection rules
