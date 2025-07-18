package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserMemoRepository extends JpaRepository<UserMemo, Long> {
    // 내부의 Lazy 필드를 Eager로 조회하여 N+1 문제 해결 ('@EntityGraph' : 간단한 로직, 'Fetch Join' : 복잡한 로직 or JPA 쿼리메소드와의 네이밍 충돌 방지용)
    // 다만, 가능한 한 LEFT JOIN 말고 성능이 더 우수한 JOIN 사용할 것.
    // - 네이밍이 남아서 JPA 쿼리메소드 규칙을 따라도 되는 경우 && 하위 엔티티의 존재가 없어도 정상 반환해주어야함 ==> EntityGraph (LEFT JOIN)
    // - 네이밍이 남아서 JPA 쿼리메소드 규칙을 따라도 되는 경우 && 하위 엔티티의 존재한다는것이 이미 확정일때 ==> Fetch Join (JOIN)
    // - 네이밍이 부족하거나 오버라이딩으로 겹쳐서 JPA 쿼리메소드 규칙을 따르지 못하는 경우 && 하위 엔티티의 존재가 없어도 정상 반환해주어야함 ==> Fetch Join (LEFT JOIN)
    // - 네이밍이 부족하거나 오버라이딩으로 겹쳐서 JPA 쿼리메소드 규칙을 따르지 못하는 경우 && 하위 엔티티의 존재한다는것이 이미 확정일때 ==> Fetch Join (JOIN)

    // Eager 조회 : 'UserMemo + UserMemo.memo + UserMemo.memo.userMemoList' (하위의 memo는 반드시 존재하므로 JOIN 사용하나, userMemoList는 비어있을수도있어 LEFT JOIN 사용.)
    @Query("SELECT um FROM UserMemo um JOIN FETCH um.memo m LEFT JOIN FETCH m.userMemoList WHERE um.user.id = :userId AND um.memo.id = :memoId")
    Optional<UserMemo> findByUser_IdAndMemo_IdToUserMemoListWithEager(@Param("userId") Long userId, @Param("memoId") Long memoId);

    // memoId의 메모가 공동메모인지 여부를 확인하는 메소드
    @Query(value = "SELECT EXISTS (SELECT 1 FROM user_memo WHERE memo_id = :memoId LIMIT 2 OFFSET 1)", nativeQuery = true)  // 두번째 작성자가 존재하는지만 확인하여, 2인 이상 여부 판단.
    Long isGroupMemoByMemoId(@Param("memoId") Long memoId);

    boolean existsByUser_IdAndMemo_Id(Long userId, Long memoId);
    void deleteByUser_IdAndMemo_Id(Long userId, Long memoId);
}
