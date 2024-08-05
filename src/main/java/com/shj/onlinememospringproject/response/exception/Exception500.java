package com.shj.onlinememospringproject.response.exception;

import com.shj.onlinememospringproject.response.ResponseCode;
import lombok.Getter;

@Getter
public class Exception500 extends CustomException {

    public Exception500(ResponseCode errorResponseCode) {
        super(errorResponseCode, null);
    }

}