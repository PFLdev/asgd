package com.example.asgd.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank String provider,
        @NotBlank String message
) {
}
