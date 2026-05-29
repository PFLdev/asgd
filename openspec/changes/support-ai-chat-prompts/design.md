## Context

当前 `POST /api/ai/chat` 支持 `provider`、兼容字段 `message`，以及新的 `systemPrompt`、`userPrompt`。Controller 将请求传给 `AiChatService`，Service 负责 provider 路由、prompt 归一化和 `userPrompt > message` 的优先级。

模型调用层原先直接使用 `ChatModel.call(Prompt)`。为了贴近 Spring AI 推荐的客户端调用方式，本变更改为在配置层创建 provider 对应的 `ChatClient`，再由 `SpringAiProviderClient` 使用 `ChatClient` 的 fluent API 发起调用。

## Goals / Non-Goals

**Goals:**

- 保持 `/api/ai/chat` 请求和响应契约不变。
- 保持 `message` 兼容逻辑、`userPrompt` 优先级、空白 `systemPrompt` 忽略逻辑不变。
- 使用 `ChatClient` 表达 system/user prompt，而不是直接调用 `ChatModel.call(Prompt)`。
- 在配置层集中构造 OpenAI/DeepSeek 的模型和 `ChatClient`。
- 保留未配置 API key 时由请求返回 503 的行为，避免应用启动阶段失败。

**Non-Goals:**

- 不引入多轮消息数组、tool calling、流式响应或 prompt 模板持久化。
- 不改变 OpenAI/DeepSeek 的模型配置项、base URL、completions path 或响应格式。

## Decisions

1. `AiChatConfig` 为 OpenAI 和 DeepSeek 分别定义懒加载 `ChatClient` bean。
   原因：provider 模型和客户端构造属于配置职责；懒加载可以避免缺少 API key 时启动失败。

2. `SpringAiProviderClient` 依赖 `Supplier<ChatClient>`，并在请求通过 API key 校验后再获取客户端。
   原因：保持旧的缺 key 行为，继续由 `SpringAiProviderClient` 抛出 `NonTransientAiException`，Controller 映射为 HTTP 503。

3. `SpringAiProviderClient` 使用 `chatClient.prompt().system(...).user(...).call().content()`。
   原因：这是 Spring AI `ChatClient` 的角色化 prompt 调用方式，外部不需要知道底层 `Prompt`/`Message` 构造细节。

4. 空白 `systemPrompt` 不调用 `system(...)`，有效 user prompt 始终调用 `user(...)`。
   原因：和能力规格保持一致，避免把空白 system prompt 传入模型。

## Risks / Trade-offs

- [Risk] 新增 `spring-ai-client-chat` 依赖后 classpath 变化 -> Mitigation：使用 Spring AI BOM 管理版本，和现有 Spring AI 1.0.0 对齐。
- [Risk] 直接创建 ChatClient bean 可能让缺 key 在启动期失败 -> Mitigation：ChatClient bean 使用 `@Lazy`，provider client 也延迟获取。
- [Risk] 测试只 mock fluent API，可能过度依赖调用细节 -> Mitigation：保留 Controller/Service 行为测试，ProviderClient 测试只覆盖 ChatClient 边界。

## Migration Plan

1. 添加 `spring-ai-client-chat` 依赖。
2. 在 `AiChatConfig` 中创建 OpenAI/DeepSeek 的懒加载 `ChatClient` bean。
3. 将 `SpringAiProviderClient` 从 `ChatModel` 调用迁移为 `ChatClient` 调用。
4. 更新 ProviderClient 测试，验证 system/user prompt 通过 `ChatClient` fluent API 发送。
5. 运行完整测试，确认旧行为和新调用方式都通过。
