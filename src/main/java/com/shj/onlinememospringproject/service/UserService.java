package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.User;

public interface UserService {
    User findUser(Long userId);
    User findLoginUser();
}
