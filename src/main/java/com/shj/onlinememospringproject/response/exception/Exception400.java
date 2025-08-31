package com.shj.onlinememospringproject.response.exception;

import com.shj.onlinememospringproject.response.ResponseCode;
import lombok.Getter;

@Getter
public class Exception400 extends CustomException {

    public Exception400(ResponseCode errorResponseCode, String message) {
        super(errorResponseCode, message);
    }


    public static class EmailDuplicate extends Exception400 {
        public EmailDuplicate(String message) {
            super(ResponseCode.DUPLICATE_EMAIL, "duplicate : " + message);
        }
    }

    public static class UserMemoDuplicate extends Exception400 {
        public UserMemoDuplicate(String message) {
            super(ResponseCode.DUPLICATE_USERANDMEMO, "duplicate : " + message);
        }
    }

    public static class UserBadRequest extends Exception400 {
        public UserBadRequest(String message) {
            super(ResponseCode.BAD_REQUEST_USER, message);
        }
    }

    public static class MemoBadRequest extends Exception400 {
        public MemoBadRequest(String message) {
            super(ResponseCode.BAD_REQUEST_MEMO, message);
        }
    }

    public static class UserMemoBadRequest extends Exception400 {
        public UserMemoBadRequest(String message) {
            super(ResponseCode.BAD_REQUEST_USERMEMO, message);
        }
    }

    public static class FriendshipBadRequest extends Exception400 {
        public FriendshipBadRequest(String message) {
            super(ResponseCode.BAD_REQUEST_FRIENDSHIP, message);
        }
    }

    public static class TokenBadRequest extends Exception400 {  // 이는 JwtException 경우와 다르게, JWT 자체는 유효하나 DB에 저장된 토큰과 일치하지 않을때 발생.
        public TokenBadRequest(String message) {
            super(ResponseCode.BAD_REQUEST_TOKEN, message);
        }
    }

    public static class Ga4FilteredBadRequest extends Exception400 {
        public Ga4FilteredBadRequest(String message) {
            super(ResponseCode.BAD_REQUEST_GA4FILTERED, message);
        }
    }
}
