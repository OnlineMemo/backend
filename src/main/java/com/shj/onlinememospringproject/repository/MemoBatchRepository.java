package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.Memo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MemoBatchRepository {  // 대용량 데이터의 batch 처리를 위한 JDBC Repository

    private static final int BATCH_SIZE = 1000;  // 배치 크기 설정 (메모리 오버헤드 방지)
    private final JdbcTemplate jdbcTemplate;


    public void batchDelete(List<Memo> memoList) {

        for (int i=0; i<memoList.size(); i+=BATCH_SIZE) {
            List<Long> batchList = memoList.subList(i, Math.min(i+BATCH_SIZE, memoList.size()))
                    .stream()
                    .map(Memo::getId)
                    .collect(Collectors.toList());

            String sql = String.format("DELETE FROM memo WHERE memo_id IN (%s)",
                    batchList.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")));

            jdbcTemplate.update(sql);
        }
    }
}
