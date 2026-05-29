package com.example.asgd.controller;

import com.example.asgd.dto.AiChatResponse;
import com.example.asgd.service.AiChatService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiChatService aiChatService;

    @Test
    void chatReturnsOpenAiResponse() throws Exception {
        given(aiChatService.chat(eq("openai"), eq("Hello")))
                .willReturn(new AiChatResponse("openai", "Hi from OpenAI"));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "openai",
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("openai"))
                .andExpect(jsonPath("$.content").value("Hi from OpenAI"));
    }

    @Test
    void chatReturnsDeepSeekResponse() throws Exception {
        given(aiChatService.chat(eq("deepseek"), eq("Hello")))
                .willReturn(new AiChatResponse("deepseek", "Hi from DeepSeek"));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "deepseek",
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("deepseek"))
                .andExpect(jsonPath("$.content").value("Hi from DeepSeek"));
    }

    @Test
    void chatRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "openai",
                                  "message": " "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatReturnsBadRequestForUnsupportedProvider() throws Exception {
        given(aiChatService.chat(eq("unknown"), eq("Hello")))
                .willThrow(new IllegalArgumentException("Unsupported AI provider: unknown"));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "unknown",
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported AI provider: unknown"));
    }

    @Test
    void chatReturnsServiceUnavailableWhenProviderKeyMissing() throws Exception {
        given(aiChatService.chat(eq("openai"), eq("Hello")))
                .willThrow(new NonTransientAiException("OpenAI API key is not configured"));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "openai",
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("OpenAI API key is not configured"));
    }
}
