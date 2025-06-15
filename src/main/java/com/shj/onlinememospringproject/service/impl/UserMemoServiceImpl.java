package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import com.shj.onlinememospringproject.repository.MemoRepository;
import com.shj.onlinememospringproject.repository.UserMemoBatchRepository;
import com.shj.onlinememospringproject.repository.UserMemoRepository;
import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.response.exception.Exception404;
import com.shj.onlinememospringproject.service.UserMemoService;
import com.shj.onlinememospringproject.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMemoServiceImpl implements UserMemoService {

    private final UserRepository userRepository;
    private final MemoRepository memoRepository;
    private final UserMemoRepository userMemoRepository;
    private final UserMemoBatchRepository userMemoBatchRepository;


    @Transactional
    @Override
    public void inviteUsersToMemo(Long memoId, List<Long> userIdList) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        checkUserInMemo(loginUserId, memoId);  // 애초에 초대할 권한(메모에 포함되지않은 사용자인 경우)이 없는 경우는 아닌지 체킹.

        // 강제 Eager 조회 (N+1 문제 해결)
        Memo memo = memoRepository.findByIdToUserWithEager(memoId).orElseThrow(
                () -> new Exception404.NoSuchUser(String.format("memoId = %d", memoId)));

        // 공동메모에 이미 참여중인 사용자들
        Set<Long> existUserIdSet = memo.getUserMemoList().stream()  // Memo.userMemoList (N+1 쿼리 발생)
                .map(userMemo -> userMemo.getUser().getId())  // Memo.userMemoList.user (N+1 쿼리 발생)
                .collect(Collectors.toSet());

        // 초대할 신규 사용자들
        for(Long userId : userIdList) {
            if(existUserIdSet.contains(userId)) {  // 이미 DB에 존재하는 사용자와 메모 관계일 경우
                throw new Exception400.UserMemoDuplicate(String.format("userId = %d, memoId = %d", userId, memoId));
            }
        }
        List<User> userList = userRepository.findByIdIn(userIdList);  // 한 번의 DB 호출로 Users 정보 조회

        // 즐겨찾기 해제
        memoRepository.updateIsStar(memoId, 0);

        List<UserMemo> userMemoList = userList.stream()
                .map(user -> UserMemo.UserMemoSaveBuilder()
                        .user(user)
                        .memo(memo)
                        .build())
                .collect(Collectors.toList());
        userMemoBatchRepository.batchInsert(userMemoList);  // UserMemos - Batch Insert
    }


    // ========== 유틸성 메소드 ========== //

    // 해당 메모에 관련한 수정/삭제 권한이 있는지 체킹하는 메소드 (메모에 포함되지않은 사용자인지 체킹함.)
    @Transactional(readOnly = true)
    @Override
    public void checkUserInMemo(Long userId, Long memoId) {  // DI된 userMemoRepository 의존성 인스턴스 변수를 사용하므로, static으로는 선언하지 않는것이 권장됨.
        if(!userMemoRepository.existsByUser_IdAndMemo_Id(userId, memoId)) {  // 애초에 초대할 권한(메모에 포함되지않은 사용자인 경우)이 없는 경우
            throw new Exception404.NoSuchUserMemo(String.format("userId = %d, memoId = %d", userId, memoId));
        }
    }

    // 해당 메모가 공동메모인지 체킹하는 메소드
    @Transactional(readOnly = true)
    @Override
    public boolean checkGroupMemo(Long memoId) {
        boolean isGroupMemo = (userMemoRepository.isGroupMemoByMemoId(memoId) != 0);
        return isGroupMemo;
    }
}
