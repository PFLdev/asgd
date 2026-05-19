# Member Activation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a tablet membership activation API where each eligible education-model device can activate exactly once during a one-month campaign.

**Architecture:** The controller accepts activation requests and delegates all decisions to a service. The service checks campaign time, verifies education-model eligibility through a DAO, and inserts an activation record through MyBatis; a unique database index on `device_id` is the final concurrency guard for five million devices.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Maven, MyBatis Spring Boot Starter, MySQL, H2 for automated tests.

---

### Task 1: Activation Tests

**Files:**
- Create: `src/test/java/com/example/asgd/service/impl/MemberActivationServiceImplTest.java`
- Create: `src/test/java/com/example/asgd/controller/MemberActivationControllerTest.java`

- [x] **Step 1: Write failing tests**

Tests cover success, duplicate activation, non-education model rejection, campaign not started, campaign ended, and HTTP request mapping.

- [x] **Step 2: Run tests to verify they fail**

Run: `mvn test -Dtest=MemberActivationServiceImplTest,MemberActivationControllerTest`
Expected before implementation: compilation fails because activation classes do not exist.

### Task 2: Persistence And Schema

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/db/schema.sql`
- Create: `src/test/resources/application.yml`

- [x] **Step 1: Add MyBatis, MySQL, and H2 test dependencies**

The production app uses MySQL configuration through environment variables. Tests use H2 with the same table shape and unique constraint.

### Task 3: Activation Domain And API

**Files:**
- Create: `src/main/java/com/example/asgd/controller/MemberActivationController.java`
- Create: `src/main/java/com/example/asgd/service/MemberActivationService.java`
- Create: `src/main/java/com/example/asgd/service/impl/MemberActivationServiceImpl.java`
- Create: `src/main/java/com/example/asgd/dao/MemberActivationMapper.java`
- Create: `src/main/java/com/example/asgd/dto/MemberActivationRequest.java`
- Create: `src/main/java/com/example/asgd/dto/MemberActivationResponse.java`
- Create: `src/main/java/com/example/asgd/dto/ActivationStatus.java`
- Create: `src/main/java/com/example/asgd/entity/MemberActivationRecord.java`

- [x] **Step 1: Implement activation flow**

The service returns stable status codes and relies on an insert plus unique key to handle high-concurrency duplicate device activation.

- [x] **Step 2: Run full verification**

Run: `mvn test` and `mvn package`.
Expected: all tests pass and `target/asgd.war` is generated.
