package com.shj.onlinememospringproject.util;

import com.shj.onlinememospringproject.response.exception.Exception500;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class SecurityUtil {

    private SecurityUtil() { }

    public static Long getCurrentMemberId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // authentication.getName() : userId or "anonymousUser"
        if (authentication == null || authentication.getName() == null || authentication.getName().equals("anonymousUser")) {
            throw new Exception500.AnonymousUser("Security Context에 인증 정보가 없습니다.");
        }

        return Long.parseLong(authentication.getName());
    }
}