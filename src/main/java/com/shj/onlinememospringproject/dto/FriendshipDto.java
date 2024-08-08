package com.shj.onlinememospringproject.dto;

import com.shj.onlinememospringproject.domain.enums.FriendshipState;
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
        private FriendshipState friendshipState;
    }

    @Getter
    @NoArgsConstructor
    public static class DeleteRequest {

        private Long userId;  // 친구관계를 삭제할 해당 상대방의 userId
    }


    // ======== < Response DTO > ======== //

}
