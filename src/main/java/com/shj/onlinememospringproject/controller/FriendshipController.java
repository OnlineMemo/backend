package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.dto.UserDto;
import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import com.shj.onlinememospringproject.service.FriendshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Friendship")
@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;


    @GetMapping("/")
    @Operation(summary = "친구/수신 목록 조회 [JWT O]", description = "URI : /friends?isFriend={0 or 1}")
    public ResponseEntity<ResponseData<List<UserDto.Response>>> findFriends(@RequestParam(value = "isFriend", required = true) Integer isFriend) {
        List<UserDto.Response> userResponseDtoList = friendshipService.findFriends(isFriend);
        return ResponseData.toResponseEntity(ResponseCode.READ_FRIENDLIST, userResponseDtoList);
    }
}
