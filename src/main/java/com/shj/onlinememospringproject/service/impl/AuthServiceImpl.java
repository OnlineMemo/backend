package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.dto.AuthDto;
import com.shj.onlinememospringproject.jwt.TokenProvider;
import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.service.AuthService;
import com.shj.onlinememospringproject.service.UserService;
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

    private final UserService userService;
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
                .password(toEncodePassword(signupRequestDto.getPassword()))
                .nickname(signupRequestDto.getNickname())
                .build();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public AuthDto.TokenResponse login(AuthDto.LoginRequest loginRequestDto) {
        UsernamePasswordAuthenticationToken authenticationToken = toAuthentication(loginRequestDto.getEmail(), loginRequestDto.getPassword());
        Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);  // 아이디와 비밀번호가 일치하는지 검증.

        return tokenProvider.generateTokenDto(authentication);  // 로그인 성공. JWT 토큰 생성.
    }

    @Transactional
    @Override
    public void updatePassword(AuthDto.UpdateRequest updateRequestDto) {
        UsernamePasswordAuthenticationToken authenticationToken = toAuthentication(updateRequestDto.getEmail(), updateRequestDto.getPassword());
        Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);  // 로그인이 가능한 계정이 맞는지 검증.

        User user = userService.findUser(Long.valueOf(authentication.getName()));
        user.updatePassword(toEncodePassword(updateRequestDto.getNewPassword()));
    }


    // ========== 유틸성 메소드 ========== //

    private String toEncodePassword(String password) {  // DI된 passwordEncoder 의존성 인스턴스 변수를 사용하므로, static으로는 선언할 수 없음.
        return passwordEncoder.encode(password);
    }

    private static UsernamePasswordAuthenticationToken toAuthentication(String email, String password) {  // 반환된 객체로 아이디와 비밀번호가 일치하는지 검증하는 로직에 활용이 가능함.
        return new UsernamePasswordAuthenticationToken(email, password);
    }
}
