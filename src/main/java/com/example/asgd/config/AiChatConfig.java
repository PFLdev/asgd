package com.example.asgd.config;

import com.example.asgd.service.AiProviderClient;
import com.example.asgd.service.impl.SpringAiProviderClient;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AiChatProperties.class)
public class AiChatConfig {

    @Bean("openAiProviderClient")
    public AiProviderClient openAiProviderClient(AiChatProperties properties) {
        AiChatProperties.Provider openai = properties.getOpenai();
        return new SpringAiProviderClient("OpenAI", openai.getApiKey(), () -> {
            OpenAiApi api = OpenAiApi.builder()
                    .apiKey(openai.getApiKey())
                    .baseUrl(openai.getBaseUrl())
                    .completionsPath(openai.getCompletionsPath())
                    .restClientBuilder(RestClient.builder())
                    .webClientBuilder(WebClient.builder())
                    .responseErrorHandler(RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER)
                    .build();
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(openai.getModel())
                    .build();
            return OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(options)
                    .toolCallingManager(toolCallingManager())
                    .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                    .observationRegistry(ObservationRegistry.NOOP)
                    .build();
        });
    }

    @Bean("deepSeekProviderClient")
    public AiProviderClient deepSeekProviderClient(AiChatProperties properties) {
        AiChatProperties.Provider deepseek = properties.getDeepseek();
        return new SpringAiProviderClient("DeepSeek", deepseek.getApiKey(), () -> {
            DeepSeekApi api = DeepSeekApi.builder()
                    .apiKey(deepseek.getApiKey())
                    .baseUrl(deepseek.getBaseUrl())
                    .completionsPath(deepseek.getCompletionsPath())
                    .restClientBuilder(RestClient.builder())
                    .webClientBuilder(WebClient.builder())
                    .responseErrorHandler(RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER)
                    .build();
            DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                    .model(deepseek.getModel())
                    .build();
            return DeepSeekChatModel.builder()
                    .deepSeekApi(api)
                    .defaultOptions(options)
                    .toolCallingManager(toolCallingManager())
                    .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                    .observationRegistry(ObservationRegistry.NOOP)
                    .build();
        });
    }

    private ToolCallingManager toolCallingManager() {
        return ToolCallingManager.builder()
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }
}
