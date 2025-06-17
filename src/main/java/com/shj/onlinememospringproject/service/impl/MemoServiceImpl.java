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
    private static final long EDIT_LOCK_EXPIRE_TIME = 1000L * 60 * 10;  // Redis 편집락 TTL : 10분


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
        if(!userMemoService.checkGroupMemo(memoId)) return;  // 공동메모 여부 체킹. (개인메모라면 락 제어는 불필요하므로 즉시 종료.)

        String loginUserNickname = userRepository.findNicknameById(loginUserId);
        StringBuilder lockValueStb = new StringBuilder();
        lockValueStb.append("userId:").append(loginUserId).append(",").append("userNickname:").append(loginUserNickname);

        String lockKey = "memoId:" + memoId;
        String lockValue = lockValueStb.toString();
        long lockTTL = EDIT_LOCK_EXPIRE_TIME;

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
        if(!userMemoService.checkGroupMemo(memoId)) return;  // 공동메모 여부 체킹. (개인메모라면 락 제어는 불필요하므로 즉시 종료.)

        String lockKey = "memoId:" + memoId;
        redisRepository.unlockOwner(lockKey, loginUserId);
    }

    // - @Transactional(propagation = Propagation.REQUIRES_NEW) 사용 이유 :
    // '자식인 updateMemo() 비즈니스 메소드'를 '부모인 updateMemoFacade() 퍼사드 메소드' 내에서 별도의 트랜잭션으로 커밋을 독립적으로 수행하게 하려면,
    // 서로 다른 클래스에 분리하여 정의해야하며, 트랜잭션 전파 속성을 '@Transactional(propagation = Propagation.REQUIRES_NEW)'로 선언해야함.
    // - 위의 트랜잭션 전파 속성을 정의하지 않을 경우 발생하는 문제 :
    // 이 경우, 자식 메소드가 부모 트랜잭션 범위 내에서 실행되어 커밋 시점이 부모와 일치하게 되고, 결국 자식 update 쿼리의 DB 커밋도 부모 트랜잭션이 끝날 때까지 지연되어 반영되지 않음.
    // 따라서 부모의 try~catch 문에서는 아직 자식의 update 쿼리가 DB에 커밋되기 전이므로 낙관적 락 충돌이 발생하지 않아, 의도한 'throw new Exception409.ConflictData()' 예외로 감지할 수 없음.
    // 그렇게 되면 최종적으로 부모 메소드가 모두 종료된 후에야 500 에러로 대신 발생하게 되어, 의도한 예외 처리로 잡히지 않게됨.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void updateMemo(Long memoId, MemoDto.UpdateRequest updateRequestDto) {
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        userMemoService.checkUserInMemo(loginUserId, memoId);  // 사용자의 메모 접근권한 체킹.

        // - case 1. 메모의 즐겨찾기 여부 수정인 경우
        if(updateRequestDto.getIsStar() != null) {
            memoRepository.updateIsStar(memoId, updateRequestDto.getIsStar());
            return;  // 바로 함수 종료.
        }

        // '낙관적 락 (Optimistic Lock)' 기반의 조회 (이는 트랜잭션 종료 후 update 될때 검증됨.)
        Memo memo = memoRepository.findByIdWithOptimisticLock(memoId).orElseThrow(
                () -> new Exception404.NoSuchMemo(String.format("memoId = %d", memoId)));

        // 1차 검증 : 전달받은 메모의 현재 버전과 DB 조회된 버전이 일치하는지 확인 (수동 필드 기반)
        Long currentVersion = updateRequestDto.getCurrentVersion();
        if(currentVersion == null) {
            throw new Exception400.MemoBadRequest("잘못된 필드값으로 API를 요청하였습니다.");
        }
        else if(currentVersion.equals(memo.getVersion()) == false) {  // && currentVersion != null
            throw new Exception409.ConflictData();
            // updateMemo 메소드의 Exception409.ConflictData()
            // -> updateMemoFacade 메소드의 catch(Exception){}
            // -> Exception409.ConflictData() 재처리 가능
        }

        // - case 2. 즐겨찾기 수정이 아닌, 메모의 제목과 내용 수정인 경우 (2차 검증 - 낙관적 락 기반)
        // 2차 검증 : 트랜잭션 커밋 시점에 JPA가 버전 일치 여부로 충돌 판단 (낙관적 락 기반)
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
        else {  // 해당 메모가 공동메모라면
            String lockKey = "memoId:" + memoId;
            if(memoHasUsersCount == 2) {  // 공동메모인데, 그룹 탈퇴로 메모의 사용자가 2명에서 1명으로 개인메모가 될 경우 (즉, 원래 2명이었을 경우)
                // isStar 필드는 수정시각에 영향을 주지않도록, @LastModifiedDate 생명주기에서 제외시켜 따로 JPQL로 직접 업데이트함.
                memoRepository.updateIsStar(memoId, 0);  // 즐겨찾기 여부를 다시 0으로 초기화.
                // 개인메모 체제로 변경되었으므로, Redis 내 편집락이 필요 없어짐.
                redisRepository.unlock(lockKey);  // Redis 내 편집락 해제 (본인 무관)
            }
            else {  // 그룹을 탈퇴해도 인원수가 2명 이상 남아, 공동메모 체제를 유지할 경우
                redisRepository.unlockOwner(lockKey, loginUserId);  // Redis 내 본인의 편집락 해제. (락 소유자만 가능)
            }
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
        String lowerSearch = search.toLowerCase();
        return memo -> memo.getTitle().toLowerCase().contains(lowerSearch) || memo.getContent().toLowerCase().contains(lowerSearch);
    }

    @Override
    public void checkOwnLock(String key, Long userId, boolean isRequiredExistKey) {  // DI된 redisRepository 의존성 인스턴스 변수를 사용하므로, static으로는 선언하지 않는것이 권장됨.
        // 파라미터 isRequiredExistKey==true인 경우 :
        //      - Redis에 해당 키가 존재하지않을때 검사 실패.
        //      - Redis에 해당 키가 존재하나, 본인의 락이 아니라면 검사 실패.
        // 파라미터 isRequiredExistKey==false인 경우 (굳이 키가 존재하지않아도 괜찮음) :
        //      - Redis에 해당 키가 존재하나, 본인의 락이 아니라면 검사 실패.
        Boolean isOwnLock = redisRepository.checkOwner(key, userId);
        if(isOwnLock == null) {
            if(isRequiredExistKey == true) {  // 반드시 Redis에 키가 존재해야만 하는가?
                throw new Exception423.LockedData(String.format("해당 데이터의 Lock은 존재하지 않습니다.", userId));
            }
        }
        else if(isOwnLock == false) {  // && isOwnLock != null
            throw new Exception423.LockedData(String.format("해당 데이터의 Lock은 사용자(userId=%d)의 소유가 아닙니다.", userId));
        }
    }

    public static Object[] parseUserInfo(String value) {
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
