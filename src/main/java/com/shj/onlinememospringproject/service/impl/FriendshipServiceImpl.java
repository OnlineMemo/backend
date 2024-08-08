package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.Friendship;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.enums.FriendshipState;
import com.shj.onlinememospringproject.dto.FriendshipDto;
import com.shj.onlinememospringproject.dto.UserDto;
import com.shj.onlinememospringproject.repository.FriendshipBatchRepository;
import com.shj.onlinememospringproject.repository.FriendshipRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.service.FriendshipService;
import com.shj.onlinememospringproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final UserService userService;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipBatchRepository friendshipBatchRepository;


    @Transactional(readOnly = true)
    @Override
    public List<UserDto.Response> findFriends(Integer isFriend) {
        // 친구목록 조회 or 친구요청 수신목록 조회
        final FriendshipState friendshipState = (isFriend == 1) ? FriendshipState.FRIEND : FriendshipState.SEND;

        User user = userService.findLoginUser();  // 현재 로그인 사용자 조회
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

    @Transactional
    @Override
    public void sendFriendship(FriendshipDto.SendRequest sendRequestDto) {
        User loginUser = userService.findLoginUser();  // 현재 로그인 사용자 조회
        User user = userService.findUserByEmail(sendRequestDto.getEmail());  // 내가 친구요청을 보낼 사용자 조회

        if(loginUser.getId() == user.getId()) {  // 자신이 자신에게 친구요청한 경우라면,
            throw new Exception400.FriendshipBadRequest("자기 자신에게 친구요청은 할 수 없습니다.");
        }

        if(friendshipRepository.existsByUserAndSenderUser(user, loginUser)) {  // 이미 DB에 존재하는 친구요청일 경우라면,
            throw new Exception400.FriendshipBadRequest("이미 친구요청을 보냈거나 친구인 상태입니다.");
        }
        else if(friendshipRepository.existsByUserAndSenderUser(loginUser, user)) {  // 또는 반대로 이미 친구요청을 받은상태인데 친구요청한 경우라면,
            throw new Exception400.FriendshipBadRequest("상대방이 먼저 친구요청을 보냈습니다.");
        }

        Friendship friendship = Friendship.FriendshipSaveBuilder()
                .user(user)
                .senderUser(loginUser)
                .build();
        friendshipRepository.save(friendship);
    }

    @Transactional
    @Override
    public void updateFriendship(FriendshipDto.UpdateRequest updateRequestDto) {

    }

    @Transactional
    @Override
    public void deleteFriendship(FriendshipDto.DeleteRequest deleteRequestDto) {
        User loginUser = userService.findLoginUser();  // 현재 로그인 사용자
        User deleteUser = userService.findUser(deleteRequestDto.getUserId());  // 친구 삭제할 사용자
        FriendshipState friendshipState = FriendshipState.FRIEND;

        Friendship friendship1 = friendshipRepository.findByUserAndSenderUserFriendshipState(loginUser, deleteUser, friendshipState).orElseThrow(
                () -> new Exception400.FriendshipBadRequest("삭제 가능한 친구상태가 아닙니다."));
        Friendship friendship2 = friendshipRepository.findByUserAndSenderUserFriendshipState(deleteUser, loginUser, friendshipState).orElseThrow(
                () -> new Exception400.FriendshipBadRequest("삭제 가능한 친구상태가 아닙니다."));

        friendshipBatchRepository.batchDelete(Arrays.asList(friendship1, friendship2));
    }
}
