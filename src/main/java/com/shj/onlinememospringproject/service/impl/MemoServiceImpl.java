package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import com.shj.onlinememospringproject.dto.MemoDto;
import com.shj.onlinememospringproject.repository.MemoRepository;
import com.shj.onlinememospringproject.repository.RedisRepository;
import com.shj.onlinememospringproject.repository.UserMemoRepository;
import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.response.exception.Exception404;
import com.shj.onlinememospringproject.response.exception.Exception409;
import com.shj.onlinememospringproject.response.exception.Exception423;
import com.shj.onlinememospringproject.service.MemoService;
import com.shj.onlinememospringproject.service.UserMemoService;
import com.shj.onlinememospringproject.service.UserService;
import com.shj.onlinememospringproject.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoServiceImpl implements MemoService {

    private final UserService userService;
    private final UserMemoService userMemoService;
    private final UserRepository userRepository;
    private final MemoRepository memoRepository;
    private final UserMemoRepository userMemoRepository;
    private final RedisRepository redisRepository;


    @Transactional(readOnly = true)
    @Override
    public MemoDto.Response findMemoInfo(Long memoId) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        userMemoService.checkUserInMemo(loginUserId, memoId);  // 사용자의 메모 접근권한 체킹.

        // 강제 Eager 조회 (N+1 문제 해결)
        UserMemo userMemo = userMemoRepository.findByUser_IdAndMemo_IdToUserMemoListWithEager(loginUserId, memoId).orElseThrow(
                () -> new Exception404.NoSuchUserMemo(String.format("userId = %d, memoId = %d", loginUserId, memoId)));  // 로그인사용자id와 메모의 사용자id 불일치 에러

        Memo memo = userMemo.getMemo();
        MemoDto.Response memoResponseDto = new MemoDto.Response(memo);  // Usermemo.memo & Usermemo.memo.userMemoList (DTO 변환 및 List길이 계산으로, N+1 쿼리 발생)
        return memoResponseDto;
    }

    @Transactional(readOnly = true)
    @Override
    public List<MemoDto.MemoPageResponse> findMemos(String filter, String search) {
        if(filter != null && search != null) throw new Exception400.MemoBadRequest("잘못된 쿼리파라미터로 API를 요청하였습니다.");  // 정렬과 검색중 하나만 적용 가능.
        Predicate<Memo> memoPredicate = (filter != null) ? filterMemos(filter) : searchMemos(search);

        // 강제 Eager 조회 (N+1 문제 해결)
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        User user = userRepository.findByIdToDeepUserWithEager(loginUserId).orElseThrow(
                () -> new Exception404.NoSuchUser(String.format("userId = %d", loginUserId)));

        List<MemoDto.MemoPageResponse> memoPageResponseDtoList = user.getUserMemoList().stream()
                .map(UserMemo::getMemo)  // User.userMemoList (N+1 쿼리 발생)
                .filter(memoPredicate)  // User.userMemoList.memo (N+1 쿼리 발생)
                .sorted(Comparator.comparing(Memo::getModifiedTime, Comparator.reverseOrder())  // 정렬 우선순위 1: 수정날짜 내림차순
                        .thenComparing(Memo::getId, Comparator.reverseOrder()))  // 정렬 우선순위 2: id 내림차순
                .map(MemoDto.MemoPageResponse::new)  // User.userMemoList.memo.userMemoList & User.userMemoList.memo.userMemoList.user (내부에서 N+1 쿼리 발생)
                .collect(Collectors.toList());

        return memoPageResponseDtoList;
    }

    @Transactional
    @Override
    public MemoDto.CreateResponse createMemo(MemoDto.CreateRequest createRequestDto) {
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

        return MemoDto.CreateResponse.builder()
                .memoId(memoId)
                .build();
    }

    @Transactional
    @Override
    public void checkEditLock(Long memoId) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        userMemoService.checkUserInMemo(loginUserId, memoId);  // 사용자의 메모 접근권한 체킹.

        String loginUserNickname = userRepository.findNicknameById(loginUserId);
        StringBuilder lockValueStb = new StringBuilder();
        lockValueStb.append("userId:").append(loginUserId).append(",").append("userNickname:").append(loginUserNickname);

        String lockKey = "memoId:" + memoId;
        String lockValue = lockValueStb.toString();
        Long lockTTL = 1000L * 60 * 5;  // TTL 5분

        String value = redisRepository.getValue(lockKey);
        if(value != null) {  // Redis에 해당 메모의 락이 이미 존재한다면
            Object[] userInfo = parseUserInfo(value);
            Long lockUserId = (Long) userInfo[0];
            String lockUserNickname = (String) userInfo[1];

            boolean isOwnLock = (lockUserId != null && lockUserId.equals(loginUserId));
            if(isOwnLock == true) {  // 존재하는 락이 이미 자신의 것이라면
                redisRepository.refreshTTL(lockKey, lockTTL);  // 락의 TTL 연장하기
            }
            else {  // 다른 사용자(락 주인)가 해당 메모를 수정중인 상황임.
                throw new Exception423.LockedData(lockUserNickname);
            }
        }
        else {
            redisRepository.lock(lockKey, lockValue, lockTTL);
        }
    }

    @Transactional
    @Override
    public void releaseEditLock(Long memoId) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        userMemoService.checkUserInMemo(loginUserId, memoId);  // 사용자의 메모 접근권한 체킹.

        String lockKey = "memoId:" + memoId;
        checkOwnLock(lockKey, loginUserId);  // 사용자의 락 접근권한 체킹.
        redisRepository.unlock(lockKey);
    }

    // < 'Redis 분산 락 (Lettuce Lock)' 기반의 퍼사드 메소드 >
