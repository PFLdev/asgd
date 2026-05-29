package com.example.asgd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.chat")
public class AiChatProperties {

    private Provider openai = new Provider("https://api.openai.com", "/v1/chat/completions", "gpt-4o-mini");

    private Provider deepseek = new Provider("https://api.deepseek.com", "/chat/completions", "deepseek-chat");

    public Provider getOpenai() {
        return openai;
    }

    public void setOpenai(Provider openai) {
        this.openai = openai;
    }

    public Provider getDeepseek() {
        return deepseek;
    }

    public void setDeepseek(Provider deepseek) {
        this.deepseek = deepseek;
    }

    public static class Provider {

        private String apiKey;

        private String baseUrl;

        private String completionsPath;

        private String model;

        public Provider() {
        }

        public Provider(String baseUrl, String completionsPath, String model) {
            this.baseUrl = baseUrl;
            this.completionsPath = completionsPath;
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getCompletionsPath() {
            return completionsPath;
        }

        public void setCompletionsPath(String completionsPath) {
            this.completionsPath = completionsPath;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
