package com.shj.onlinememospringproject.service;

import java.util.List;

public interface UserMemoService {
    void inviteUsersToMemo(Long memoId, List<Long> userIdList);
}
