package com.shj.onlinememospringproject.client;

import com.shj.onlinememospringproject.response.exception.Exception429;
import com.shj.onlinememospringproject.response.exception.Exception500;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

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
        } catch (HttpClientErrorException hcEx) {
            if(hcEx.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {  // 429 예외 응답
                // - OpenAI 429 응답사유 1 : TPM RPM TPD 등, 시간 내 최대 요청횟수 제한에 도달한 경우
                // - OpenAI 429 응답사유 2 : 크레딧이 부족하거나 월 최대 지출액에 도달한 경우
                throw new Exception429.ExcessRequestOpenAI(
                        String.format("OpenAI API 키 소유자가 최대 한도에 도달했습니다. (%s)", hcEx.getMessage())  // 또는 'hcEx.getResponseBodyAsString()'
                );
            }
            throw new Exception500.ExternalServer(this.getClass().getSimpleName(), "getChatAnswer", hcEx.getMessage());  // clientClassName = "OpenAIClient"
        } catch (Exception ex) {
            throw new Exception500.ExternalServer(this.getClass().getSimpleName(), "getChatAnswer", ex.getMessage());  // clientClassName = "OpenAIClient"
        }
    }
}
