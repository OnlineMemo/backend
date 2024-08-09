package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.Friendship;
import com.shj.onlinememospringproject.domain.enums.FriendshipState;
import com.shj.onlinememospringproject.dto.FriendshipDto;
import com.shj.onlinememospringproject.dto.UserDto;

import java.util.List;

public interface FriendshipService {
    List<UserDto.Response> findFriends(Integer isFriend);
    void sendFriendship(FriendshipDto.SendRequest sendRequestDto);
    void updateFriendship(FriendshipDto.UpdateRequest updateRequestDto);
    void deleteFriendship(FriendshipDto.DeleteRequest deleteRequestDto);

    // ========== 유틸성 메소드 ========== //
    Friendship findFriendshipWithId(Long userId, Long senderUserId, FriendshipState friendshipState);
}
