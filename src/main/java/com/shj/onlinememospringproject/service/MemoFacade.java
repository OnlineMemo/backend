package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.dto.MemoDto;

public interface MemoFacade {
    void updateMemoFacade(Long memoId, MemoDto.UpdateRequest updateRequestDto);
}
