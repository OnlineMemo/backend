package com.shj.onlinememospringproject.response.exception;

import com.shj.onlinememospringproject.response.ResponseCode;
import lombok.Getter;

@Getter
public class Exception500 extends CustomException {

    public Exception500(ResponseCode errorResponseCode) {
        super(errorResponseCode, null);
    }

    public Exception500(ResponseCode errorResponseCode, String message) {
        super(errorResponseCode, message);
    }


    public static class AnonymousUser extends Exception500 {  // 시큐리티 헤더의 로그인 정보가 없을때 값을 조회하면 발생.
        public AnonymousUser(String message) {
            super(ResponseCode.ANONYMOUS_USER_ERROR, message);
        }
    }
}