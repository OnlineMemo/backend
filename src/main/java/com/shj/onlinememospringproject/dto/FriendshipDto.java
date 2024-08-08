package com.shj.onlinememospringproject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class FriendshipDto {

    // ======== < Request DTO > ======== //

    @Getter
    @NoArgsConstructor
    public static class SendRequest {

        private String email;  // 내가 친구요청을 보낼 사용자의 email
    }


    // ======== < Response DTO > ======== //

}
