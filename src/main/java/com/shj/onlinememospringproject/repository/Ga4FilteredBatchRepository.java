package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.MongoBulkWriteException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class Ga4FilteredBatchRepository {  // 대용량 데이터의 batch 처리를 위한 BulkOps Repository

    private static final int BATCH_SIZE = 1000;  // 배치 크기 설정 (메모리 오버헤드 방지)
    private static final int MAX_RETRY = 3;  // 삽입 재시도 최대 횟수

    private final Ga4FilteredRepository ga4FilteredRepository;
    private final MongoTemplate mongoTemplate;


    public void batchInsert(List<Ga4Filtered> ga4FilteredList) {  // 참고로 단일 MongoDB 환경에서는 트랜잭션을 미지원함.
        List<Ga4Filtered> failedList = new ArrayList<>();  // 삽입 실패한 데이터들
        long insertCount = ga4FilteredList.size();
        long beforeAllCount = ga4FilteredRepository.count(), afterAllCount = -1;
        long firstFailedCount = -1, lastFailedCount = -1;

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
        firstFailedCount = failedList.size();

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
        afterAllCount = ga4FilteredRepository.count();
        lastFailedCount = failedList.size();

        logInsertResult(insertCount, beforeAllCount, afterAllCount, firstFailedCount, lastFailedCount);  // 로깅
    }


    // ========== 유틸성 메소드 ========== //

    private void logInsertResult(long insertCount, long beforeAllCount, long afterAllCount, long firstFailedCount, long lastFailedCount) {
        boolean isSuccess = (lastFailedCount == 0);
        StringBuilder logMessageStb = new StringBuilder(isSuccess ? "SUCCESS" : "FAIL")
                .append(" - MongoDB Batch Insert ");

        if (insertCount == 0) {
            logMessageStb.append("전체 성공 (데이터 없음)");
        }
        else {  // else if (insertCount > 0)
            if (lastFailedCount == 0) {
                if (firstFailedCount == 0) logMessageStb.append("전체 성공");
                else logMessageStb.append("전체 성공 (실패 복구)");
            }
            else {
                if (insertCount == lastFailedCount) logMessageStb.append("전체 실패");
                else logMessageStb.append("일부 실패");
            }
        }

        logMessageStb.append("\n==> ")
                .append("삽입: ").append(insertCount).append("개 / ")
                .append("전체(전): ").append(beforeAllCount).append("개, ") .append("(후): ").append(afterAllCount).append("개 / ")
                .append("실패(첫): ").append(firstFailedCount).append("개, ") .append("(끝): ").append(lastFailedCount).append("개");

        if (isSuccess) log.info(logMessageStb.toString());
        else log.warn(logMessageStb.toString());
    }
}
