package com.shj.onlinememospringproject.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${openai.models.chat.model}")  // 차후 Embedding 등의 모델 추가를 고려한 네이밍.
    private String chatModel;
    @Value("${openai.models.chat.temperature}")
    private double chatTemperature;


    @Bean
    public ChatClient chatClient() {  // Chat AI 모델
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(getOpenAiApi())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModel)
                        .temperature(chatTemperature)
                        .build())
                .build();

        return ChatClient.builder(openAiChatModel)
                .build();
    }

    private OpenAiApi getOpenAiApi() {  // API 키는 모델과 관계없이 공용이므로 분리.
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
    }


//    @Value("${openai.models.embedding.model}")
//    private String embeddingModel;
//
//    @Bean
//    public EmbeddingModel embeddingModel() {  // Embedding AI 모델
//        return new OpenAiEmbeddingModel(
//                getOpenAiApi(),
//                MetadataMode.EMBED,
//                OpenAiEmbeddingOptions.builder()
//                        .model(embeddingModel)
//                        .build(),
//                RetryUtils.DEFAULT_RETRY_TEMPLATE
//        );
//    }
}
