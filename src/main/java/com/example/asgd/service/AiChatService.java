package com.example.asgd.service;

import com.example.asgd.dto.AiChatResponse;

public interface AiChatService {

    AiChatResponse chat(String provider, String systemPrompt, String userPrompt, String message);
}
