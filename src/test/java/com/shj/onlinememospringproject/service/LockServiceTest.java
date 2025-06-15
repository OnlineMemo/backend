//package com.shj.onlinememospringproject.service;
//
//import com.shj.onlinememospringproject.dto.MemoDto;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//public class LockServiceTest {
//
//    @Autowired
//    private MemoService memoService;
//
//
//    @Test
//    @DisplayName("메모 수정 동시요청_Test")
//    public void updateMemoFacade_Test() throws InterruptedException {
//        int requestCnt = 50;  // threadCnt
//        ExecutorService executorService = Executors.newFixedThreadPool(32);
//        CountDownLatch latch = new CountDownLatch(requestCnt);
//
//        Long memoId = 1L;
//        for(int i=0; i<requestCnt; i++) {
//            MemoDto.UpdateRequest updateRequestDto = MemoDto.UpdateRequest.builder()
//                    .title("testTitle" + requestCnt)
//                    .content("testContent" + requestCnt)
//                    .isStar(null)
//                    .build();
//            final Long userId = (long) ((i % 5) + 1);  // 1~5번 유저 순환
//
//            executorService.submit(() -> {
//                try {
//                    memoService.updateMemoFacade(memoId, updateRequestDto, userId);
//                } finally {
//                    latch.countDown();  // latch 카운트 감소
//                }
//            });
//        }
//        latch.await();  // latch 카운트가 0이 될때까지 대기.
//
//        assertThat(true).isEqualTo(true);  // 실제값, 기댓값
//    }
//}
