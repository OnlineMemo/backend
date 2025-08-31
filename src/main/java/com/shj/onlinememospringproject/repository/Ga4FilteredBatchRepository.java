package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.MongoBulkWriteException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class Ga4FilteredBatchRepository {  // 대용량 데이터의 batch 처리를 위한 BulkOps Repository

    private final MongoTemplate mongoTemplate;
    private static final int BATCH_SIZE = 1000;  // 배치 크기 설정 (메모리 오버헤드 방지)
    private static final int MAX_RETRY = 3;  // 삽입 재시도 최대 횟수


    public void batchInsert(List<Ga4Filtered> ga4FilteredList) {  // 참고로 단일 MongoDB 환경에서는 트랜잭션을 미지원함.
        List<Ga4Filtered> failedList = new ArrayList<>();  // 삽입 실패한 데이터들

        // 기본 Bulk 삽입
        for (int i=0; i<ga4FilteredList.size(); i+=BATCH_SIZE) {
            List<Ga4Filtered> batchList = ga4FilteredList.subList(i, Math.min(i + BATCH_SIZE, ga4FilteredList.size()));

            try {
                BulkOperations bulkOps = mongoTemplate.bulkOps(
                        BulkOperations.BulkMode.UNORDERED,  // 순서 무관하게 삽입
                        Ga4Filtered.class
                );
                bulkOps.insert(batchList);  // 삽입할 bulk 연산을 쌓음 (아직 DB 반영 X)
                bulkOps.execute();  // 쌓인 bulk 연산을 MongoDB에 전송 (DB 반영 O)
            } catch (BulkOperationException boEx) {
                Throwable cause = boEx.getCause();
                if (cause instanceof MongoBulkWriteException) {
                    MongoBulkWriteException mbwEx = (MongoBulkWriteException) cause;
                    mbwEx.getWriteErrors().forEach(error -> {
                        int failedIdx = error.getIndex();  // 실패한 데이터 추출
                        Ga4Filtered failedGa4Filtered = batchList.get(failedIdx);
                        failedList.add(failedGa4Filtered);
                    });
                }
            }
        }

        // 재시도 Bulk 삽입
        int attemptCnt = 0;
        while (!failedList.isEmpty() && attemptCnt < MAX_RETRY) {
            List<Ga4Filtered> retryList = new ArrayList<>(failedList);
            failedList.clear();

            for (int i=0; i<retryList.size(); i+=BATCH_SIZE) {
                List<Ga4Filtered> batchList = retryList.subList(i, Math.min(i + BATCH_SIZE, retryList.size()));

                try {
                    BulkOperations bulkOps = mongoTemplate.bulkOps(
                            BulkOperations.BulkMode.UNORDERED,  // 순서 무관하게 삽입
                            Ga4Filtered.class
                    );
                    bulkOps.insert(batchList);  // 삽입할 bulk 연산을 쌓음 (아직 DB 반영 X)
                    bulkOps.execute();  // 쌓인 bulk 연산을 MongoDB에 전송 (DB 반영 O)
                } catch (BulkOperationException boEx) {
                    Throwable cause = boEx.getCause();
                    if (cause instanceof MongoBulkWriteException) {
                        MongoBulkWriteException mbwEx = (MongoBulkWriteException) cause;
                        mbwEx.getWriteErrors().forEach(error -> {
                            int failedIdx = error.getIndex();  // 실패한 데이터 추출
                            Ga4Filtered failedGa4Filtered = batchList.get(failedIdx);
                            failedList.add(failedGa4Filtered);
                        });
                    }
                }
            }
            attemptCnt++;
        }
    }
}
