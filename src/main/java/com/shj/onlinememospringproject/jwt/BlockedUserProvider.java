package com.shj.onlinememospringproject.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class BlockedUserProvider {

    private final Set<Long> loginUserIdSet;

    // 주의 : 밑의 @Value는 'springframework.beans.factory.annotation.Value' 소속임. lombok의 @Value와 혼동하지 말것.
    public BlockedUserProvider(@Value("${blacklist.ddos.login-user-id}") String loginUserIdsStr) {  // 구분자(,)로 연결된 userId 문자열을 Set으로 변환.
        Set<Long> loginUserIdSet = new HashSet<>();
        if(loginUserIdsStr != null && !loginUserIdsStr.isBlank()) {
            String[] loginUserIdArr = loginUserIdsStr.split(",");
            for(String loginUserIdStr : loginUserIdArr) {
                loginUserIdSet.add(Long.parseLong(loginUserIdStr.strip()));
            }
        }
        this.loginUserIdSet = Collections.unmodifiableSet(loginUserIdSet);
    }


    public boolean isEmpty() {
        return loginUserIdSet.isEmpty();
    }

    public boolean checkBlockedUser(Long userId) {
        return loginUserIdSet.contains(userId);
    }
}
