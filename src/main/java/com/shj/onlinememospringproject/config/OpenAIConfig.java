package com.shj.onlinememospringproject.config;

import com.shj.onlinememospringproject.response.exception.Exception429;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

@Configuration
public class OpenAIConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${openai.models.chat.model}")  // 차후 Embedding 등의 모델 추가를 고려한 네이밍.
    private String chatModel;
    @Value("${openai.models.chat.temperature}")
    private double chatTemperature;
    @Value("${openai.models.chat.max-tokens}")
    private int chatMaxTokens;


    @Bean
    public ChatClient chatClient() {  // Chat AI 모델
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(getOpenAiApi())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModel)
                        .temperature(chatTemperature)
                        .maxTokens(chatMaxTokens)
                        .build())
                .retryTemplate(getCustomRetryTemplate())
                .build();

        return ChatClient.builder(openAiChatModel)
                .build();
    }

    private OpenAiApi getOpenAiApi() {  // API 키는 모델과 관계없이 공용이므로 분리.
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
    }

    private RetryTemplate getCustomRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)  // 총 3회까지 실패 재시도 (첫시도 1회 + 재시도 2회)
                .retryOn(TransientAiException.class)
                .retryOn(ResourceAccessException.class)
                .retryOn(HttpClientErrorException.class)  // onError()에서 처리하기위함.
                .fixedBackoff(Duration.ofMillis(200))  // 0.2초 간격
                .withListener(new RetryListener() {
                    @Override
                    public <T extends Object, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                        if(throwable instanceof HttpClientErrorException ex && ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                            // - OpenAI 429 응답사유 1 : TPM RPM TPD 등, 시간 내 최대 요청횟수 제한에 도달한 경우
                            // - OpenAI 429 응답사유 2 : 크레딧이 부족하거나 월 최대 지출액에 도달한 경우
                            throw new Exception429.ExcessRequestOpenAI(
                                    String.format("OpenAI API 키 소유자가 최대 한도에 도달했습니다. (%s)", ex.getMessage())  // 또는 'ex.getResponseBodyAsString()'
                            );  // 이는 부모에서 catch 후 다시 재전파되므로, 로깅 메세지를 그대로 포함함.
                        }
                    }
                })
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
