package com.example.asgd.service.impl;

import com.example.asgd.service.AiProviderClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

public class SpringAiProviderClient implements AiProviderClient {

    private final String providerName;
    private final String apiKey;
    private final Supplier<ChatModel> chatModelFactory;
    private ChatModel chatModel;

    public SpringAiProviderClient(String providerName, String apiKey, Supplier<ChatModel> chatModelFactory) {
        this.providerName = providerName;
        this.apiKey = apiKey;
        this.chatModelFactory = chatModelFactory;
    }

    @Override
    public String chat(String message) {
        if (!StringUtils.hasText(apiKey)) {
            throw new NonTransientAiException(providerName + " API key is not configured");
        }
        return getChatModel().call(message);
    }

    private ChatModel getChatModel() {
        if (chatModel == null) {
            chatModel = chatModelFactory.get();
        }
        return chatModel;
    }
}
