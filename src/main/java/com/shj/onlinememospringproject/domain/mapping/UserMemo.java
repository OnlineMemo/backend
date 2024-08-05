package com.shj.onlinememospringproject.domain.mapping;

import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor

@Table(name = "user_memo")
@Entity
public class UserMemo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_memo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id")
    private Memo memo;


    @Builder(builderClassName = "UserMemoSaveBuilder", builderMethodName = "UserMemoSaveBuilder")
    public UserMemo(User user, Memo memo) {
        // 이 빌더는 UserMemo 생성때만 사용할 용도
        this.user = user;
        this.memo = memo;
    }
}
