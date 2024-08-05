package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, Long> {
}
