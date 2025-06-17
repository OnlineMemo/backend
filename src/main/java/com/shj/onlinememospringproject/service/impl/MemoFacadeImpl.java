package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.dto.MemoDto;
import com.shj.onlinememospringproject.repository.RedisRepository;
import com.shj.onlinememospringproject.response.exception.Exception404;
import com.shj.onlinememospringproject.response.exception.Exception409;
import com.shj.onlinememospringproject.response.exception.Exception423;
import com.shj.onlinememospringproject.service.MemoFacade;
import com.shj.onlinememospringproject.service.MemoService;
import com.shj.onlinememospringproject.service.UserMemoService;
import com.shj.onlinememospringproject.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class MemoFacadeImpl implements MemoFacade {

    private final MemoService memoService;
    private final UserMemoService userMemoService;
    private final RedisRepository redisRepository;


    // < '낙관적 락 (Optimistic Lock)' 기반의 퍼사드 메소드 >
    @Transactional
    @Override
    public void updateMemoFacade(Long memoId, MemoDto.UpdateRequest updateRequestDto) {  // 메모의 제목 또는 내용을 수정할 경우에 호출하는 메소드
        try {
            String lockKey = "memoId:" + memoId;
            Long loginUserId = SecurityUtil.getCurrentMemberId();

            boolean isGroupMemo = userMemoService.checkGroupMemo(memoId);  // 공동메모 여부 체킹. (개인메모라면 락 제어는 불필요하므로 리소스 낭비를 방지하기위함.)
            if(isGroupMemo == true) {
                // 공동메모를 수정중인 다른 사용자가 없다면(키가 존재하지않을때), 굳이 수정을 막을 필요가 없으므로 파라미터에 false를 전달.
                memoService.checkOwnLock(lockKey, loginUserId, false);  // 사용자의 락 접근권한 체킹. (메모의 즐겨찾기 수정과는 무관함.)
            }

            // 메모 수정 비즈니스 로직
            memoService.updateMemo(memoId, updateRequestDto);

            // - 현재 구조에서는 updateMemo()가 updateMemoFacade() 메소드의 트랜잭션 범위 안에서 실행되므로,
            // updateMemo() 종료 시점에는 트랜잭션이 커밋되지 않음. 실제 DB 업데이트 커밋은 updateMemoFacade() 종료 시 발생함.
            // - 이에 TransactionSynchronizationManager.registerSynchronization()을 사용하면,
            // 트랜잭션이 성공적으로 커밋된 직후에 afterCommit() 콜백이 실행되게 구성할 수 있음.
            // 따라서 이 콜백 안에서 Redis 편집락 해제를 수행하게 하면, 메모 수정이 DB에 완전히 반영된 이후에 락 해제가 이뤄지므로 데이터 정합성을 보장할 수 있음.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if(isGroupMemo == true) {
                        redisRepository.unlockOwner(lockKey, loginUserId);
                    }
                }
            });
        } catch (Exception404.NoSuchMemo ex404) {
            throw ex404;
        } catch (Exception423.LockedData ex423) {
            throw ex423;
        } catch (Exception ex) {
            throw new Exception409.ConflictData();
        }
    }

    // < 'Redis 분산 락 (Lettuce Lock)' 기반의 퍼사드 메소드 >
    // ==> 위의 '낙관적 락 (Optimistic Lock)' 방식이 더 적합하다고 판단되어 비활성화 처리함.
//    @Transactional
//    @Override
//    public void updateMemoFacade(Long memoId, MemoDto.UpdateRequest updateRequestDto) {
//        Long loginUserId = SecurityUtil.getCurrentMemberId();
//        userMemoService.checkUserInMemo(loginUserId, memoId);  // 사용자의 메모 접근권한 체킹.
//
//        String loginUserNickname = userRepository.findNicknameById(loginUserId);
//        StringBuilder lockValueStb = new StringBuilder();
//        lockValueStb.append("userId:").append(loginUserId).append(",").append("userNickname:").append(loginUserNickname);
//
//        String lockKey = "memoId:" + memoId;
//        String lockValue = lockValueStb.toString();
//        Long lockTTL = 1000L * 5;  // TTL 5초
//
//        String value = redisRepository.getValue(lockKey);
//        if(value != null) {  // Redis에 해당 메모의 락이 이미 존재한다면
//            Object[] userInfo = memoService.parseUserInfo(value);
//            Long lockUserId = (Long) userInfo[0];
//            String lockUserNickname = (String) userInfo[1];
//
//            boolean isOwnLock = (lockUserId != null && lockUserId.equals(loginUserId));
//            int retryCnt = 0, limitRetryCnt = 3;
//            if(isOwnLock == false) {  // 존재하는 락이 자신의 것이 아니라면, 지정한 최대횟수까지 재시도.
//                while(!redisRepository.lock(lockKey, lockValue, lockTTL)) {
//                    if(++retryCnt >= limitRetryCnt) {
//                        throw new Exception423.LockedData(lockUserNickname);
//                    }
//
//                    try {
//                        Thread.sleep(200);  // Redis의 부하를 줄이기 위해, Spin Lock 사이사이에 텀을 주어야함.
//                    } catch (InterruptedException iex) {
//                        Thread.currentThread().interrupt();  // 현재 스레드의 인터럽트 상태를 복원.
//                        throw new RuntimeException(iex);
//                    }
//                }
//            }
//        }
//
//        try {
//            memoService.updateMemo(memoId, updateRequestDto);
//        } finally {
//            redisRepository.unlockOwner(lockKey, loginUserId);
//        }
//    }
}
