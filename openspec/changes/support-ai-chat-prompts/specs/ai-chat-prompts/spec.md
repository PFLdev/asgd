## ADDED Requirements

### Requirement: AI chat accepts prompt roles
系统 SHALL 支持 `POST /api/ai/chat` 请求传入可选的 `systemPrompt` 和 `userPrompt` 字段，同时继续兼容现有 `message` 字段。

#### Scenario: 使用旧 message 字段
- **WHEN** 请求只提供 `provider` 和非空 `message`
- **THEN** 系统 SHALL 继续将 `message` 作为用户提示词调用指定模型

#### Scenario: 使用 userPrompt 字段
- **WHEN** 请求提供非空 `userPrompt`
- **THEN** 系统 SHALL 将 `userPrompt` 作为有效用户提示词调用指定模型

#### Scenario: userPrompt 优先于 message
- **WHEN** 请求同时提供非空 `userPrompt` 和 `message`
- **THEN** 系统 SHALL 使用 `userPrompt` 作为有效用户提示词，并忽略 `message` 的用户内容

#### Scenario: 使用 systemPrompt 字段
- **WHEN** 请求提供非空 `systemPrompt` 和有效用户提示词
- **THEN** 系统 SHALL 将 `systemPrompt` 作为 system message，将有效用户提示词作为 user message 调用指定模型

### Requirement: AI chat prompt validation
系统 MUST 校验 AI chat 请求至少包含一个非空用户提示词来源：`userPrompt` 或 `message`。

#### Scenario: 缺少用户提示词
- **WHEN** 请求未提供 `userPrompt`，且 `message` 缺失或为空白
- **THEN** 系统 MUST 返回 HTTP 400，且不调用任何模型 provider

#### Scenario: 空白 systemPrompt
- **WHEN** 请求提供空白 `systemPrompt` 和有效用户提示词
- **THEN** 系统 SHALL 忽略空白 `systemPrompt`，并按无 system message 的方式调用模型

### Requirement: AI chat existing behavior is preserved
系统 SHALL 保留现有 provider 路由、响应结构、缺少 API key 和不支持 provider 的错误行为。

#### Scenario: 不支持 provider
- **WHEN** 请求使用不支持的 provider
- **THEN** 系统 SHALL 返回现有的 HTTP 400 错误响应

#### Scenario: provider API key 缺失
- **WHEN** provider API key 未配置
- **THEN** 系统 SHALL 返回现有的 HTTP 503 错误响应
