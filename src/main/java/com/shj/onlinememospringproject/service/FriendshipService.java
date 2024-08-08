package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.dto.FriendshipDto;
import com.shj.onlinememospringproject.dto.UserDto;

import java.util.List;

public interface FriendshipService {
    List<UserDto.Response> findFriends(Integer isFriend);
    void sendFriendship(FriendshipDto.SendRequest sendRequestDto);
    void updateFriendship(FriendshipDto.UpdateRequest updateRequestDto);
    void deleteFriendship(FriendshipDto.DeleteRequest deleteRequestDto);
}
