package com.shj.onlinememospringproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemoScheduler {

    private final MemoService memoService;


    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Seoul")  // 매일 오전 3시에 실행
    public void deleteExpiredTrashMemos() {
        memoService.deleteExpiredTrashMemos();
    }
}
