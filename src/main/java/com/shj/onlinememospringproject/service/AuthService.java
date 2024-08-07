package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.dto.AuthDto;

public interface AuthService {
    void signup(AuthDto.SignupRequest signupRequestDto);
    AuthDto.TokenResponse login(AuthDto.LoginRequest loginRequestDto);
    void updatePassword(AuthDto.UpdateRequest updateRequestDto);
}
