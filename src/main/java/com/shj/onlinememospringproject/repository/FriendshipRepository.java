package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.Friendship;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.enums.FriendshipState;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // - Eager 조회 : 'Friendship + Friendship.senderUser'
    @EntityGraph(attributePaths = {"senderUser"})
    List<Friendship> findAllByUserAndFriendshipState(User user, FriendshipState friendshipState);
}
