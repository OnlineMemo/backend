package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.dto.UserDto;
import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import com.shj.onlinememospringproject.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;


    @GetMapping
    @Operation(summary = "회원정보 조회 [JWT O]")
    public ResponseEntity<ResponseData<UserDto.Response>> findUserProfile() {
        UserDto.Response userResponseDto = userService.findUserProfile();
        return ResponseData.toResponseEntity(ResponseCode.READ_USER, userResponseDto);
    }

    @PutMapping
    @Operation(summary = "회원정보 수정 [JWT O]")
    public ResponseEntity<ResponseData> update(@RequestBody UserDto.UpdateRequest updateRequestDto) {
        userService.updateUserProfile(updateRequestDto);
        return ResponseData.toResponseEntity(ResponseCode.UPDATE_USER);
    }
}
