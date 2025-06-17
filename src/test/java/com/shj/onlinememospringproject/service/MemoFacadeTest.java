package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.dto.MemoDto;
import com.shj.onlinememospringproject.repository.MemoRepository;
import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.response.exception.Exception409;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest
public class MemoFacadeTest {

    @Autowired
    private MemoFacade memoFacade;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MemoRepository memoRepository;

    // 테스트용 데이터
    private final Long MEMO_ID = 1L;
    private final List<Long> USER_ID_LIST = List.of(1L, 2L, 3L, 4L, 5L);

    // 테스트용 데이터가 실제 DB에 있음을 검증하고, 메모 내용을 초기화한 후 시작.
    @BeforeEach
    void setup() {
        // DB 데이터 존재여부 검증
        Optional<Memo> optionalMemo = memoRepository.findById(MEMO_ID);
        assertThat(optionalMemo).isPresent()
                .as("404 TEST ERROR - 'MEMO_ID = %d' 메모가 DB에 존재해야 합니다.", MEMO_ID);
        USER_ID_LIST.forEach(userId -> {
            assertThat(userRepository.findById(userId)).isPresent()
                    .as("404 TEST ERROR - 'USER_ID = %d' 사용자가 DB에 존재해야 합니다.", userId);
        });

        // 메모 내용 초기화
        Memo memo = optionalMemo.get();
        memo.updateTitle("testTitle init");
        memo.updateContent("testContent init");
        memoRepository.flush();
    }


    // @Test
    @DisplayName("동시 수정 Test - 낙관적 락 충돌 감지 확인")
    public void updateMemoFacade_Test() throws InterruptedException {
        int threadCnt = 5;  // requestCnt
        ExecutorService executorService = Executors.newFixedThreadPool(threadCnt);
        CountDownLatch latch = new CountDownLatch(threadCnt);

        List<Exception> exception409List = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptionOtherList = Collections.synchronizedList(new ArrayList<>());
        for(Long userId : USER_ID_LIST) {
            executorService.submit(() -> {
                try {
                    // 로그인 상태로 만듦
                    setAuthentication(userId);

                    // updateDTO 생성
                    Long currentVersion = memoRepository.findVersionById(MEMO_ID);  // 1차 검증 : 전달받은 메모의 현재 버전과 DB 조회된 버전이 일치하는지 확인 (수동 필드 기반)
                    MemoDto.UpdateRequest updateRequestDto = MemoDto.UpdateRequest.builder()
                            .title("testTitle " + userId)
                            .content("testContent " + userId)
                            .currentVersion(currentVersion)
                            .build();

                    // 낙관적 락 메인로직 실행
                    memoFacade.updateMemoFacade(MEMO_ID, updateRequestDto);  // 2차 검증 : 트랜잭션 커밋 시점에 JPA가 버전 일치 여부로 충돌 판단 (낙관적 락 기반)
                } catch (Exception409.ConflictData ex409) {
                    exception409List.add(ex409);
                } catch (Exception ex) {
                    exceptionOtherList.add(ex);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();  // latch 카운트가 0이 될때까지 대기.

        // 예외처리 결과 디버깅용
        exception409List.forEach(ex409 -> System.out.println("409 TEST ERROR - 낙관적 락 충돌 발생"));
        exceptionOtherList.forEach(ex -> System.out.println("500 TEST ERROR - " + ex.getMessage()));

        // 검증 - 낙관적 락 예외가 최소 1번이라도 발생했는가?
        assertThat(exception409List.size() > 0)
                .as("(1)검증 실패 - 낙관적 락 충돌이 감지되지 않았습니다.")
                .isTrue();  // 실제값, 기댓값
        // 검증 - 기타 예외는 발생하지 않았는가?
        assertThat(exceptionOtherList.size() == 0)
                .as("(2)검증 실패 - 의도 외의 예외가 발생했습니다.")
                .isTrue();  // 실제값, 기댓값
    }


    // ========== 유틸성 메소드 ========== //

    private void setAuthentication(Long userId) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
