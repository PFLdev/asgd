package com.example.asgd.service.impl;

import com.example.asgd.service.AiProviderClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

public class SpringAiProviderClient implements AiProviderClient {

    private final String providerName;
    private final String apiKey;
    private final Supplier<ChatClient> chatClientFactory;
    private ChatClient chatClient;

    public SpringAiProviderClient(String providerName, String apiKey, Supplier<ChatClient> chatClientFactory) {
        this.providerName = providerName;
        this.apiKey = apiKey;
        this.chatClientFactory = chatClientFactory;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new NonTransientAiException(providerName + " API key is not configured");
        }
        ChatClient.ChatClientRequestSpec requestSpec = getChatClient().prompt();
        if (StringUtils.hasText(systemPrompt)) {
            requestSpec = requestSpec.system(systemPrompt.trim());
        }
        return requestSpec.user(userPrompt.trim()).call().content();
    }

    private ChatClient getChatClient() {
        if (chatClient == null) {
            chatClient = chatClientFactory.get();
        }
        return chatClient;
    }
}
