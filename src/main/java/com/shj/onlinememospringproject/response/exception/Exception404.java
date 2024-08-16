package com.shj.onlinememospringproject.response.exception;

import com.shj.onlinememospringproject.response.ResponseCode;
import lombok.Getter;

@Getter
public class Exception404 extends CustomException {

    public Exception404(ResponseCode errorResponseCode, String message) {
        super(errorResponseCode, message);
    }


    public static class NoSuchUser extends Exception404 {
        public NoSuchUser(String message) {
            super(ResponseCode.NOT_FOUND_USER, message);
        }
    }

    public static class NoSuchMemo extends Exception404 {
        public NoSuchMemo(String message) {
            super(ResponseCode.NOT_FOUND_MEMO, message);
        }
    }

    public static class NoSuchUserMemo extends Exception404 {
        public NoSuchUserMemo(String message) {
            super(ResponseCode.NOT_FOUND_USERMEMO, message);
        }
    }

    public static class NoSuchFriendship extends Exception404 {
        public NoSuchFriendship(String message) {
            super(ResponseCode.NOT_FOUND_FRIENDSHIP, message);
        }
    }
}
