package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.Friendship;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.enums.FriendshipState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    // 내부의 Lazy 필드를 Eager로 조회하여 N+1 문제 해결 ('@EntityGraph' : 간단한 로직, 'Fetch Join' : 복잡한 로직 or JPA 쿼리메소드와의 네이밍 충돌 방지용)
    // 다만, 가능한 한 LEFT JOIN 말고 성능이 더 우수한 JOIN 사용할 것.
    // - 네이밍이 남아서 JPA 쿼리메소드 규칙을 따라도 되는 경우 && 하위 엔티티의 존재가 없어도 정상 반환해주어야함 ==> EntityGraph (LEFT JOIN)
    // - 네이밍이 남아서 JPA 쿼리메소드 규칙을 따라도 되는 경우 && 하위 엔티티의 존재한다는것이 이미 확정일때 ==> Fetch Join (JOIN)
    // - 네이밍이 부족해서 JPA 쿼리메소드 규칙을 따르지 못하는 경우 && 하위 엔티티의 존재가 없어도 정상 반환해주어야함 ==> Fetch Join (LEFT JOIN)
    // - 네이밍이 부족해서 JPA 쿼리메소드 규칙을 따르지 못하는 경우 && 하위 엔티티의 존재한다는것이 이미 확정일때 ==> Fetch Join (JOIN)

    // Eager 조회 : 'Friendship + Friendship.senderUser' (어차피 하위의 senderUser는 반드시 존재하므로, JOIN 사용.)
    @Query("SELECT f FROM Friendship f JOIN FETCH f.senderUser WHERE f.user = :user AND f.friendshipState = :friendshipState")
    List<Friendship> findAllByUserAndFriendshipStateWithEager(@Param("user") User user, @Param("friendshipState") FriendshipState friendshipState);

    boolean existsByUserAndSenderUser(User user, User senderUser);
    Optional<Friendship> findByUser_IdAndSenderUser_Id(Long userId, Long senderUserId);
    Optional<Friendship> findByUser_IdAndSenderUser_IdAndFriendshipState(Long userId, Long senderUserId, FriendshipState friendshipState);
}
