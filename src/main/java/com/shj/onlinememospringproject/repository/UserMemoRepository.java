package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMemoRepository extends JpaRepository<UserMemo, Long> {
}
