package com.shj.onlinememospringproject.response.exception;

import com.shj.onlinememospringproject.response.ResponseCode;
import lombok.Getter;

@Getter
public class Exception429 extends CustomException {

    public Exception429(ResponseCode errorResponseCode, String message) {
        super(errorResponseCode, message);
    }


    public static class ExcessRequestOpenAI extends Exception429 {
        public ExcessRequestOpenAI(String message) {
            super(ResponseCode.EXCESS_REQUEST_OPENAI, message);
        }
    }
}
