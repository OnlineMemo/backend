package com.shj.onlinememospringproject.ratelimit;

import com.shj.onlinememospringproject.domain.enums.UserState;
import com.shj.onlinememospringproject.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BlockedUserProvider {

    private final UserRepository userRepository;

    private volatile Set<Long> blockedUserIdSet = Collections.emptySet();  // 스케줄러 스레드에서 교체되므로 volatile 선언


    @PostConstruct
    @Scheduled(fixedRate = 1000 * 60, initialDelay = 1000 * 60)  // 1분 간격으로 실행
    public void syncBlockedUserIdSet() {
        Set<Long> blockedUserIdSet = userRepository.findIdSetByUserState(UserState.BLOCKED);
        this.blockedUserIdSet = Collections.unmodifiableSet(blockedUserIdSet);
    }

    public boolean isEmpty() {
        return blockedUserIdSet.isEmpty();
    }

    public boolean checkBlockedUser(Long userId) {
        return blockedUserIdSet.contains(userId);
    }
}
