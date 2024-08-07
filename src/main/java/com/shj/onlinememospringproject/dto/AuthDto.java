package com.shj.onlinememospringproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    // ======== < Request DTO > ======== //

    @Getter
    @NoArgsConstructor
    public static class SignupRequest {

        private String email;
        private String password;
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    public static class LoginRequest {

        private String email;
        private String password;
    }


    // ======== < Response DTO > ======== //

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {

        private String grantType;
        private String accessToken;
        private Long accessTokenExpiresIn;
    }
}