//    // updateMemoFacade()와 updateMemo()를 동일 클래스에 작성해 트랜잭션이 적용되지 않으므로, 퍼사드 메소드에도 트랜잭션 명시가 필요함.
//    // 단, 이 경우 updateMemo()의 트랜잭션은 '@Transactional(propagation = Propagation.REQUIRES_NEW)' 선언할것.
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
//            Object[] userInfo = parseUserInfo(value);
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
//            updateMemo(memoId, updateRequestDto);
//        } finally {
//            redisRepository.unlock(lockKey);
//        }
//    }

    // < '낙관적 락 (Optimistic Lock)' 기반의 퍼사드 메소드 >
    @Transactional  // updateMemoFacade()와 updateMemo()를 동일 클래스에 작성해 트랜잭션이 적용되지 않으므로, 퍼사드 메소드에도 트랜잭션 명시가 필요함.
    @Override
    public void updateMemoFacade(Long memoId, MemoDto.UpdateRequest updateRequestDto) {  // 메모의 제목 or 내용을 수정할 경우에 호출하는 메소드
        try {
            String lockKey = "memoId:" + memoId;
            Long loginUserId = SecurityUtil.getCurrentMemberId();
            checkOwnLock(lockKey, loginUserId);  // 사용자의 락 접근권한 체킹.

            // 메모 수정 비즈니스 로직
            updateMemo(memoId, updateRequestDto);

            // - 현재 구조에서는 updateMemo()가 updateMemoFacade() 메소드의 트랜잭션 범위 안에서 실행되므로,
            // updateMemo() 종료 시점에는 트랜잭션이 커밋되지 않음. 실제 DB 업데이트 커밋은 updateMemoFacade() 종료 시 발생함.
            // - 이에 TransactionSynchronizationManager.registerSynchronization()을 사용하면,
            // 트랜잭션이 성공적으로 커밋된 직후에 afterCommit() 콜백이 실행되게 구성할 수 있음.
            // 따라서 이 콜백 안에서 Redis 편집락 해제를 수행하게 하면, 메모 수정이 DB에 완전히 반영된 이후에 락 해제가 이뤄지므로 데이터 정합성을 보장할 수 있음.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    redisRepository.unlock(lockKey);
                }
            });
        } catch (Exception404.NoSuchMemo ex404) {
            throw ex404;
        } catch (Exception ex) {
            throw new Exception409.ConflictData();
        }
    }

    @Transactional
    @Override
    public void updateMemo(Long memoId, MemoDto.UpdateRequest updateRequestDto) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        userMemoService.checkUserInMemo(loginUserId, memoId);  // 사용자의 메모 접근권한 체킹.

        // '낙관적 락 (Optimistic Lock)' 기반의 조회 (이는 트랜잭션 종료 후 update 될때 검증됨.)
        Memo memo = memoRepository.findByIdWithOptimisticLock(memoId).orElseThrow(
                () -> new Exception404.NoSuchMemo(String.format("memoId = %d", memoId)));

        // 메모의 즐겨찾기 여부 수정인 경우
        if(updateRequestDto.getIsStar() != null) {
            memoRepository.updateIsStar(memoId, updateRequestDto.getIsStar());
            return;  // 바로 함수 종료.
        }

        // 즐겨찾기 수정이 아닌, 메모의 제목과 내용 수정인 경우
        memo.updateTitle(updateRequestDto.getTitle());
        memo.updateContent(updateRequestDto.getContent());
    }

    @Transactional
    @Override
    public void deleteMemo(Long memoId) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        userMemoService.checkUserInMemo(loginUserId, memoId);  // 메모를 삭제/탈퇴할 권한이 있는지 체킹.

        // 강제 Eager 조회 (N+1 문제 해결)
        Memo memo = memoRepository.findByIdToUserMemoListWithEager(memoId).orElseThrow(
                () -> new Exception404.NoSuchUser(String.format("memoId = %d", memoId)));
        int memoHasUsersCount = memo.getUserMemoList().size();  // Memo.userMemoList (리스트의 size 측정으로, N+1 쿼리 발생)

        // 공동메모 그룹 탈퇴 처리. (자식 테이블인 UserMemo에서 먼저 삭제.)
        userMemoRepository.deleteByUser_IdAndMemo_Id(loginUserId, memoId);
        userMemoRepository.flush();  // 영속성 컨텍스트 내 변경상태인 위 delete를 즉시 반영.

        if(memoHasUsersCount == 1) {  // 해당 메모가 개인메모라면
            memoRepository.delete(memo);  // 그 이후에 부모 테이블인 Memo에서 해당 메모를 삭제. (이후 부모 테이블인 Memo에서 삭제.)
        }
        else if(memoHasUsersCount == 2) {  // 공동메모인데, 그룹 탈퇴로 메모의 사용자가 2명에서 1명으로 개인메모가 될 경우 (즉, 원래 2명이었을 경우)
            // 즐겨찾기 여부를 다시 0으로 초기화.
            memoRepository.updateIsStar(memoId, 0);  // isStar 필드는 수정시각에 영향을 주지않도록, @LastModifiedDate 생명주기에서 제외시켜 따로 JPQL로 직접 업데이트함.
        }
    }


    // ========== 유틸성 메소드 ========== //

    @Transactional(readOnly = true)
    @Override
    public Memo findMemo(Long memoId) {
        return memoRepository.findById(memoId).orElseThrow(
                () -> new Exception404.NoSuchMemo(String.format("memoId = %d", memoId)));
    }

    private static Predicate<Memo> filterMemos(String filter) {
        if(filter == null) return memo -> true;
        Predicate<Memo> predicate = switch (filter) {
            case "private-memo" -> memo -> memo.getUserMemoList().size() == 1;
            case "group-memo" -> memo -> memo.getUserMemoList().size() > 1;
            case "star-memo" -> memo -> memo.getIsStar() == 1;
            default -> throw new Exception400.MemoBadRequest("잘못된 쿼리파라미터로 API를 요청하였습니다.");
        };
        return predicate;
    }

    private static Predicate<Memo> searchMemos(String search) {
        if(search == null) return memo -> true;
        return memo -> memo.getTitle().contains(search) || memo.getContent().contains(search);
    }

    public void checkOwnLock(String key, Long userId) {
        boolean isOwnLock = redisRepository.checkOwner(key, userId);
        if(isOwnLock == false) {
            throw new Exception423.LockedData(String.format("해당 Lock은 사용자(userId = %d)의 소유가 아니거나 존재하지 않습니다.", userId));
        }
    }

    private static Object[] parseUserInfo(String value) {
        StringTokenizer typeStt = new StringTokenizer(value, ",");
        StringTokenizer fieldStt;
        Long lockUserId = null;
        String lockUserNickname = null;

        while(typeStt.hasMoreTokens()) {
            fieldStt = new StringTokenizer(typeStt.nextToken(), ":");
            String fieldName = fieldStt.nextToken();
            if(lockUserId == null && fieldName.equals("userId") == true) {
                lockUserId = Long.valueOf(fieldStt.nextToken());
            }
            else if(lockUserNickname == null && fieldName.equals("userNickname") == true) {
                lockUserNickname = fieldStt.nextToken();
            }
            if(lockUserId != null && lockUserNickname != null) break;
        }

        return new Object[]{ lockUserId, lockUserNickname };
    }
}
