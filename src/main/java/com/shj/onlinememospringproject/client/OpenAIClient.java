package com.shj.onlinememospringproject.client;

import com.shj.onlinememospringproject.response.exception.Exception429;
import com.shj.onlinememospringproject.response.exception.Exception500;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAIClient {

    private final ChatClient chatClient;


    public String getChatAnswer(String question) {
        try {
            return chatClient.prompt()
                    .user(question)
                    .call()
                    .content();
        } catch (NonTransientAiException ntaEx) {
            if(ntaEx.getMessage() != null && ntaEx.getMessage().startsWith("429")) {  // 429 예외 응답
                // - OpenAI 429 응답사유 1 : TPM RPM TPD 등, 시간 내 최대 요청횟수 제한에 도달한 경우
                // - OpenAI 429 응답사유 2 : 크레딧이 부족하거나 월 최대 지출액에 도달한 경우
                throw new Exception429.ExcessRequestOpenAI(
                        String.format("OpenAI API 키 소유자가 최대 한도에 도달했습니다. (%s)", ntaEx.getMessage())
                );
            }
            throw new Exception500.ExternalServer(this.getClass().getSimpleName(), "getChatAnswer", ntaEx.getMessage());  // clientClassName = "OpenAIClient"
        } catch (Exception ex) {
            throw new Exception500.ExternalServer(this.getClass().getSimpleName(), "getChatAnswer", ex.getMessage());  // clientClassName = "OpenAIClient"
        }
    }
}
