package com.example.asgd.service.impl;

import com.example.asgd.dto.AiChatResponse;
import com.example.asgd.service.AiProviderClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiChatServiceImplTest {

    @Test
    void chatRoutesOpenAiProviderToOpenAiModel() {
        AiProviderClient openAiClient = (systemPrompt, userPrompt) -> "openai: " + userPrompt;
        AiProviderClient deepSeekClient = (systemPrompt, userPrompt) -> "deepseek: " + userPrompt;
        AiChatServiceImpl service = new AiChatServiceImpl(openAiClient, deepSeekClient);

        AiChatResponse response = service.chat("openai", null, null, "Hello");

        assertThat(response).isEqualTo(new AiChatResponse("openai", "openai: Hello"));
    }

    @Test
    void chatRoutesDeepSeekProviderToDeepSeekModel() {
        AiProviderClient openAiClient = (systemPrompt, userPrompt) -> "openai: " + userPrompt;
        AiProviderClient deepSeekClient = (systemPrompt, userPrompt) -> "deepseek: " + userPrompt;
        AiChatServiceImpl service = new AiChatServiceImpl(openAiClient, deepSeekClient);

        AiChatResponse response = service.chat("deepseek", null, null, "Hello");

        assertThat(response).isEqualTo(new AiChatResponse("deepseek", "deepseek: Hello"));
    }

    @Test
    void chatNormalizesProviderAndMessage() {
        AiProviderClient openAiClient = (systemPrompt, userPrompt) -> "openai: " + userPrompt;
        AiProviderClient deepSeekClient = (systemPrompt, userPrompt) -> "deepseek: " + userPrompt;
        AiChatServiceImpl service = new AiChatServiceImpl(openAiClient, deepSeekClient);

        AiChatResponse response = service.chat(" OpenAI ", " System ", null, " Hello ");

        assertThat(response).isEqualTo(new AiChatResponse("openai", "openai: Hello"));
    }

    @Test
    void chatUsesUserPromptBeforeMessageAndPassesSystemPrompt() {
        AiProviderClient openAiClient = (systemPrompt, userPrompt) -> systemPrompt + " | " + userPrompt;
        AiProviderClient deepSeekClient = (systemPrompt, userPrompt) -> "deepseek: " + userPrompt;
        AiChatServiceImpl service = new AiChatServiceImpl(openAiClient, deepSeekClient);

        AiChatResponse response = service.chat("openai", " You are concise ", " Summarize this ", "ignored");

        assertThat(response).isEqualTo(new AiChatResponse("openai", "You are concise | Summarize this"));
    }

    @Test
    void chatRejectsUnsupportedProvider() {
        AiProviderClient openAiClient = (systemPrompt, userPrompt) -> "openai: " + userPrompt;
        AiProviderClient deepSeekClient = (systemPrompt, userPrompt) -> "deepseek: " + userPrompt;
        AiChatServiceImpl service = new AiChatServiceImpl(openAiClient, deepSeekClient);

        assertThatThrownBy(() -> service.chat("unknown", null, null, "Hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported AI provider: unknown");
    }
}
