package com.shj.onlinememospringproject.response;

import com.blueconic.browscap.BrowsCapField;
import com.blueconic.browscap.Capabilities;
import com.blueconic.browscap.UserAgentParser;
import com.shj.onlinememospringproject.response.exception.*;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {  // Filter 예외는 이보다 앞단(DispatcherServlet 외부)에 위치하므로, 여기서 감지 X.

    private static final String HANDLER_ATTRIBUTE = "org.springframework.web.servlet.HandlerMapping.bestMatchingHandler";
    private static final String PROJECT_PACKAGE = "com.shj.onlinememospringproject";
    private static final String CGLIB_STRING = "$$SpringCGLIB$$";
    private static final Pattern CGLIB_PATTERN = Pattern.compile("\\$\\$SpringCGLIB\\$\\$\\d+");  // CGLIB 프록시 패턴
    private static final String[] FILTER_CLASSNAMES = {"JwtExceptionFilter", "JwtFilter"};

    private static final Marker ERROR_500_LOG_MARKER = MarkerFactory.getMarker("ERROR_500_LOG");
    private static final Marker OPENAI_429_LOG_MARKER = MarkerFactory.getMarker("OPENAI_429_LOG");

    private final UserAgentParser userAgentParser;


    // < 500 Exception >
    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception ex, WebRequest webRequest) {
        StringBuilder exStb = new StringBuilder();  // with 'StringBuilder requestStb'
        StringBuilder traceStb = new StringBuilder();

        // [ 예외 정보 로깅 (error_message) ]
        String exMessage = (ex.getMessage() != null) ? ex.getMessage() : "null";
        String exClassName = ex.getClass().getName();
        exStb.append(exMessage).append(" (").append(exClassName).append(")");

        // [ 예외 요청값 로깅 (error_request) ]
        appendRequestInfo(exStb, webRequest);

        // [ 예외 호출경로 로깅 (error_trace) ]
        StackTraceElement[] traces = ex.getStackTrace();  // 500 예외처리인 경우, 상세 위치까지 추가로 기록.
        String traceClassName;

        // Trace 우선순위 1: 내 서비스 코드 발생위치
        int appendCnt = 0;
        for(StackTraceElement trace : traces) {
            traceClassName = trace.getClassName();

            if(traceClassName.startsWith(PROJECT_PACKAGE)) {
                if(Arrays.stream(FILTER_CLASSNAMES).anyMatch(traceClassName::contains)) continue;  // 모든 Trace는 필터로 귀결되므로 제외.
                if(traceClassName.contains(CGLIB_STRING)) continue;  // CGLIB 프록시는 출력이 중복되므로 제외.
                appendTraceInfo(traceStb, trace, traceClassName);
                if(++appendCnt >= 20) break;  // trace 로그는 최대 20개까지만 기록.
            }
        }

        // Trace 우선순위 2: 첫번째 스택 발생위치
        if(traceStb.isEmpty() && traces.length > 0) {
            traceClassName = CGLIB_PATTERN.matcher(traces[0].getClassName()).replaceAll("");
            appendTraceInfo(traceStb, traces[0], traceClassName);
        }

        exStb.append(traceStb);
        return logAndResponse(ResponseCode.INTERNAL_SERVER_ERROR, exStb.toString(), ERROR_500_LOG_MARKER);
    }

    // < 401 Exception - JWT >
    @ExceptionHandler(JwtException.class)  // Filter 단계 이후에 JWT 토큰 검증시 (Filter 단계에서는 JwtExceptionFilter가 대신 처리.)
    public ResponseEntity handleJwtException(Exception ex) {
        return logAndResponse(ResponseCode.TOKEN_ERROR, ex.getMessage(), null);
    }

    // < 401 Exception - UNAUTHORIZED >
    @ExceptionHandler({
            AuthenticationException.class,
            BadCredentialsException.class  // 로그인 실패시
    })
    public ResponseEntity handleUnauthorizedException(Exception ex) {
        return logAndResponse(ResponseCode.UNAUTHORIZED_ERROR, ex.getMessage(), null);
        // 참고로 AuthenticationException 경우에는 예외처리권한이 JwtAuthenticationEntryPoint로 넘어가기에
        // 크롬콘솔에선 설정한방식대로 출력되지않지만, 이는 postman 프로그램에서 확인이 가능하여 명시하였음.
    }

    // < 403 Exception >
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity handleForbiddenException(Exception ex) {
        return logAndResponse(ResponseCode.FORBIDDEN_ERROR, ex.getMessage(), null);
        // 참고로 AccessDeniedException 경우에는 예외처리권한이 JwtAccessDeniedHandler로 넘어가기에
        // 크롬콘솔에선 설정한방식대로 출력되지않지만, 이는 postman 프로그램에서 확인이 가능하여 명시하였음.
    }

    // < 405,406,415 Exception >
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,  // 405
            HttpMediaTypeNotAcceptableException.class,  // 406
            HttpMediaTypeNotSupportedException.class  // 415
    })
    public ResponseEntity handleInvalidHttpException(Exception ex, WebRequest webRequest) {
        StringBuilder exStb = new StringBuilder();  // with 'StringBuilder requestStb'

        // [ 예외 정보 로깅 (error_message) ]
        exStb.append(ex.getMessage());

        // [ 예외 요청값 로깅 (error_request) ]
        appendRequestInfo(exStb, webRequest);

        // 우선순위: HttpMethod 행위 (405) -> ContentType 요청타입 (415) -> Accept 응답타입 (406)
        if(ex instanceof HttpRequestMethodNotSupportedException) {
            return logAndResponse(ResponseCode.NOT_ALLOWED_METHOD, exStb.toString(), null);
        }
        else if(ex instanceof HttpMediaTypeNotSupportedException) {
            return logAndResponse(ResponseCode.UNSUPPORTED_TYPE, exStb.toString(), null);
        }
        return logAndResponse(ResponseCode.NOT_ACCEPTABLE_TYPE, exStb.toString(), null);  // instanceof HttpMediaTypeNotAcceptableException
    }


    // ========== 커스텀 예외 처리 ========== //

    // < 400,404,409,423,500 Exception >
    @ExceptionHandler({
            Exception400.class,
            Exception404.class,
            Exception409.class,
            Exception423.class,
            Exception500.class
    })
    public ResponseEntity handleCommonCustomException(CustomException ex) {
        return logAndResponse(ex.getErrorResponseCode(), ex.getMessage(), null);
    }

    // < 429 Exception >
    @ExceptionHandler({
            Exception429.class
    })
    public ResponseEntity handle429CustomException(CustomException ex) {
        if(ex instanceof Exception429.ExcessRequestOpenAI) {
            return logAndResponse(ex.getErrorResponseCode(), ex.getMessage(), OPENAI_429_LOG_MARKER);
        }
        return logAndResponse(ex.getErrorResponseCode(), ex.getMessage(), null);
    }


    // ========== 유틸성 메소드 ========== //

    private void appendRequestInfo(StringBuilder requestStb, WebRequest webRequest) {
        if(!(webRequest instanceof ServletWebRequest servletWebRequest)) return;
        HttpServletRequest httpServletRequest = servletWebRequest.getRequest();
        requestStb.append("\n==> error_request / ");

        // controller method
        Object handler = httpServletRequest.getAttribute(HANDLER_ATTRIBUTE);
        if(handler instanceof HandlerMethod handlerMethod) {
            requestStb.append(handlerMethod.getBeanType().getSimpleName()).append(".").append(handlerMethod.getMethod().getName());
        }
        else requestStb.append("Unknown Controller");

        // URI
        requestStb.append(" (URI: ").append(httpServletRequest.getRequestURI());
        if(httpServletRequest.getQueryString() != null) {
            requestStb.append("?").append(httpServletRequest.getQueryString());
        }

        // HTTP method
        requestStb.append("[").append(httpServletRequest.getMethod()).append("]").append(")");

        // Content-Type header
        requestStb.append("\n                    Content-Type: ").append(httpServletRequest.getContentType());  // 널체크없이 "null" 문자열로도 로깅 허용.

        // Accept header
        requestStb.append(", Accept: ").append(httpServletRequest.getHeader("Accept"));  // 널체크없이 "null" 문자열로도 로깅 허용.

        // User-Agent header
        String userAgentStr = httpServletRequest.getHeader("User-Agent");
        requestStb.append("\n                    User-Agent: ");
        try {
            Capabilities capabilities = userAgentParser.parse(userAgentStr);
            requestStb.append("browser=").append(capabilities.getBrowserType()).append("(").append(capabilities.getBrowser()).append(")")  // browser=%s(%s)
                    .append(", device=").append(capabilities.getDeviceType()).append("(").append(capabilities.getPlatform()).append(")")  // device=%s(%s)
                    .append(", isCrawler=").append(capabilities.getValue(BrowsCapField.IS_CRAWLER).equals("true") ? 'O' : 'X')  // isCrawler=%c
                    .append(", isFake=").append(capabilities.getValue(BrowsCapField.IS_FAKE).equals("true") ? 'O' : 'X')  // isFake=%c
                    .append(", isModified=").append(capabilities.getValue(BrowsCapField.IS_MODIFIED).equals("true") ? 'O' : 'X');  // isModified=%c
        } catch (Exception ex) {
            requestStb.append(userAgentStr);  // 널체크없이 "null" 문자열로도 로깅 허용.
        }
    }

    private static void appendTraceInfo(StringBuilder traceStb, StackTraceElement trace, String traceClassName) {
        boolean isFirstTrace = traceStb.isEmpty();
        traceStb.append(isFirstTrace ? "\n==> error_trace / " : "\n                  ")
                .append(traceClassName).append(".").append(trace.getMethodName());

        int traceLineNumber = trace.getLineNumber();
        if(traceLineNumber > 0) {
            traceStb.append(" (Line:").append(traceLineNumber).append(")");
        }
    }

    private static ResponseEntity logAndResponse(ResponseCode responseCode, String message, Marker marker) {
        int statusItem = responseCode.getHttpStatus();
        String messageItem = responseCode.getMessage();

        String prefix = (statusItem == 404)
                ? "==> error_data / "  // 404 예외처리인 경우에만, 'error_data'로 로깅. (존재하지 않는 데이터의 상세 정보임을 명시하기 위함.)
                : "==> error_message / ";
        StringBuilder logMessageStb = new StringBuilder()
                .append(statusItem).append(" ").append(messageItem).append("\n")
                .append(prefix);

        if(statusItem == 423) {  // 423 예외처리인 경우, message는 Lock 사용자의 정보를 가리킴.
            logMessageStb.append("Lock user info = ").append(message);
            log.error(logMessageStb.toString());
            return ResponseData.toResponseEntity(responseCode, message);  // 메모가 잠겼으므로 클라이언트에 편집자 정보(message)를 포함해 응답해야함.
        }

        logMessageStb.append(message);
        if(marker != null) log.error(marker, logMessageStb.toString());
        else log.error(logMessageStb.toString());

        return ResponseData.toResponseEntity(responseCode);
    }

//    private static ResponseEntity responseWithoutLog(ResponseCode responseCode, String message) {
//        if(responseCode.getHttpStatus() == 423) {  // 423 예외처리인 경우, message는 Lock 사용자의 정보를 가리킴.
//            return ResponseData.toResponseEntity(responseCode, message);  // 메모가 잠겼으므로 클라이언트에 편집자 정보(message)를 포함해 응답해야함.
//        }
//        return ResponseData.toResponseEntity(responseCode);
//    }
}
