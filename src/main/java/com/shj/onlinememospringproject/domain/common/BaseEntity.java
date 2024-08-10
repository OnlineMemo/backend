package com.shj.onlinememospringproject.domain.common;

import com.shj.onlinememospringproject.util.TimeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Column(name = "modified_time")
    protected String modifiedTime;


    @PreUpdate
    public void onPreUpdate(){  // isStar 필드는 수정시각에 영향을 주지않도록, @PreUpdate 생명주기에서 제외시켜 따로 JPQL로 직접 업데이트함.
        this.modifiedTime = TimeConverter.timeToString(LocalDateTime.now());
    }
}