package com.shj.onlinememospringproject.dto;

import com.shj.onlinememospringproject.domain.Memo;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemoDto {

    // ======== < Request DTO > ======== //


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
}
