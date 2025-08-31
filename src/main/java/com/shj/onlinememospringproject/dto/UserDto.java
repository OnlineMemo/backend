package com.shj.onlinememospringproject.dto;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.util.TimeConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
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

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountResponse {

        private long signupUserCount;  // 총 가입자 수
        private long remainUserCount;  // 탈퇴자 제외 회원 수 (남은 가입자 수)
    }
}
