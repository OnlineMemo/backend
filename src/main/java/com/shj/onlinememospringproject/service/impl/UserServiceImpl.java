package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.enums.UserState;
import com.shj.onlinememospringproject.dto.UserDto;
import com.shj.onlinememospringproject.ratelimit.BlockedUserProvider;
import com.shj.onlinememospringproject.ratelimit.RateLimitProvider;
import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.response.exception.Exception404;
import com.shj.onlinememospringproject.service.UserService;
import com.shj.onlinememospringproject.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RateLimitProvider rateLimitProvider;
    private final BlockedUserProvider blockedUserProvider;


    @Transactional(readOnly = true)
    @Override
    public UserDto.Response findUserProfile() {
        User user = findLoginUser();
        UserDto.Response userResponseDto = new UserDto.Response(user);
        return userResponseDto;
    }

    @Transactional
    @Override
    public void updateUserProfile(UserDto.UpdateRequest updateRequestDto) {
        User user = findLoginUser();
        user.updateNickName(updateRequestDto.getNickname());
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto.CountResponse countUsers() {
        long signupUserCount = userRepository.findMaxUserId().orElse(0L);
        long remainUserCount = userRepository.count();
        return UserDto.CountResponse.builder()
                .signupUserCount(signupUserCount)
                .remainUserCount(remainUserCount)
                .withdrawnUserCount(signupUserCount - remainUserCount)
                .build();
    }

    @Transactional
    @Override
    public void updateUserBlock(Long userId, UserDto.UpdateBlockRequest updateBlockRequestDto) {
        User user = findUser(userId);

        if(updateBlockRequestDto.getIsBlock() == 1) {  // 유저 영구정지일 경우
            user.updateUserState(UserState.BLOCKED);
        }
        else if(updateBlockRequestDto.getIsBlock() == 0) {  // 유저 영구정지 해제일 경우
            user.updateUserState(UserState.ACTIVE);
        }
        else {  // 잘못된 영구정지 수정 요청일 경우
            throw new Exception400.UserBadRequest("잘못된 필드값으로 API를 요청하였습니다.");
        }
        rateLimitProvider.deleteBlock(userId);  // 영구정지 및 해제 시 24시간 차단 초기화
        rateLimitProvider.deleteBlockCount(userId);  // 영구정지 및 해제 시 24시간 차단 횟수 초기화
        blockedUserProvider.syncBlockedUserIdSet();  // 1분 주기를 기다리지 않고 즉시 반영
    }


    // ========== 유틸성 메소드 ========== //

    @Transactional(readOnly = true)
    @Override
    public User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new Exception404.NoSuchUser(String.format("userId = %d", userId)));
    }

    @Transactional(readOnly = true)
    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new Exception404.NoSuchUser(String.format("email = %s", email)));
    }

    @Transactional(readOnly = true)
    @Override
    public User findLoginUser() {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        User loginUser = findUser(loginUserId);
        return loginUser;
    }
}
