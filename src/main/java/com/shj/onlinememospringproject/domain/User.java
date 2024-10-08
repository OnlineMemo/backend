package com.shj.onlinememospringproject.domain;

import com.shj.onlinememospringproject.domain.enums.Authority;
import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Column(name = "refresh_token")
    private String refreshToken;

    @OneToMany(mappedBy = "user")  // User-UserMemo 양방향매핑 (읽기 전용 필드)
    private Set<UserMemo> userMemoList = new HashSet<>();  // MultipleBagFetchException Fetch Join 에러 해결을 위해, Set으로 선언하고 List로 변환해서 사용함.

    @OneToMany(mappedBy = "user")  // User-Friendship 양방향매핑 1 (읽기 전용 필드)
    private List<Friendship> receiveFriendshipList = new ArrayList<>();  // 나에게 친구요청을 보내온(받은) 관계리스트 getSenderUser 활용할것.

    @OneToMany(mappedBy = "senderUser")  // User-Friendship 양방향매핑 2 (읽기 전용 필드)
    private List<Friendship> sendFriendshipList = new ArrayList<>();  // 내가 친구요청을 보낸(신청한) 관계리스트 getUser 활용할것.


    @Builder(builderClassName = "UserSaveBuilder", builderMethodName = "UserSaveBuilder")
    public User(String email, String password, String nickname) {
        // 이 빌더는 사용자 회원가입때만 사용할 용도 (refreshToken=null로 저장됨.)
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.authority = Authority.ROLE_USER;
    }


    // get UserMemoList
    public List<UserMemo> getUserMemoList() {
        return new ArrayList<>(this.userMemoList);
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateNickName(String nickname) {
        this.nickname = nickname;
    }
}