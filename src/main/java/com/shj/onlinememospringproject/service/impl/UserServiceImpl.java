package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.dto.UserDto;
import com.shj.onlinememospringproject.repository.UserRepository;
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


    @Transactional(readOnly = true)
    @Override
    public User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new Exception404.NoSuchUser(String.format("userId = %d", userId)));
    }

    @Transactional(readOnly = true)
    @Override
    public User findLoginUser() {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        User loginUser = findUser(loginUserId);
        return loginUser;
    }

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
}
