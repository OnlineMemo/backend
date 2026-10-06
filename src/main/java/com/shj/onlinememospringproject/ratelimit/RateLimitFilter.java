package com.shj.onlinememospringproject.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import com.shj.onlinememospringproject.response.item.MessageItem;
import com.shj.onlinememospringproject.response.item.StatusItem;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {  // 인증된 계정의 요청 횟수를 검사하고, 초과 시 429 응답하는 필터

    private static final long ERROR_500_LOG_INTERVAL = 1000 * 60 * 60;  // 60분 = 1시간
    private static final Marker ERROR_500_LOG_MARKER = MarkerFactory.getMarker("ERROR_500_LOG");

    private final RateLimitProvider rateLimitProvider;
    private final ObjectMapper objectMapper;

    private volatile long lastErrorLogTime = 0;  // 여러 요청 스레드에서 갱신되므로 volatile 선언


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null) {  // 로그인 요청만 검사 (비로그인은 Cloudflare/WAF IP 제한으로 처리)
            Long userId = Long.valueOf(authentication.getName());
            ConsumptionProbe probe = tryConsume(request, userId);
            if(probe != null && !probe.isConsumed()) {  // 요청 횟수를 초과한 경우
                writeErrorResponse(response, probe);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private ConsumptionProbe tryConsume(HttpServletRequest request, Long userId) {
        try {
            return rateLimitProvider.tryConsume(userId);
        } catch (Exception ex) {  // 저장소 장애 시 검사 생략 (fail-open)
            long now = System.currentTimeMillis();
            if(now - lastErrorLogTime >= ERROR_500_LOG_INTERVAL) {  // 알림 폭주 방지를 위해 1시간에 1번만 로깅
                lastErrorLogTime = now;
                log.error(ERROR_500_LOG_MARKER,
                        String.format("%d %s\n==> error_message / RateLimit 저장소 장애로 사용자(userId=%d)의 요청제한 검사 생략 : %s\n==> error_request / RateLimitFilter.tryConsume (URI: %s[%s])",  // Slack Template
                                StatusItem.INTERNAL_SERVER_ERROR, MessageItem.INTERNAL_SERVER_ERROR, userId, ex.getMessage(), request.getRequestURI(), request.getMethod()));
            }
            return null;
        }
    }

    private void writeErrorResponse(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        long retryAfterSeconds = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1e9);  // 나노초 -> 초 (올림)

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(StatusItem.TO_MANY_REQUESTS);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ResponseEntity responseEntity = ResponseData.toResponseEntity(ResponseCode.EXCESS_REQUEST_USER);

        // 전체 ResponseEntity 객체를 JSON 문자열로 변환.
        String jsonString = objectMapper.writeValueAsString(responseEntity);
        // 위의 JSON 문자열에서 "body" 필드만 추출.
        JsonNode rootNode = objectMapper.readTree(jsonString);
        JsonNode dataNode = rootNode.path("body");
        String jsonData = objectMapper.writeValueAsString(dataNode);

        response.getWriter().write(jsonData);
    }
}
