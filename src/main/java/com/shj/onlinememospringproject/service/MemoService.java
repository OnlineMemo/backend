package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.dto.MemoDto;

public interface MemoService {
    Memo findMemo(Long memoId);
    MemoDto.Response findMemoInfo(Long memoId);
}
