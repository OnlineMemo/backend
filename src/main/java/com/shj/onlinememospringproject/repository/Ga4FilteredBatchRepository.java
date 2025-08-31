package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class Ga4FilteredBatchRepository {  // 대용량 데이터의 batch 처리를 위한 BulkOps Repository

    private final MongoTemplate mongoTemplate;
    private static final int BATCH_SIZE = 1000;  // 배치 크기 설정 (메모리 오버헤드 방지)


    public void batchInsert(List<Ga4Filtered> ga4FilteredList) {
        // 이미 DB 삽입이 완료된 데이터 추적
        // (단일 MongoDB 환경에서는 트랜잭션을 미지원해, 중간 실패 시 원자성을 확보할 수동 롤백을 구현.)
        List<Ga4Filtered> insertedList = new ArrayList<>();

        for (int i=0; i<ga4FilteredList.size(); i+=BATCH_SIZE) {
            List<Ga4Filtered> batchList = ga4FilteredList.subList(i, Math.min(i + BATCH_SIZE, ga4FilteredList.size()));

            try {
                BulkOperations bulkOps = mongoTemplate.bulkOps(
                        BulkOperations.BulkMode.ORDERED,  // 순서대로 삽입 (외부 Ga4Client API에서 이미 정렬된 데이터를 제공함)
                        Ga4Filtered.class
                );
                bulkOps.insert(batchList);  // 삽입할 bulk 연산을 쌓음 (아직 DB 반영 X)

                bulkOps.execute();  // 쌓인 bulk 연산을 MongoDB에 전송 (DB 반영 O)
                insertedList.addAll(batchList);
            } catch (Exception ex) {
                if(!insertedList.isEmpty()) {
                    List<String> idList = insertedList.stream()
                            .map(Ga4Filtered::getId)
                            .collect(Collectors.toList());
                    mongoTemplate.remove(Query.query(Criteria.where("_id").in(idList)), Ga4Filtered.class);  // 수동 롤백
                }
                throw ex;
            }
        }
    }
}
