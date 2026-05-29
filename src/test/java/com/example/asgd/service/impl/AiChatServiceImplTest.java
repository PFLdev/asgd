package com.example.asgd.service.impl;

import com.example.asgd.dto.AiChatResponse;
import com.example.asgd.service.AiProviderClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiChatServiceImplTest {

    @Test
    void chatRoutesOpenAiProviderToOpenAiModel() {
        AiProviderClient openAiClient = prompt -> "openai: " + prompt;
        AiProviderClient deepSeekClient = prompt -> "deepseek: " + prompt;
        AiChatServiceImpl service = new AiChatServiceImpl(openAiClient, deepSeekClient);

        AiChatResponse response = service.chat("openai", "Hello");

        assertThat(response).isEqualTo(new AiChatResponse("openai", "openai: Hello"));
    }

    @Test
    void chatRoutesDeepSeekProviderToDeepSeekModel() {
        AiProviderClient openAiClient = prompt -> "openai: " + prompt;
        AiProviderClient deepSeekClient = prompt -> "deepseek: " + prompt;
        AiChatServiceImpl service = new AiChatServiceImpl(openAiClient, deepSeekClient);

        AiChatResponse response = service.chat("deepseek", "Hello");

        assertThat(response).isEqualTo(new AiChatResponse("deepseek", "deepseek: Hello"));
    }

    @Test
    void chatNormalizesProviderAndMessage() {
        AiProviderClient openAiClient = prompt -> "openai: " + prompt;
        AiProviderClient deepSeekClient = prompt -> "deepseek: " + prompt;
        AiChatServiceImpl service = new AiChatServiceImpl(openAiClient, deepSeekClient);

        AiChatResponse response = service.chat(" OpenAI ", " Hello ");

        assertThat(response).isEqualTo(new AiChatResponse("openai", "openai: Hello"));
    }

    @Test
    void chatRejectsUnsupportedProvider() {
        AiProviderClient openAiClient = prompt -> "openai: " + prompt;
        AiProviderClient deepSeekClient = prompt -> "deepseek: " + prompt;
        AiChatServiceImpl service = new AiChatServiceImpl(openAiClient, deepSeekClient);

        assertThatThrownBy(() -> service.chat("unknown", "Hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported AI provider: unknown");
    }
}
