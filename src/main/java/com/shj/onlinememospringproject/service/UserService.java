package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.dto.UserDto;

public interface UserService {
    UserDto.Response findUserProfile();
    void updateUserProfile(UserDto.UpdateRequest updateRequestDto);

    // ========== 유틸성 메소드 ========== //
    User findUser(Long userId);
    User findUserByEmail(String email);
    User findLoginUser();
}
