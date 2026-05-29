## Why

The existing AI chat endpoint accepts only a single user message, which limits callers that need to control model behavior with a system prompt or separate reusable user prompt. Supporting explicit prompt roles makes the OpenAI and DeepSeek integration more useful for guided assistants, domain-specific responses, and prompt experiments.

## What Changes

- Extend the AI chat request contract to accept optional `systemPrompt` and `userPrompt` fields.
- Keep the existing `message` field for backward compatibility.
- Use `userPrompt` as the effective user content when present; otherwise continue using `message`.
- Send `systemPrompt` to Spring AI as a system message when it is present.
- Preserve the existing provider routing, response shape, API key handling, and unsupported-provider behavior.

## Capabilities

### New Capabilities
- `ai-chat-prompts`: Defines prompt role support for the AI chat endpoint.

### Modified Capabilities

## Impact

- Affected API: `POST /api/ai/chat`
- Affected code: AI chat DTOs, controller/service interfaces, Spring AI provider client, and tests.
- Dependencies: No new external dependency beyond the existing Spring AI integration.
- Compatibility: Existing requests that only send `provider` and `message` remain valid.
