package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import com.shj.onlinememospringproject.dto.MemoDto;
import com.shj.onlinememospringproject.repository.MemoRepository;
import com.shj.onlinememospringproject.repository.UserMemoRepository;
import com.shj.onlinememospringproject.response.exception.Exception404;
import com.shj.onlinememospringproject.service.MemoService;
import com.shj.onlinememospringproject.service.UserMemoService;
import com.shj.onlinememospringproject.service.UserService;
import com.shj.onlinememospringproject.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemoServiceImpl implements MemoService {

    private final UserService userService;
    private final UserMemoService userMemoService;
    private final MemoRepository memoRepository;
    private final UserMemoRepository userMemoRepository;


    @Transactional(readOnly = true)
    @Override
    public Memo findMemo(Long memoId) {
        return memoRepository.findById(memoId).orElseThrow(
                () -> new Exception404.NoSuchMemo(String.format("memoId = %d", memoId)));
    }

    @Transactional(readOnly = true)
    @Override
    public MemoDto.Response findMemoInfo(Long memoId) {
        // 강제 Eager 조회 (N+1 문제 해결)
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        UserMemo userMemo = userMemoRepository.findByUser_IdAndMemo_IdWithEager(loginUserId, memoId).orElseThrow(
                () -> new Exception404.NoSuchUserMemo(String.format("userId = %d, memoId = %d", loginUserId, memoId)));  // 로그인사용자id와 메모의 사용자id 불일치 에러

        Memo memo = userMemo.getMemo();
        MemoDto.Response memoResponseDto = new MemoDto.Response(memo);  // Usermemo.memo (DTO 변환으로, N+1 쿼리 발생)
        return memoResponseDto;
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
}
