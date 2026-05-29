package com.example.asgd.service.impl;

import com.example.asgd.dto.AiChatResponse;
import com.example.asgd.service.AiChatService;
import com.example.asgd.service.AiProviderClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final AiProviderClient openAiClient;
    private final AiProviderClient deepSeekClient;

    public AiChatServiceImpl(
            @Qualifier("openAiProviderClient") AiProviderClient openAiClient,
            @Qualifier("deepSeekProviderClient") AiProviderClient deepSeekClient
    ) {
        this.openAiClient = openAiClient;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public AiChatResponse chat(String provider, String message) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedMessage = message.trim();

        return switch (normalizedProvider) {
            case "openai" -> new AiChatResponse(normalizedProvider, openAiClient.chat(normalizedMessage));
            case "deepseek" -> new AiChatResponse(normalizedProvider, deepSeekClient.chat(normalizedMessage));
            default -> throw new IllegalArgumentException("Unsupported AI provider: " + normalizedProvider);
        };
    }

    private String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("Unsupported AI provider: " + provider);
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
