package com.example.asgd.config;

import com.example.asgd.service.AiProviderClient;
import com.example.asgd.service.impl.SpringAiProviderClient;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AiChatProperties.class)
public class AiChatConfig {

    @Bean("openAiProviderClient")
    public AiProviderClient openAiProviderClient(
            AiChatProperties properties,
            @Qualifier("openAiChatClient") ObjectProvider<ChatClient> chatClientProvider
    ) {
        AiChatProperties.Provider openai = properties.getOpenai();
        return new SpringAiProviderClient("OpenAI", openai.getApiKey(), chatClientProvider::getObject);
    }

    @Bean("openAiChatClient")
    @Lazy
    public ChatClient openAiChatClient(AiChatProperties properties) {
        return ChatClient.create(openAiChatModel(properties.getOpenai()));
    }

    @Bean("deepSeekProviderClient")
    public AiProviderClient deepSeekProviderClient(
            AiChatProperties properties,
            @Qualifier("deepSeekChatClient") ObjectProvider<ChatClient> chatClientProvider
    ) {
        AiChatProperties.Provider deepseek = properties.getDeepseek();
        return new SpringAiProviderClient("DeepSeek", deepseek.getApiKey(), chatClientProvider::getObject);
    }

    @Bean("deepSeekChatClient")
    @Lazy
    public ChatClient deepSeekChatClient(AiChatProperties properties) {
        return ChatClient.create(deepSeekChatModel(properties.getDeepseek()));
    }

    private ChatModel openAiChatModel(AiChatProperties.Provider openai) {
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
    }

    private ChatModel deepSeekChatModel(AiChatProperties.Provider deepseek) {
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
    }

    private ToolCallingManager toolCallingManager() {
        return ToolCallingManager.builder()
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }
}
