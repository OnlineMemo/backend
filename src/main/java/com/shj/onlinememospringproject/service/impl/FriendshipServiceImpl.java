package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.Friendship;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.enums.FriendshipState;
import com.shj.onlinememospringproject.dto.UserDto;
import com.shj.onlinememospringproject.repository.FriendshipRepository;
import com.shj.onlinememospringproject.service.FriendshipService;
import com.shj.onlinememospringproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final UserService userService;
    private final FriendshipRepository friendshipRepository;


    @Transactional(readOnly = true)
    @Override
    public List<UserDto.Response> findFriends(Integer isFriend) {
        // 친구목록 조회 or 친구요청 수신목록 조회
        final FriendshipState friendshipState = (isFriend == 1) ? FriendshipState.FRIEND : FriendshipState.SEND;

        User user = userService.findLoginUser();  // 현재 로그인 사용자
        List<Friendship> friendshipList = friendshipRepository.findAllByUserAndFriendshipState(user, friendshipState);
        // 위 대신 간단히 밑의 로직을 사용할수도 있으나, N+1 문제 해결을 위해 직접 DB에서 Eager 조회를 하도록함.
        // List<Friendship> friendshipList = user.getReceivefriendshipList();

        return friendshipList.stream()
                .filter(friendship -> friendship.getFriendshipState().equals(friendshipState))  // 친구관계상태 필터링
                .map(friendship -> new UserDto.Response(friendship.getSenderUser()))  // DTO 변환때문에 N+1 접근을 하게됨 (N+1 해결완료)
                .sorted(Comparator.comparing(UserDto.Response::getNickname)  // 정렬 우선순위 1: 이름 오름차순
                        .thenComparing(UserDto.Response::getUserId))  // 정렬 우선순위 2: id 오름차순
                .collect(Collectors.toList());
    }
}
