package com.shj.onlinememospringproject.dto;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.util.TimeConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
        private LocalDateTime createdTime;
        private String createdTimeStr;

        public Response(User entity) {
            this.userId = entity.getId();
            this.email = entity.getEmail();
            this.nickname = entity.getNickname();
            this.createdTime = entity.getCreatedTime();
            this.createdTimeStr = TimeConverter.timeToString(entity.getCreatedTime());
        }
    }
}
