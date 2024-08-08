package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.dto.UserDto;

import java.util.List;

public interface FriendshipService {
    List<UserDto.Response> findFriends(Integer isFriend);
}
