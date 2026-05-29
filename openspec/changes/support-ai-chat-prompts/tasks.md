## 1. API 契约

- [x] 1.1 扩展 `AiChatRequest`，新增可选 `systemPrompt` 和 `userPrompt` 字段
- [x] 1.2 调整请求校验，确保 `message` 或 `userPrompt` 至少一个非空，并保持 `provider` 必填
- [x] 1.3 更新 `AiChatController`，将完整请求传递给 Service，保留响应结构和异常处理行为

## 2. 服务与 Provider 调用

- [x] 2.1 扩展 `AiChatService`，支持接收 `systemPrompt`、`userPrompt` 和兼容 `message`
- [x] 2.2 在 `AiChatServiceImpl` 中实现 prompt 归一化和 `userPrompt` 优先级
- [x] 2.3 扩展 `AiProviderClient`，支持 system/user prompt 调用
- [x] 2.4 更新 `SpringAiProviderClient`，使用 Spring AI role message 发送 system message 和 user message
- [x] 2.5 保留现有 OpenAI/DeepSeek provider 路由、API key 缺失和不支持 provider 的行为

## 3. 测试验证

- [x] 3.1 添加 Controller 测试：旧 `message` 请求继续兼容
- [x] 3.2 添加 Controller 测试：`userPrompt` 优先于 `message`
- [x] 3.3 添加 Controller 测试：缺少有效用户提示词时返回 HTTP 400
- [x] 3.4 添加 Service 测试：provider 路由时传递 system/user prompt
- [x] 3.5 添加 ProviderClient 测试：system prompt 作为 system message，user prompt 作为 user message
- [x] 3.6 运行 `mvn test` 验证现有功能和新增测试通过

## 4. ChatClient 调用方式

- [x] 4.1 添加 `spring-ai-client-chat` 依赖
- [x] 4.2 在 `AiChatConfig` 中为 OpenAI/DeepSeek 创建懒加载 `ChatClient` bean
- [x] 4.3 将 `SpringAiProviderClient` 改为通过 `ChatClient` fluent API 调用模型
- [x] 4.4 更新 ProviderClient 测试，验证 `system(...)`、`user(...)`、`call().content()` 调用链

## 5. 配置

- [x] 5.1 在 `application.yml` 中补全 OpenAI/DeepSeek 的 API key、base URL、completions path 和 model 配置项
- [x] 5.2 API key 使用环境变量占位，避免提交密钥
