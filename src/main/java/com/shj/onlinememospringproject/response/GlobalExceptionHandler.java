package com.shj.onlinememospringproject.response;

import com.shj.onlinememospringproject.response.exception.*;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {  // 참고로 Filter에서 throw된 에러는 이보다 앞단에 위치하여 잡아내지 못함.

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception ex) {
        String exMessage = (ex.getMessage() != null) ? ex.getMessage() : "null";
        String exClassName = ex.getClass().getName();
        StringBuilder exStb = new StringBuilder()
                .append(exMessage).append(" (").append(exClassName).append(")");
        return logAndResponse(ResponseCode.INTERNAL_SERVER_ERROR, exStb.toString());
    }

    @ExceptionHandler(JwtException.class)  // Filter 단계 이후에 JWT 토큰 검증시 (Filter 단계에서는 JwtExceptionFilter가 대신 처리.)
    public ResponseEntity handleJwtException(Exception ex) {
        return logAndResponse(ResponseCode.TOKEN_ERROR, ex.getMessage());
    }

    @ExceptionHandler({
            AuthenticationException.class,
            BadCredentialsException.class  // 로그인 실패시
    })
    public ResponseEntity handleUnauthorizedException(Exception ex) {
        return logAndResponse(ResponseCode.UNAUTHORIZED_ERROR, ex.getMessage());
        // 참고로 AuthenticationException 경우에는 예외처리권한이 JwtAuthenticationEntryPoint로 넘어가기에
        // 크롬콘솔에선 설정한방식대로 출력되지않지만, 이는 postman 프로그램에서 확인이 가능하여 명시하였음.
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity handleForbiddenException(Exception ex) {
        return logAndResponse(ResponseCode.FORBIDDEN_ERROR, ex.getMessage());
        // 참고로 AccessDeniedException 경우에는 예외처리권한이 JwtAccessDeniedHandler로 넘어가기에
        // 크롬콘솔에선 설정한방식대로 출력되지않지만, 이는 postman 프로그램에서 확인이 가능하여 명시하였음.
    }

    // ========== 커스텀 예외 처리 ========== //

    // < 400,404,423,500 Exception >
    @ExceptionHandler({
            Exception400.class,
            Exception404.class,
            Exception409.class,
            Exception423.class,
            Exception500.class
    })
    public ResponseEntity handleCustomException(CustomException ex) {
        return logAndResponse(ex.getErrorResponseCode(), ex.getMessage());
    }

    // ========== 유틸성 메소드 ========== //

    private ResponseEntity logAndResponse(ResponseCode responseCode, String message) {
        int statusItem = responseCode.getHttpStatus();
        String messageItem = responseCode.getMessage();

        String prefix = (statusItem == 404 || statusItem == 423) ? "==> error_data / " : "==> error_message / ";  // 404 or 423 예외처리인 경우에만, 'error_data'로 출력.
        StringBuilder logMessageStb = new StringBuilder()
                .append(statusItem).append(" ").append(messageItem).append("\n")
                .append(prefix);

        if(statusItem == 423) {
            logMessageStb.append("Lock user info = ").append(message);
            log.error(logMessageStb.toString());
            return ResponseData.toResponseEntity(responseCode, message);  // 423 예외처리인 경우, message는 Lock 사용자의 정보를 가리킴.
        }

        logMessageStb.append(message);
        log.error(logMessageStb.toString());
        return ResponseData.toResponseEntity(responseCode);
    }
}
