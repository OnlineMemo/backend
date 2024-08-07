package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.dto.AuthDto;
import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import com.shj.onlinememospringproject.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/signup")
    @Operation(summary = "회원가입 [JWT X]")
    public ResponseEntity<ResponseData> signup(@RequestBody AuthDto.SignupRequest signupRequestDto) {
        authService.signup(signupRequestDto);
        return ResponseData.toResponseEntity(ResponseCode.CREATED_USER);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인 [JWT X]")
    public ResponseEntity<ResponseData<AuthDto.TokenResponse>> login(@RequestBody AuthDto.LoginRequest loginRequestDto) {
        AuthDto.TokenResponse tokenResponseDto = authService.login(loginRequestDto);
        return ResponseData.toResponseEntity(ResponseCode.LOGIN_SUCCESS, tokenResponseDto);
    }

    @PutMapping("/password")
    @Operation(summary = "비밀번호 변경 [JWT X]")
    public ResponseEntity<ResponseData> updatePassword(@RequestBody AuthDto.UpdateRequest updateRequestDto) {
        authService.updatePassword(updateRequestDto);
        return ResponseData.toResponseEntity(ResponseCode.UPDATE_PASSWORD);
    }
}
