package com.shj.onlinememospringproject.repository;

import com.shj.onlinememospringproject.domain.backoffice.State;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StateRepository extends MongoRepository<State, String> {

    Optional<State> findFirstByOrderByRecentDatetimeDesc();

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'recent_datetime': ?1 } }")
    void updateRecentDatetimeById(String stateId, LocalDateTime recentDatetime);
}
