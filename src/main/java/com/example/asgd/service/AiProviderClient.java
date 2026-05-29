package com.example.asgd.service;

@FunctionalInterface
public interface AiProviderClient {

    String chat(String systemPrompt, String userPrompt);
}
