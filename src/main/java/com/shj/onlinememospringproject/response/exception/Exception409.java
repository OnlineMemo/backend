package com.shj.onlinememospringproject.response.exception;

import com.shj.onlinememospringproject.response.ResponseCode;
import lombok.Getter;

@Getter
public class Exception409 extends CustomException {

    public Exception409(ResponseCode errorResponseCode) {
        super(errorResponseCode, null);
    }


    public static class ConflictData extends Exception409 {
        public ConflictData() {
            super(ResponseCode.CONFLICT_DATA_ERROR);
        }
    }
}
