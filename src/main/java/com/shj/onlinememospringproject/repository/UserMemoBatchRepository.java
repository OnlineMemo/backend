package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserMemoBatchRepository {  // 대용량 데이터의 batch 처리를 위한 JDBC Repository

    private final JdbcTemplate jdbcTemplate;
    private static final int BATCH_SIZE = 1000;  // 배치 크기 설정 (메모리 오버헤드 방지)


    public void batchInsert(List<UserMemo> userMemoList) {
        String sql = "INSERT INTO user_memo (user_id, memo_id) VALUES (?, ?)";

        for (int i=0; i<userMemoList.size(); i+=BATCH_SIZE) {
            List<UserMemo> batchList = userMemoList.subList(i, Math.min(i+BATCH_SIZE, userMemoList.size()));

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    UserMemo userMemo = batchList.get(i);
                    ps.setLong(1, userMemo.getUser().getId());
                    ps.setLong(2, userMemo.getMemo().getId());
                }

                @Override
                public int getBatchSize() {
                    return batchList.size();
                }
            });
        }
    }

    public void batchDelete(List<UserMemo> userMemoList) {

        for (int i=0; i<userMemoList.size(); i+=BATCH_SIZE) {
            List<Long> batchList = userMemoList.subList(i, Math.min(i+BATCH_SIZE, userMemoList.size()))
                    .stream()
                    .map(UserMemo::getId)
                    .collect(Collectors.toList());

            String sql = String.format("DELETE FROM user_memo WHERE user_memo_id IN (%s)",
                    batchList.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")));

            jdbcTemplate.update(sql);
        }
    }
}
