# Repository Guidelines

## Project Structure & Module Organization

This is a Java 17 Spring Boot 3.3.5 application packaged as a WAR. Main code lives under `src/main/java/com/example/oomcheck`.

- `config/`: Spring configuration and application properties binding.
- `controller/`: HTTP endpoints such as health/check and member activation APIs.
- `service/` and `service/impl/`: business interfaces and implementations.
- `dao/` and `dao/impl/`: persistence abstractions and in-memory implementations.
- `dto/`: request/response objects and enums.
- `entity/`: domain/data model classes.
- `src/main/resources/`: runtime configuration and database schema (`application.yml`, `db/schema.sql`).
- `src/test/java/`: JUnit tests mirroring production packages.
- `docs/superpowers/plans/`: implementation notes and planning documents.

Do not edit generated build output in `target/`.

## Build, Test, and Development Commands

- `mvn test`: runs the full test suite with Spring Boot Test, JUnit, and test-scoped H2.
- `mvn package`: compiles, tests, and produces `target/oomcheck.war`.
- `mvn spring-boot:run`: starts the app locally using `src/main/resources/application.yml`.

Use Java 17 for all Maven commands.

## Coding Style & Naming Conventions

Follow standard Java/Spring conventions: 4-space indentation, UTF-8 files, PascalCase classes, camelCase fields and methods, and uppercase enum constants. Keep controllers thin; place business rules in services. Name Spring components by role, for example `CheckController`, `MemberActivationService`, and `MemberActivationServiceImpl`.

Prefer constructor injection for dependencies. Keep DTOs simple and avoid leaking persistence entities into API contracts unless the existing endpoint already does so.

## Testing Guidelines

Tests use Spring Boot's test stack and JUnit 5 through `spring-boot-starter-test`. Place tests under the same package path as the class under test, and name them `*Test.java`, for example `CheckControllerTest` or `MemberActivationServiceImplTest`.

Run `mvn test` before submitting changes. Add or update controller tests for endpoint behavior and service tests for business rules, especially around activation status, locking, and persistence edge cases.

## Commit & Pull Request Guidelines

Git history is not available in this workspace, so no repository-specific commit convention can be inferred. Use concise imperative commit messages such as `Add member activation validation` or `Fix check endpoint response`.

Pull requests should include a short summary, test results (`mvn test`), linked issue or task when applicable, and API examples or screenshots if HTTP behavior changes. Call out configuration changes to MySQL or `application.yml`.

## Security & Configuration Tips

Do not commit secrets or environment-specific credentials in `application.yml`. Keep production database settings externalized through environment-specific configuration.

## 工作规则

- 默认先阅读相关代码和文档，再开始修改。
- 修改前先说明影响范围。
- 只做和当前任务相关的最小改动。
- 提交前必须运行最小验证。
- 说明用中文，代码、命令、文件名保持英文。
- 不要修改无关文件。
- OpenSpec 相关文档必须使用简体中文。
- proposal.md、design.md、tasks.md、spec.md 中的需求、设计、任务、异常场景、验收标准必须用中文描述。
- 技术术语可以保留英文，例如 API、Redis、MySQL、RocketMQ、Spring Boot。
- 代码、类名、方法名、字段名、文件路径保持英文。
- 不要把 Java 代码、SQL、配置文件里的关键字翻译成中文。
