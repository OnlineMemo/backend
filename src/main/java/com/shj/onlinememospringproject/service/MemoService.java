package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.dto.MemoDto;

import java.util.List;

public interface MemoService {
    MemoDto.Response findMemoInfo(Long memoId);
    List<MemoDto.MemoPageResponse> findMemos(String filter, String search);
    MemoDto.CreateResponse createMemo(MemoDto.CreateRequest createRequestDto);
    void releaseEditLock(Long memoId);
    void checkEditLock(Long memoId);
    void updateMemoFacade(Long memoId, MemoDto.UpdateRequest updateRequestDto);
    void updateMemo(Long memoId, MemoDto.UpdateRequest updateRequestDto);
    void deleteMemo(Long memoId);

    // ========== 유틸성 메소드 ========== //
    Memo findMemo(Long memoId);
}
