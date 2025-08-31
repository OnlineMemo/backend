package com.shj.onlinememospringproject.domain.backoffice;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor

@Document(collection = "state")  // 백오피스 전용 데이터는 MongoDB로 관리.
public class State {

    @Id
    private String id;

    @Indexed
    @Field("recent_datetime")
    private LocalDateTime recentDatetime;


    @Builder(builderClassName = "StateSaveBuilder", builderMethodName = "StateSaveBuilder")
    public State(LocalDateTime recentDatetime) {
        // 이 빌더는 State 생성때만 사용할 용도
        this.recentDatetime = recentDatetime;
    }
}
