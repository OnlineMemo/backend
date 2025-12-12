package com.shj.onlinememospringproject.jwt;

import com.shj.onlinememospringproject.response.item.MessageItem;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {  // HTTP 요청을 가로채 JWT를 검사하고, 사용자를 인증해 SecurityContextHolder에 인증 정보를 설정하는 필터

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private static final String[] EXCLUDE_PATHS = {"/health", "/test", "/reissue", "/login", "/signup", "/password"};

    private final TokenProvider tokenProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = resolveToken(request);  // 헤더 내 토큰값 문자열 (or null)

        // * if : JwtFilter에서 발생한 예외는 ExceptionHandler가 아닌, 앞단의 JwtExceptionFilter로 던져짐.
        // ==> HTTP 요청 -> JwtExceptionFilter.doFilter(JwtFilter) 호출 -> JwtFilter 예외발생 -> JwtExceptionFilter.catch{JwtFilter} 대신처리
        if(jwt != null) {  // 헤더에 비어있지 않은 JWT가 존재하는 경우
            Boolean jwtStatus = tokenProvider.checkTokenStatus(jwt);
            if(jwtStatus == false) {  // 유효하지 않은 토큰인 경우
                throw new JwtException(MessageItem.TOKEN_ERROR);  // InValid 에러
            }
            else if(jwtStatus == null) {  // 만료된 토큰인 경우
                throw new JwtException(MessageItem.TOKEN_EXPIRED);  // Expired 에러
            }
            else {
                Authentication authentication = tokenProvider.getAuthentication(jwt);  // 사용자를 인증. (+ 토큰 내 auth 권한필드 검사)
                SecurityContextHolder.getContext().setAuthentication(authentication);  // SecurityContextHolder에 인증 정보를 설정.
            }
        }
        // * else : 토큰이 없어 JwtFilter를 통과한 후, SecurityConfig에 정의한 URI 권한에 따라 JwtAuthenticationEntryPoint로 던져짐.
        // ==> HTTP 요청 -> JwtExceptionFilter.doFilter(JwtFilter) 호출 -> JwtFilter.doFilter() 통과 -> JwtAuthenticationEntryPoint 401 응답

        filterChain.doFilter(request, response);
    }

    // 토큰값 문자열 리턴 메소드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = getHeaderField(request, AUTHORIZATION_HEADER);
        if(bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);  // 앞부분인 "Bearer "을 제외하여 7인덱스부터 끝까지인 실제 토큰 문자열을 반환.
        }
        return null;
    }

    private String getHeaderField(HttpServletRequest request, String fieldName) {
        String headerField = request.getHeader(fieldName);
        if(StringUtils.hasText(headerField)) {
            return headerField;
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return Arrays.stream(EXCLUDE_PATHS).anyMatch(path::startsWith);
    }
}