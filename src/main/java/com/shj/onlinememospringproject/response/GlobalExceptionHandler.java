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
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {  // Filter 예외는 이보다 앞단(DispatcherServlet 외부)에 위치해, 여기서 감지 X.

    private static final String PROJECT_PACKAGE = "com.shj.onlinememospringproject";
    private static final String EXCEPTION_FILTER_CLASSNAME = "JwtExceptionFilter";
    private static final String CGLIB_STRING = "$$SpringCGLIB$$";
    private static final Pattern CGLIB_PATTERN = Pattern.compile("\\$\\$SpringCGLIB\\$\\$\\d+");  // CGLIB 프록시 패턴


    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception ex) {
        StringBuilder exStb = new StringBuilder();
        StringBuilder traceStb = new StringBuilder();

        // [ 예외 정보 로깅 (error_message) ]
        String exMessage = (ex.getMessage() != null) ? ex.getMessage() : "null";
        String exClassName = ex.getClass().getName();
        exStb.append(exMessage).append(" (").append(exClassName).append(")");

        // [ 예외 호출경로 로깅 (error_trace) ]
        StackTraceElement[] traces = ex.getStackTrace();  // 500 예외처리인 경우, 상세 위치까지 추가로 기록.
        String traceClassName;

        // Trace 우선순위 1: 내 서비스 코드 발생위치
        for(StackTraceElement trace : traces) {
            traceClassName = trace.getClassName();

            if(traceClassName.startsWith(PROJECT_PACKAGE)) {
                if(traceClassName.contains(EXCEPTION_FILTER_CLASSNAME)) continue;  // 모든 Trace는 필터로 귀결되므로 제외.
                if(traceClassName.contains(CGLIB_STRING)) continue;  // CGLIB 프록시는 출력이 중복되므로 제외.
                appendTraceInfo(traceStb, trace, traceClassName);
            }
        }

        // Trace 우선순위 2: 첫번째 스택 발생위치
        if(traceStb.isEmpty() && traces.length > 0) {
            traceClassName = CGLIB_PATTERN.matcher(traces[0].getClassName()).replaceAll("");
            appendTraceInfo(traceStb, traces[0], traceClassName);
        }
        exStb.append(traceStb);

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

    private static void appendTraceInfo(StringBuilder traceStb, StackTraceElement trace, String traceClassName) {
        boolean isFirstTrace = traceStb.isEmpty();
        traceStb.append(isFirstTrace ? "\n==> error_trace / " : "\n                  ")
                .append(traceClassName).append(".").append(trace.getMethodName());

        int traceLineNumber = trace.getLineNumber();
        if(traceLineNumber > 0) {
            traceStb.append(" (Line:").append(traceLineNumber).append(")");
        }
    }

    private static ResponseEntity logAndResponse(ResponseCode responseCode, String message) {
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
