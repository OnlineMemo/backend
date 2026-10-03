package com.shj.onlinememospringproject.response.exception;

import com.shj.onlinememospringproject.response.ResponseCode;
import lombok.Getter;

@Getter
public class Exception403 extends CustomException {

    public Exception403(ResponseCode errorResponseCode, String message) {
        super(errorResponseCode, message);
    }


    public static class BlockedUser extends Exception403 {
        public BlockedUser(String message) {
            super(ResponseCode.BLOCKED_USER_ERROR, message);
        }
    }
}
