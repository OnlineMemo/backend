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

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {

        private Long userId;  // 친구관계를 수락/거절할 해당 상대방의 userId
        private Integer isAccept;  // 수락:1 or 거절:0
    }

    @Getter
    @NoArgsConstructor
    public static class DeleteRequest {

        private Long userId;  // 친구관계를 삭제할 해당 상대방의 userId
    }


    // ======== < Response DTO > ======== //

}
