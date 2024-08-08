package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.Friendship;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.enums.FriendshipState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // - Eager 조회 : 'Friendship + Friendship.senderUser'
    // 하위 엔티티의 존재가 없어도 정상 반환해주고 싶다면 LEFT JOIN을 사용해야겠지만, 어차피 하위의 senderUser는 반드시 존재하므로, LEFT JOIN(EntityGraph) 말고 성능이 더 우수한 JOIN(Fetch Join) 사용.
    @Query("SELECT f FROM Friendship f JOIN FETCH f.senderUser WHERE f.user = :user AND f.friendshipState = :friendshipState")
    List<Friendship> findAllByUserAndFriendshipState(@Param("user") User user, @Param("friendshipState") FriendshipState friendshipState);
}
