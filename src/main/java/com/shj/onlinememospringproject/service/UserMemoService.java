package com.shj.onlinememospringproject.service;

import java.util.List;

public interface UserMemoService {
    void inviteUsersToMemo(Long memoId, List<Long> userIdList);

    // ========== 유틸성 메소드 ========== //
    void checkUserInMemo(Long userId, Long memoId);
    boolean checkGroupMemo(Long memoId);
}
