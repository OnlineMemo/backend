package com.shj.onlinememospringproject.dto.user;

import com.shj.onlinememospringproject.domain.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdatePwRequestDto {  // 요청하는 DTO. 예를들어 CRUD의 C. method로는 post.
    // 사용자 1차비밀번호 수정 전용의 RequestDto

    private String loginId;
    private String firstPw;
    private String secondPw;

    @Builder
    public UserUpdatePwRequestDto(String loginId, String firstPw, String secondPw) {
        this.loginId = loginId;
        this.firstPw = firstPw;
        this.secondPw = secondPw;
    }

    // 클라이언트에게 받아왔고 계층간 이동에 사용되는 dto를 DB에 접근할수있는 entity로 변환 용도
    public User toEntity() {
        return User.UserUpdatePwBuilder()
                .loginId(loginId)
                .firstPw(firstPw)
                .secondPw(secondPw)
                .build();
    }
}