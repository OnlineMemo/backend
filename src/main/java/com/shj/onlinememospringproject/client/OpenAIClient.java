package com.shj.onlinememospringproject.client;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAIClient {

    private final ChatClient chatClient;


    public String getChatAnswer(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
