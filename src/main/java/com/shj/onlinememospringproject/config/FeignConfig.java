package com.shj.onlinememospringproject.config;

import com.shj.onlinememospringproject.response.exception.Exception500;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class FeignConfig {

    @Bean
    public Decoder feignDecoder() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();  // 기본 메시지 컨버터

        // 기본 지원되는 미디어 타입에 'text/json' 추가
        List<MediaType> mediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        mediaTypes.add(MediaType.TEXT_PLAIN);  // 'text/plain' 추가
        mediaTypes.add(MediaType.valueOf("text/json"));  // 'text/json' 추가
        converter.setSupportedMediaTypes(mediaTypes);

        ObjectFactory<HttpMessageConverters> messageConverters = () -> new HttpMessageConverters(converter);
        return new ResponseEntityDecoder(new SpringDecoder(messageConverters));
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new ErrorDecoder() {
            private final ErrorDecoder defaultErrorDecoder = new Default();

            @Override
            public Exception decode(String methodKey, Response response) {
                int httpStatus = response.status();
                if(!(300 <= httpStatus && httpStatus < 600)) {  // 적절한 범위는 400~599지만, ErrorDecoder가 300번대도 캐치하므로 300~599로 지정.
                    return defaultErrorDecoder.decode(methodKey, response);  // 위 범위 이외는 기본 응답
                }

                // 400번대, 500번대 예외 응답 (+ 300번대)
                String[] parts = methodKey.split("#");
                String clientClassName = (parts.length > 0) ? parts[0] : "UnknownClient";
                String clientMethodName = (parts.length > 1) ? parts[1].split("\\(")[0].strip() : "UnknownMethod";

                String clientResponseBody = null;  // 널체크없이 "null" 문자열로도 로깅 허용.
                try {
                    if(response.body() != null) {
                        try (InputStream inputStream = response.body().asInputStream()) {
                            byte[] bytes = inputStream.readNBytes(1024);  // 최대 1KB 본문까지만 읽기
                            clientResponseBody = new String(bytes, StandardCharsets.UTF_8)
                                    .replaceAll("\\s+", " ")
                                    .strip();
                        }
                    }
                } catch (IOException ioEx) {
                    clientResponseBody = "외부 응답본문 읽기 실패: " + ioEx.getMessage();
                }

                return new Exception500.ExternalServer(clientClassName, clientMethodName, clientResponseBody, httpStatus);
            }
        };
    }
}
