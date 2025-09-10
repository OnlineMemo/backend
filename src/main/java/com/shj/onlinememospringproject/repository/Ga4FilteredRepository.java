package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface Ga4FilteredRepository extends MongoRepository<Ga4Filtered, String> {  // MongoDB

    Optional<Ga4Filtered> findFirstByOrderByEventDatetimeDesc();

    // 주의 : Spring Data MongoDB는 LocalDateTime 쿼리 시, JVM 시간대를 적용하여 UTC로 변환함.
    //       따라서 검색 시 KST 파라미터를 주면, Spring이 자동으로 UTC로 변환해 MongoDB로 전송해줌.
    List<Ga4Filtered> findByEventDatetimeBetweenOrderByEventDatetimeAsc(LocalDateTime startDatetime, LocalDateTime endDatetime);

    long count();
}
