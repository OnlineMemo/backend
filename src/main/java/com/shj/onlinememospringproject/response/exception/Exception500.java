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

    public static class ExternalServer extends Exception500 {
        // only message
        public ExternalServer(String message) {
            super(ResponseCode.EXTERNAL_SERVER_ERROR, message);
        }

        // exclude httpStatus
        public ExternalServer(String clientClassName, String clientMethodName, String clientExMessage) {  // 외부 API Client 종류 기재할것.
            super(ResponseCode.EXTERNAL_SERVER_ERROR,
                    String.format("%s.%s API 호출 에러 (%s)",
                            clientClassName, clientMethodName, clientExMessage));
        }

        // include httpStatus
        public ExternalServer(String clientClassName, String clientMethodName, String clientExMessage, int httpStatus) {  // 외부 API Client 종류 기재할것.
            super(ResponseCode.EXTERNAL_SERVER_ERROR,
                    String.format("%s.%s API 호출 에러 (%d Status - %s)",
                            clientClassName, clientMethodName, httpStatus, clientExMessage));
        }
    }
}