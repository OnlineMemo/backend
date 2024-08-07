package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.dto.AuthDto;
import com.shj.onlinememospringproject.jwt.TokenProvider;
import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder managerBuilder;


    @Transactional
    @Override
    public void signup(AuthDto.SignupRequest signupRequestDto) {
        String newEmail = signupRequestDto.getEmail();
        userRepository.findByEmail(newEmail)
                .ifPresent(user -> { throw new Exception400.EmailDuplicate(newEmail); });  // 회원가입 로그인아이디 중복 예외처리.

        User user = User.UserSaveBuilder()
                .email(signupRequestDto.getEmail())
                .password(passwordEncoder.encode(signupRequestDto.getPassword()))
                .nickname(signupRequestDto.getNickname())
                .build();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public AuthDto.TokenResponse login(AuthDto.LoginRequest loginRequestDto) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword());
        Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);  // 여기서 실제로 아이디와 비밀번호가 일치하는지 검증이 이루어짐.
        return tokenProvider.generateTokenDto(authentication);  // 로그인 성공. JWT 토큰 생성.
    }
}
