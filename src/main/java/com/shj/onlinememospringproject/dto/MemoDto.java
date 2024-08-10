package com.shj.onlinememospringproject.dto;

import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MemoDto {

    // ======== < Request DTO > ======== //

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {

        private String title;
        private String content;
        private List<Long> userIdList;  // 함께 공동메모를 생성할 사용자들 userId 리스트. null 허용. (null일 경우, 개인메모)
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {

        private String title;
        private String content;
        private Integer isStar;  // null 허용. (null일 경우, 메모 제목과 내용만 수정함을 의미.)
    }

    @Getter
    @NoArgsConstructor
    public static class InviteRequest {

        private List<Long> userIdList;  // 추가적으로 초대할 사용자들 userId 리스트
    }


    // ======== < Response DTO > ======== //

    @Getter
    @NoArgsConstructor
    public static class Response {

        private Long memoId;
        private String title;
        private String content;
        private Integer isStar;
        private String modifiedTime;

        public Response(Memo entity) {
            this.memoId = entity.getId();
            this.title = entity.getTitle();
            this.content = entity.getContent();
            this.isStar = entity.getIsStar();
            this.modifiedTime = entity.getModifiedTime();
        }
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoPageResponse {

        // Memo
        private Long memoId;
        private String title;
        private Integer isStar;
        private String modifiedTime;

        // User
        private List<UserDto.Response> userResponseDtoList;  // 해당 메모를 가지고 있는 사용자들의 리스트

        // UserMemo
        private Integer memoHasUsersCount;  // 해당 메모를 가지고 있는 사용자의 수

        public MemoPageResponse(Memo entity) {
            this.memoId = entity.getId();
            this.title = entity.getTitle();
            this.isStar = entity.getIsStar();
            this.modifiedTime = entity.getModifiedTime();

            List<UserDto.Response> userResponseDtoList = entity.getUserMemoList().stream()
                    .map(UserMemo::getUser)
                    .map(UserDto.Response::new)
                    .sorted(Comparator.comparing(UserDto.Response::getNickname)
                            .thenComparing(UserDto.Response::getUserId))
                    .collect(Collectors.toList());
            this.userResponseDtoList = userResponseDtoList;
            this.memoHasUsersCount = userResponseDtoList.size();
        }
    }
}
