package com.shj.onlinememospringproject.domain;

import com.shj.onlinememospringproject.domain.enums.Authority;
import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor

@Table(name = "user")
@Entity
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", unique = true)
    private String email;

    private String password;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Authority authority;

    @OneToMany(mappedBy = "user")  // User-UserMemo 양방향매핑 (읽기 전용 필드)
    private List<UserMemo> userMemoList = new ArrayList<>();

    @OneToMany(mappedBy = "user")  // User-Friendship 양방향매핑 (읽기 전용 필드)
    private List<Friendship> friendshipList = new ArrayList<>();


    @Builder(builderClassName = "UserSaveBuilder", builderMethodName = "UserSaveBuilder")
    public User(String email, String password, String nickname, Authority authority) {
        // 이 빌더는 사용자 회원가입때만 사용할 용도
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.authority = authority;
    }
}