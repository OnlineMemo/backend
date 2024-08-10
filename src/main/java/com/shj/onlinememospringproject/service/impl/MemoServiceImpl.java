package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import com.shj.onlinememospringproject.dto.MemoDto;
import com.shj.onlinememospringproject.repository.MemoRepository;
import com.shj.onlinememospringproject.repository.UserMemoRepository;
import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.response.exception.Exception404;
import com.shj.onlinememospringproject.service.MemoService;
import com.shj.onlinememospringproject.service.UserMemoService;
import com.shj.onlinememospringproject.service.UserService;
import com.shj.onlinememospringproject.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoServiceImpl implements MemoService {

    private final UserService userService;
    private final UserMemoService userMemoService;
    private final UserRepository userRepository;
    private final MemoRepository memoRepository;
    private final UserMemoRepository userMemoRepository;


    @Transactional(readOnly = true)
    @Override
    public MemoDto.Response findMemoInfo(Long memoId) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        userMemoService.checkUserInMemo(loginUserId, memoId);  // 사용자의 메모 접근권한 체킹.

        // 강제 Eager 조회 (N+1 문제 해결)
        UserMemo userMemo = userMemoRepository.findByUser_IdAndMemo_IdToMemoWithEager(loginUserId, memoId).orElseThrow(
                () -> new Exception404.NoSuchUserMemo(String.format("userId = %d, memoId = %d", loginUserId, memoId)));  // 로그인사용자id와 메모의 사용자id 불일치 에러

        Memo memo = userMemo.getMemo();
        MemoDto.Response memoResponseDto = new MemoDto.Response(memo);  // Usermemo.memo (DTO 변환으로, N+1 쿼리 발생)
        return memoResponseDto;
    }

    @Transactional(readOnly = true)
    @Override
    public List<MemoDto.MemoPageResponse> findMemos(String filter, String search) {
        if(filter != null && search != null) throw new Exception400.MemoBadRequest("잘못된 쿼리파라미터로 API를 요청하였습니다.");  // 정렬과 검색중 하나만 적용 가능.
        Predicate<Memo> memoPredicate = (filter != null) ? filterMemos(filter) : searchMemos(search);

        // 강제 Eager 조회 (N+1 문제 해결)
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        User user = userRepository.findByIdToDeepUserWithEager(loginUserId).orElseThrow(
                () -> new Exception404.NoSuchUser(String.format("userId = %d", loginUserId)));

        List<MemoDto.MemoPageResponse> memoPageResponseDtoList = user.getUserMemoList().stream()
                .map(UserMemo::getMemo)
                .filter(memoPredicate)  // User.userMemoList.memo (N+1 쿼리 발생)
                .sorted(Comparator.comparing(Memo::getModifiedTime).reversed()  // 정렬 우선순위 1: 수정날짜 내림차순
                        .thenComparing(Memo::getId).reversed())  // 정렬 우선순위 2: id 내림차순
                .map(MemoDto.MemoPageResponse::new)  // User.userMemoList.memo.userMemoList & User.userMemoList.memo.userMemoList.user (내부에서 N+1 쿼리 발생)
                .collect(Collectors.toList());

        return memoPageResponseDtoList;
    }

    @Transactional
    @Override
    public void createMemo(MemoDto.CreateRequest createRequestDto) {
        User loginUser = userService.findLoginUser();
        Memo memo = Memo.MemoSaveBuilder()
                .title(createRequestDto.getTitle())
                .content(createRequestDto.getContent())
                .build();
        Long memoId = memoRepository.save(memo).getId();

        UserMemo newUserMemo = UserMemo.UserMemoSaveBuilder()
                .user(loginUser)
                .memo(memo)
                .build();
        userMemoRepository.save(newUserMemo);

        // 공동메모 생성시 (초대로직 추가실행)
        if(!(createRequestDto.getUserIdList() == null || createRequestDto.getUserIdList().isEmpty())) {  // 개인메모가 아닐때
            userMemoService.inviteUsersToMemo(memoId, createRequestDto.getUserIdList());
        }
    }

    @Transactional
    @Override
    public void updateMemo(Long memoId, MemoDto.UpdateRequest updateRequestDto) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        userMemoService.checkUserInMemo(loginUserId, memoId);  // 사용자의 메모 접근권한 체킹.

        if(updateRequestDto.getIsStar() != null) {  // 메모의 즐겨찾기 여부 수정인 경우
            memoRepository.updateIsStar(memoId, updateRequestDto.getIsStar());
            return;  // 바로 함수 종료.
        }

        // 즐겨찾기 수정이 아닌, 메모의 제목과 내용 수정인 경우
        Memo memo = findMemo(memoId);
        memo.updateTitle(updateRequestDto.getTitle());
        memo.updateContent(updateRequestDto.getContent());
    }

    @Transactional
    @Override
    public void deleteMemo(Long memoId) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        userMemoService.checkUserInMemo(loginUserId, memoId);  // 메모를 삭제/탈퇴할 권한이 있는지 체킹.

        // 강제 Eager 조회 (N+1 문제 해결)
        Memo memo = memoRepository.findByIdToUserMemoListWithEager(memoId).orElseThrow(
                () -> new Exception404.NoSuchUser(String.format("memoId = %d", memoId)));
        int memoHasUsersCount = memo.getUserMemoList().size();  // Memo.userMemoList (리스트의 size 측정으로, N+1 쿼리 발생)

        // 공동메모 그룹 탈퇴 처리. (자식 테이블인 UserMemo에서 먼저 삭제.)
        userMemoRepository.deleteByUser_IdAndMemo_Id(loginUserId, memoId);

        if(memoHasUsersCount == 1) {  // 해당 메모가 개인메모라면
            memoRepository.delete(memo);  // 그 이후에 부모 테이블인 Memo에서 해당 메모를 삭제. (이후 부모 테이블인 Memo에서 삭제.)
        }
        else if(memoHasUsersCount == 2) {  // 공동메모인데, 그룹 탈퇴로 메모의 사용자가 2명에서 1명으로 개인메모가 될 경우 (즉, 원래 2명이었을 경우)
            // 즐겨찾기 여부를 다시 0으로 초기화.
            memoRepository.updateIsStar(memoId, 0);  // isStar 필드는 수정시각에 영향을 주지않도록, @LastModifiedDate 생명주기에서 제외시켜 따로 JPQL로 직접 업데이트함.
        }
    }


    // ========== 유틸성 메소드 ========== //

    @Transactional(readOnly = true)
    @Override
    public Memo findMemo(Long memoId) {
        return memoRepository.findById(memoId).orElseThrow(
                () -> new Exception404.NoSuchMemo(String.format("memoId = %d", memoId)));
    }

    private static Predicate<Memo> filterMemos(String filter) {
        if(filter == null) return memo -> true;
        Predicate<Memo> predicate = switch (filter) {
            case "private-memo" -> memo -> memo.getUserMemoList().size() == 1;
            case "group-memo" -> memo -> memo.getUserMemoList().size() > 1;
            case "star-memo" -> memo -> memo.getIsStar() == 1;
            default -> throw new Exception400.MemoBadRequest("잘못된 쿼리파라미터로 API를 요청하였습니다.");
        };
        return predicate;
    }

    private static Predicate<Memo> searchMemos(String search) {
        if(search == null) return memo -> true;
        return memo -> memo.getTitle().contains(search) || memo.getContent().contains(search);
    }
}
