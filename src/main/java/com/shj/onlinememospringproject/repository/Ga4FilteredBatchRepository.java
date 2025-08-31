package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class Ga4FilteredBatchRepository {  // 대용량 데이터의 batch 처리를 위한 BulkOps Repository

    private final MongoTemplate mongoTemplate;
    private static final int BATCH_SIZE = 1000;  // 배치 크기 설정 (메모리 오버헤드 방지)


    @Transactional  // 원자성을 위함. (ACID 중 A)
    public void batchInsert(List<Ga4Filtered> ga4FilteredList) {

        for (int i=0; i<ga4FilteredList.size(); i+=BATCH_SIZE) {
            List<Ga4Filtered> batchList = ga4FilteredList.subList(i, Math.min(i + BATCH_SIZE, ga4FilteredList.size()));

            BulkOperations bulkOps = mongoTemplate.bulkOps(
                    BulkOperations.BulkMode.ORDERED,  // 순서대로 삽입
                    Ga4Filtered.class
            );

            bulkOps.insert(batchList);
            bulkOps.execute();  // 하나라도 실패힐 경우, 전체 롤백
        }
    }
}
