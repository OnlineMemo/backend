package com.shj.onlinememospringproject.response.exception;

import com.shj.onlinememospringproject.response.ResponseCode;
import lombok.Getter;

@Getter
public class Exception423 extends CustomException {

    public Exception423(ResponseCode errorResponseCode, String message) {
        super(errorResponseCode, message);
    }


    public static class LockedData extends Exception423 {
        public LockedData(String message) {
            super(ResponseCode.LOCKED_DATA_ERROR, message);
        }
    }
}
