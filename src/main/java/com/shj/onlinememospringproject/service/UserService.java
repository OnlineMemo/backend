package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.dto.UserDto;

public interface UserService {
    User findUser(Long userId);
    User findLoginUser();
    UserDto.Response findUserProfile();
    void updateUserProfile(UserDto.UpdateRequest updateRequestDto);
}
