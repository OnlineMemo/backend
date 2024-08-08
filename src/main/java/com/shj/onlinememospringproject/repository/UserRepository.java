package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 내부의 Lazy 필드를 Eager로 조회하여 N+1 문제 해결 ('@EntityGraph' : 간단한 로직, 'Fetch Join' : 복잡한 로직 or JPA 쿼리메소드와의 네이밍 충돌 방지용)
    // 다만, 가능한 한 LEFT JOIN 말고 성능이 더 우수한 JOIN 사용할 것.
    // - 네이밍이 남아서 JPA 쿼리메소드 규칙을 따라도 되는 경우 && 하위 엔티티의 존재가 없어도 정상 반환해주어야함 ==> EntityGraph (LEFT JOIN)
    // - 네이밍이 남아서 JPA 쿼리메소드 규칙을 따라도 되는 경우 && 하위 엔티티의 존재한다는것이 이미 확정일때 ==> Fetch Join (JOIN)
    // - 네이밍이 부족해서 JPA 쿼리메소드 규칙을 따르지 못하는 경우 && 하위 엔티티의 존재가 없어도 정상 반환해주어야함 ==> Fetch Join (LEFT JOIN)
    // - 네이밍이 부족해서 JPA 쿼리메소드 규칙을 따르지 못하는 경우 && 하위 엔티티의 존재한다는것이 이미 확정일때 ==> Fetch Join (JOIN)

    // Eager 조회 : 'User + User.userMemoList + User.userMemoList.memo' (하위의 userRoomList가 비어있더라도 정상 반환되도록, LEFT JOIN 사용. userRoomList가 비어있을수도있으니, User.userMemoList.memo 부분도 LEFT JOIN 사용.)
    // 비록 OneToMany 필드인 userMemoList를 Eager로 지정하여 카테시안곱의 중복데이터 위험이 있지만, 메소드 반환자료형이 List가 아닌 고유한 하나의 값이기에, DISTINCT 없이 작성해도 무방함.
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userMemoList uml LEFT JOIN FETCH uml.memo WHERE u.id = :userId")
    Optional<User> findByIdWithEager(@Param("userId") Long userId);

    Optional<User> findByEmail(String email);
    List<User> findByIdIn(List<Long> userIdList);
}
