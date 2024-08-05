package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
}
