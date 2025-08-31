package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface Ga4FilteredRepository extends MongoRepository<Ga4Filtered, String> {  // MongoDB

    Optional<Ga4Filtered> findFirstByOrderByEventDatetimeDesc();
    List<Ga4Filtered> findByEventDatetimeBetweenOrderByEventDatetimeAsc(LocalDateTime startDatetime, LocalDateTime endDatetime);
}
