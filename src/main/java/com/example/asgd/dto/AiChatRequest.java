package com.example.asgd.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank String provider,
        String message,
        String systemPrompt,
        String userPrompt
) {

    @AssertTrue(message = "message or userPrompt must not be blank")
    public boolean hasUserPromptContent() {
        return hasText(message) || hasText(userPrompt);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
