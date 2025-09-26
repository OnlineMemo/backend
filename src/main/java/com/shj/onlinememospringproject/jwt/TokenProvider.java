package com.shj.onlinememospringproject.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.shj.onlinememospringproject.dto.AuthDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "bearer";
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 180;  // 180분 = 3시간
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 1440 * 14;  // 1440분 x 14 = 24시간 x 14 = 14일 = 2주
    private final Key key;

    // 개선 여지 : 차후 테스트용 객체의 외부 주입이 필요할 경우, DI 방식으로 전환해 결합도를 낮출 예정.
    private static final ObjectReader claimsReader  // 다른 클래스에서는 이를 사용하지 않으므로, 보안상 public 없이 private 선언함.
            = new ObjectMapper().readerFor(Map.class);  // decodeByBase64()에서 호출할 Map 변환용 ObjectReader

    // 주의 : 밑의 @Value는 'springframework.beans.factory.annotation.Value' 소속임. lombok의 @Value와 혼동하지 말것.
    public TokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }


    // 전체 토큰 새로 생성
    public AuthDto.TokenResponse generateTokenDto(Authentication authentication) {
        String accessToken = generateAccessToken(authentication);
        String refreshToken = generateRefreshToken();

        return AuthDto.TokenResponse.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpiresIn(parseClaims(accessToken).getExpiration().getTime())
                .refreshToken(refreshToken)
                .build();
    }

    // Access 토큰이 만료된 경우, Refresh Token으로 Access Token 재발급하기
    public AuthDto.TokenResponse generateAccessTokenByRefreshToken(Authentication authentication, String refreshToken) {
        String accessToken = generateAccessToken(authentication);

        return AuthDto.TokenResponse.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpiresIn(parseClaims(accessToken).getExpiration().getTime())
                .refreshToken(refreshToken)
                .build();
    }

    // Access Token 생성 후 반환하는 메소드
    public String generateAccessToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date tokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);

        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .setExpiration(tokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        return accessToken;
    }

    // Refresh Token 생성 후 반환하는 메소드
    public String generateRefreshToken() {
        long now = (new Date()).getTime();
        Date refreshTokenExpiresIn = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);

        // Refresh Token 생성
        String refreshToken = Jwts.builder()  // 로그인유지(재발급) 용도로써, 중요정보 Claim 없이 만료 시간만 담음.
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        return refreshToken;
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new JwtException("권한 정보가 없는 토큰입니다.");
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);  // domain의 User가 아닌, security.core.userdetails.User 이다.

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;  // 서명이 유효하지 않거나 토큰 형식이 잘못된 경우, JwtException 예외 처리가 발생.
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public Boolean checkTokenStatus(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;  // 유효한 토큰
        } catch (ExpiredJwtException e) {
            return null;  // 만료된 토큰
        } catch (JwtException | IllegalArgumentException e) {
            return false;  // 유효하지 않은 토큰
        }
    }

    public boolean isExpiredToken(String accessToken) {  // 반환결과가 true면 토큰이 만료됨을 의미.
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // 주의 : Base64 디코딩으로 빠르지만 Jwts.parserBuilder 방식과 달리 서명 검증이 없으므로, 유효성이 이미 검증된 토큰에만 사용할것.
    public Object decodeByBase64(String token, String claimName) throws IOException {
        StringTokenizer stt = new StringTokenizer(token, ".");  // JWT 구조 : "header.payload.signature"
        stt.nextToken();  // header
        String payload = stt.nextToken();  // payload

        byte[] decoded = Base64.getUrlDecoder().decode(payload);
        Map<String, Object> claims = claimsReader.readValue(decoded);
        Object claimValue = claims.get(claimName);

        if (claimValue == null) {
            throw new IOException(String.format("JWT 토큰의 %s 클레임은 존재하지 않거나 null 값입니다.", claimName));
        }
        if (claimName.equals("exp")) {
            if (!(claimValue instanceof Integer || claimValue instanceof Long)) {
                throw new IOException("JWT 토큰의 exp 자료형이 Integer 또는 Long이 아닙니다.");
            }
        }
        else if (claimName.equals("sub") || claimName.equals("auth")) {
            if (!(claimValue instanceof String)) {
                throw new IOException(String.format("JWT 토큰의 %s 자료형이 String이 아닙니다.", claimName));
            }
        }
        return claimValue;
    }

//    public Long decodeExpByBase64(String token) throws IOException {
//        StringTokenizer stt = new StringTokenizer(token, ".");  // JWT 구조 : "header.payload.signature"
//        stt.nextToken();  // header
//        String payload = stt.nextToken();  // payload
//
//        byte[] decoded = Base64.getUrlDecoder().decode(payload);
//        Map<String, Object> claims = claimsReader.readValue(decoded);
//        Object expObj = claims.get("exp");
//
//        if (expObj instanceof Integer) {
//            return ((Integer) expObj).longValue();
//        }
//        else if (expObj instanceof Long) {
//            return (Long) expObj;
//        }
//        else {
//            throw new IOException("JWT 토큰의 exp 자료형이 Integer 또는 Long이 아닙니다.");
//        }
//    }

//    public String decodeSubByBase64(String token) throws IOException {
//        StringTokenizer stt = new StringTokenizer(token, ".");  // JWT 구조 : "header.payload.signature"
//        stt.nextToken();  // header
//        String payload = stt.nextToken();  // payload
//
//        byte[] decoded = Base64.getUrlDecoder().decode(payload);
//        Map<String, Object> claims = claimsReader.readValue(decoded);
//        return (String) claims.get("sub");
//    }
}