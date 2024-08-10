package com.shj.onlinememospringproject.domain;

import com.shj.onlinememospringproject.domain.common.BaseEntity;
import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor

@Table(name = "memo")
@Entity
public class Memo extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_id")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "is_star", columnDefinition = "TINYINT(1) default 0", length = 1)
    private Integer isStar;  // isStar 필드는 수정시각에 영향을 주지않도록, @LastModifiedDate 생명주기에서 제외시켜 따로 JPQL로 직접 업데이트함.

    @OneToMany(mappedBy = "memo")  // Memo-UserMemo 양방향매핑 (읽기 전용 필드)
    private List<UserMemo> userMemoList = new ArrayList<>();


    @Builder(builderClassName = "MemoSaveBuilder", builderMethodName = "MemoSaveBuilder")
    public Memo(String title, String content) {
        // 이 빌더는 메모 생성때만 사용할 용도
        this.title = title;
        this.content = content;
        this.isStar = 0;
        this.modifiedTime = LocalDateTime.now();
    }


    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
