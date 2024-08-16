package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.Friendship;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.enums.FriendshipState;
import com.shj.onlinememospringproject.dto.FriendshipDto;
import com.shj.onlinememospringproject.dto.UserDto;
import com.shj.onlinememospringproject.repository.FriendshipBatchRepository;
import com.shj.onlinememospringproject.repository.FriendshipRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.response.exception.Exception404;
import com.shj.onlinememospringproject.service.FriendshipService;
import com.shj.onlinememospringproject.service.UserService;
import com.shj.onlinememospringproject.util.SecurityUtil;
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
        if(!(isFriend == 0 || isFriend == 1)) throw new Exception400.FriendshipBadRequest("잘못된 쿼리파라미터로 API를 요청하였습니다.");
        final FriendshipState friendshipState = (isFriend == 1) ? FriendshipState.FRIEND : FriendshipState.SEND;

        // 강제 Eager 조회 (N+1 문제 해결)
        User user = userService.findLoginUser();  // 현재 로그인 사용자 조회
        List<Friendship> friendshipList = friendshipRepository.findAllByUserAndFriendshipStateToSenderUserWithEager(user, friendshipState);
        // List<Friendship> friendshipList = user.getReceivefriendshipList();

        return friendshipList.stream()
                .filter(friendship -> friendship.getFriendshipState().equals(friendshipState))  // 친구관계상태 필터링
                .map(friendship -> new UserDto.Response(friendship.getSenderUser()))  // Friendship.senderUser (DTO 변환으로, N+1 쿼리 발생)
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
            throw new Exception400.FriendshipBadRequest("자기 자신에게 친구요청 할 수 없습니다.");
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
    public void updateFriendship(Long friendId, FriendshipDto.UpdateRequest updateRequestDto) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();  // 현재 로그인 사용자
        Long updateUserId = friendId;  // 친구상태를 업데이트할 사용자

        // 어차피 자식 테이블인 Friendship보다 부모 테이블인 User를 먼저 삭제하는건 불가능하기에, 굳이 따로 User 엔티티들을 조회해보는 과정없이 바로 Friend 엔티티를 조회해도 무관함.
        Friendship friendship = findFriendshipWithId(loginUserId, updateUserId, null);

        if(updateRequestDto.getIsAccept() == 1) {  // 친구요청 수락일 경우
            friendship.updateFriendshipState(FriendshipState.FRIEND);

            // 반대의 경우도 친구관계 상태를 신규 생성 + FRIEND 상태 적용
            Friendship reverseFriendship = Friendship.FriendshipSaveBuilder()
                    .user(friendship.getSenderUser())
                    .senderUser(friendship.getUser())
                    .build();
            reverseFriendship.updateFriendshipState(FriendshipState.FRIEND);
            friendshipRepository.save(reverseFriendship);
        }
        else if(updateRequestDto.getIsAccept() == 0) {  // 친구요청 거절일 경우
            friendshipRepository.delete(friendship);  // 친구요청 관계 자체를 삭제시킴. 이는 요청만 온 상태에서 삭제이므로, 반대의 경우는 삭제하지 않아도 됨.
        }
        else {  // 잘못된 친구관계 수정 요청일 경우
            throw new Exception400.FriendshipBadRequest("잘못된 필드값으로 API를 요청하였습니다.");
        }
    }

    @Transactional
    @Override
    public void deleteFriendship(Long friendId) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();  // 현재 로그인 사용자
        Long deleteUserId = friendId;  // 친구 삭제할 사용자

        // 어차피 자식 테이블인 Friendship보다 부모 테이블인 User를 먼저 삭제하는건 불가능하기에, 굳이 따로 User 엔티티들을 조회해보는 과정없이 바로 Friend 엔티티를 조회해도 무관함.
        Friendship friendship1 = findFriendshipWithId(loginUserId, deleteUserId, FriendshipState.FRIEND);  // 삭제 가능한 친구상태가 아님.
        Friendship friendship2 = findFriendshipWithId(deleteUserId, loginUserId, FriendshipState.FRIEND);  // 삭제 가능한 친구상태가 아님.

        friendshipBatchRepository.batchDelete(Arrays.asList(friendship1, friendship2));  // Friendships - Batch Delete
    }


    // ========== 유틸성 메소드 ========== //

    @Transactional(readOnly = true)
    @Override
    public Friendship findFriendshipWithId(Long userId, Long senderUserId, FriendshipState friendshipState) {
        if(friendshipState != null) {
            return friendshipRepository.findByUser_IdAndSenderUser_IdAndFriendshipState(userId, senderUserId, friendshipState).orElseThrow(
                    () -> new Exception404.NoSuchFriendship(String.format("userId = %d, senderUserId = %d, friendshipState = %s", userId, senderUserId, friendshipState.name())));
        }
        else {  // if(friendshipState == null)
            return friendshipRepository.findByUser_IdAndSenderUser_Id(userId, senderUserId).orElseThrow(
                    () -> new Exception404.NoSuchFriendship(String.format("userId = %d, senderUserId = %d", userId, senderUserId)));
        }
    }
}
