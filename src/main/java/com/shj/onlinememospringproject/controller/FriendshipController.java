package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.dto.FriendshipDto;
import com.shj.onlinememospringproject.dto.UserDto;
import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import com.shj.onlinememospringproject.service.FriendshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Friendship")
@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;


    @GetMapping
    @Operation(summary = "친구/수신 목록 조회 [JWT O]", description = "URI : /friends?isFriend={0 or 1}")
    public ResponseEntity<ResponseData<List<UserDto.Response>>> findFriends(@RequestParam(value = "isFriend", required = true) Integer isFriend) {
        List<UserDto.Response> userResponseDtoList = friendshipService.findFriends(isFriend);
        return ResponseData.toResponseEntity(ResponseCode.READ_FRIENDLIST, userResponseDtoList);
    }

    @PostMapping
    @Operation(summary = "친구요청 생성 [JWT O]")
    public ResponseEntity<ResponseData> sendFriendship(@RequestBody FriendshipDto.SendRequest sendRequestDto) {
        friendshipService.sendFriendship(sendRequestDto);
        return ResponseData.toResponseEntity(ResponseCode.CREATED_SENDFRIENDSHIP);
    }

    @PutMapping("/{friendId}")  // "/{친구 userId}"
    @Operation(summary = "친구요청 수락/거절 [JWT O]", description = "- isAccept 필드 : 0(거절) or 1(수락)")
    public ResponseEntity<ResponseData> updateFriendship(@PathVariable(value = "friendId") Long friendId, @RequestBody FriendshipDto.UpdateRequest updateRequestDto) {
        friendshipService.updateFriendship(friendId, updateRequestDto);
        return ResponseData.toResponseEntity(ResponseCode.UPDATE_FRIENDSHIP);
    }

    @DeleteMapping("/{friendId}")  // "/{친구 userId}"
    @Operation(summary = "친구 삭제 [JWT O]")
    public ResponseEntity<ResponseData> deleteFriendship(@PathVariable(value = "friendId") Long friendId) {
        friendshipService.deleteFriendship(friendId);
        return ResponseData.toResponseEntity(ResponseCode.DELETE_FRIENDSHIP);
    }
}
