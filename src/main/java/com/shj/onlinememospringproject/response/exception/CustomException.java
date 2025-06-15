package com.shj.onlinememospringproject.response.exception;

import com.shj.onlinememospringproject.response.ResponseCode;
import lombok.Getter;

@Getter
public abstract class CustomException extends RuntimeException {

    private ResponseCode errorResponseCode;
    private String message;  // 409 or 500 예외인 경우에는 null 가능.

    public CustomException(ResponseCode errorResponseCode, String message) {
        this.errorResponseCode = errorResponseCode;
        this.message = message;
    }
}
