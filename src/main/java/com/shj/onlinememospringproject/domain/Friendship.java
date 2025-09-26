package com.shj.onlinememospringproject.domain;

import com.shj.onlinememospringproject.domain.enums.FriendshipState;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@Table(name = "friendship")  // 사실상 이 테이블도, User과 User의 mapping 다대다 테이블이다.
@Entity
public class Friendship implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friendship_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")  // 친구요청 받은 유저.
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")  // 친구요청 보낸 유저.
    private User senderUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "friendship_state")
    private FriendshipState friendshipState;


    @Builder(builderClassName = "FriendshipSaveBuilder", builderMethodName = "FriendshipSaveBuilder")
    public Friendship(User user, User senderUser) {
        // 이 빌더는 친구관계 생성(Send요청)때만 사용할 용도
        this.user = user;
        this.senderUser = senderUser;
        this.friendshipState = FriendshipState.SEND;
    }


    public void updateFriendshipState(FriendshipState friendshipState) {
        this.friendshipState = friendshipState;
    }
}
