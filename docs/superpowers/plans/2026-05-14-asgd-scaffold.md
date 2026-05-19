# Asgd Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot Maven project named `asgd` with controller, service, dao, DTO, and Tomcat deployable WAR packaging.

**Architecture:** The project exposes a small check API through a controller, delegates behavior to a service interface and implementation, and reads sample data from a DAO interface. DTOs define HTTP response shapes while an entity represents the internal check item.

**Tech Stack:** Java 17, Spring Boot 3.3.x, Maven, JUnit 5, Spring MockMvc, external Tomcat compatible WAR.

---

### Task 1: Project Configuration And Tests

**Files:**
- Create: `pom.xml`
- Create: `src/test/java/com/example/asgd/AsgdApplicationTests.java`
- Create: `src/test/java/com/example/asgd/controller/CheckControllerTest.java`

- [x] **Step 1: Write failing tests**

Create tests that expect the Spring context to load and `/api/check/health` plus `/api/check/items` to return usable JSON.

- [x] **Step 2: Run tests to verify they fail**

Run: `mvn test`
Expected before implementation: compilation fails because application and controller classes do not exist.

### Task 2: Minimal Spring Boot WAR Application

**Files:**
- Create: `src/main/java/com/example/asgd/AsgdApplication.java`
- Create: `src/main/java/com/example/asgd/config/WebApplicationInitializer.java`

- [x] **Step 1: Add the Spring Boot entrypoint**

Create an application class with `main` and `SpringBootServletInitializer.configure(...)` so it can run from Maven or an external Tomcat WAR.

### Task 3: Layered Check API

**Files:**
- Create: `src/main/java/com/example/asgd/controller/CheckController.java`
- Create: `src/main/java/com/example/asgd/service/CheckService.java`
- Create: `src/main/java/com/example/asgd/service/impl/CheckServiceImpl.java`
- Create: `src/main/java/com/example/asgd/dao/CheckDao.java`
- Create: `src/main/java/com/example/asgd/dao/impl/InMemoryCheckDao.java`
- Create: `src/main/java/com/example/asgd/dto/HealthResponse.java`
- Create: `src/main/java/com/example/asgd/dto/CheckItemResponse.java`
- Create: `src/main/java/com/example/asgd/entity/CheckItem.java`
- Create: `src/main/resources/application.yml`

- [x] **Step 1: Add DTOs, entity, DAO, service, and controller**

Implement two endpoints:
- `GET /api/check/health`
- `GET /api/check/items`

- [x] **Step 2: Run tests to verify they pass**

Run: `mvn test`
Expected after implementation: all tests pass.
