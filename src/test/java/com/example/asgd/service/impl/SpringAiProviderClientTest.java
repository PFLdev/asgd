package com.example.asgd.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SpringAiProviderClientTest {

    @Test
    void chatUsesChatClientSystemAndUserPrompts() {
        ChatClientFixture fixture = new ChatClientFixture("ok");
        SpringAiProviderClient client = new SpringAiProviderClient("OpenAI", "api-key", () -> fixture.chatClient);

        String response = client.chat("You are concise", "Summarize this");

        assertThat(response).isEqualTo("ok");
        verify(fixture.requestSpec).system("You are concise");
        verify(fixture.requestSpec).user("Summarize this");
        verify(fixture.requestSpec).call();
    }

    @Test
    void chatOmitsBlankSystemPrompt() {
        ChatClientFixture fixture = new ChatClientFixture("ok");
        SpringAiProviderClient client = new SpringAiProviderClient("OpenAI", "api-key", () -> fixture.chatClient);

        client.chat(" ", "Hello");

        verify(fixture.requestSpec, never()).system(anyString());
        verify(fixture.requestSpec).user("Hello");
    }

    private static final class ChatClientFixture {
        private final ChatClient chatClient = mock(ChatClient.class);
        private final ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        private final ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        private ChatClientFixture(String response) {
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.system(anyString())).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(callResponseSpec);
            given(callResponseSpec.content()).willReturn(response);
        }
    }
}
