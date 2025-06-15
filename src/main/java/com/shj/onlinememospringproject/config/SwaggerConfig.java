package com.shj.onlinememospringproject.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.env}")
    private String serverEnv;
    @Value("${server.url}")
    private String serverUrl;


    @Bean
    public OpenAPI openAPI() {
        if(serverEnv.equals("prod")) {
            return null;  // prod 환경일 경우 Swagger 설정을 반환하지 않음. (어차피 현재는 properties에 분리해두어 사용X)
        }

        String authName = "JWT";  // 기능 타이틀명
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(authName);

        Components components = new Components()
                .addSecuritySchemes(
                        authName,
                        new SecurityScheme()
                                .name(authName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("Bearer")
                                .bearerFormat("JWT")
                                .description("'Bearer '을 제외한 Access Token 입력하세요.")
                );

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components)
                .info(apiInfo())
                .servers(apiServer());
    }

    private Info apiInfo() {
        return new Info()
                .title("OnlineMemo Ver.2 - Swagger API")
                .description("<a href=\"https://www.onlinememo.kr/\" target=\"blank\">OnlineMemo - Web</a> / <a href=\"https://play.google.com/store/apps/details?id=com.shj.onlinememo\" target=\"blank\">OnlineMemo - App</a>")
                .version("2.0.0");
    }

    private List<Server> apiServer() {
        String description = "Local Server";
        if(serverEnv.equals("prod")) description = "Prod Server";

        Server server = new Server()
                .description(description)
                .url(serverUrl);

        return Arrays.asList(server);
    }
}