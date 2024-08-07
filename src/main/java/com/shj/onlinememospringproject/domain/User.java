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

    @OneToMany(mappedBy = "user")  // User-Friendship 양방향매핑 1 (읽기 전용 필드)
    private List<Friendship> receivefriendshipList = new ArrayList<>();  // 나에게 친구요청을 보내온(받은) 관계리스트 gerSenderUser 활용할것.

    @OneToMany(mappedBy = "senderUser")  // User-Friendship 양방향매핑 2 (읽기 전용 필드)
    private List<Friendship> sendfriendshipList = new ArrayList<>();  // 내가 친구요청을 보낸(신청한) 관계리스트 gerUser 활용할것.


    @Builder(builderClassName = "UserSaveBuilder", builderMethodName = "UserSaveBuilder")
    public User(String email, String password, String nickname) {
        // 이 빌더는 사용자 회원가입때만 사용할 용도
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.authority = Authority.ROLE_USER;
    }


    public void updatePassword(String password) {
        this.password = password;
    }
}