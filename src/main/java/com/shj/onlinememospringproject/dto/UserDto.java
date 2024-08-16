package com.shj.onlinememospringproject.dto;

import com.shj.onlinememospringproject.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDto {

    // ======== < Request DTO > ======== //

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {

        private String nickname;
    }


    // ======== < Response DTO > ======== //

    @Getter
    @NoArgsConstructor
    public static class Response {

        private Long userId;
        private String email;
        private String nickname;

        public Response(User entity) {
            this.userId = entity.getId();
            this.email = entity.getEmail();
            this.nickname = entity.getNickname();
        }
    }
}
