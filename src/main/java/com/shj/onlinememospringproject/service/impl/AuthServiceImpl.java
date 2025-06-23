package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.domain.Friendship;
import com.shj.onlinememospringproject.domain.Memo;
import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.domain.mapping.UserMemo;
import com.shj.onlinememospringproject.dto.AuthDto;
import com.shj.onlinememospringproject.jwt.TokenProvider;
import com.shj.onlinememospringproject.repository.FriendshipBatchRepository;
import com.shj.onlinememospringproject.repository.MemoBatchRepository;
import com.shj.onlinememospringproject.repository.UserMemoBatchRepository;
import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.response.exception.Exception404;
import com.shj.onlinememospringproject.service.AuthService;
import com.shj.onlinememospringproject.service.UserService;
import com.shj.onlinememospringproject.util.SecurityUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final MemoBatchRepository memoBatchRepository;
    private final FriendshipBatchRepository friendshipBatchRepository;
    private final UserMemoBatchRepository userMemoBatchRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder managerBuilder;


    @Transactional
    @Override
    public void signup(AuthDto.SignupRequest signupRequestDto) {
        String newEmail = signupRequestDto.getEmail();
        userRepository.findByEmail(newEmail)
                .ifPresent(user -> { throw new Exception400.EmailDuplicate(newEmail); });  // 회원가입 로그인아이디 중복 예외처리.

        User user = User.UserSaveBuilder()
                .email(signupRequestDto.getEmail())
                .password(toEncodePassword(signupRequestDto.getPassword()))
                .nickname(signupRequestDto.getNickname())
                .build();
        userRepository.save(user);
    }

    @Transactional
    @Override
    public AuthDto.TokenResponse login(AuthDto.LoginRequest loginRequestDto) {
        UsernamePasswordAuthenticationToken authenticationToken = toAuthentication(loginRequestDto.getEmail(), loginRequestDto.getPassword());
        Authentication authentication;
        try {
            authentication = managerBuilder.getObject().authenticate(authenticationToken);  // 아이디와 비밀번호가 일치하는지 검증.
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException(String.format("email = %s", loginRequestDto.getEmail()), ex);
        }

        User user = userService.findUser(Long.valueOf(authentication.getName()));
        String refreshToken = user.getRefreshToken();
        AuthDto.TokenResponse tokenResponseDto;

        if(refreshToken == null || tokenProvider.validateToken(refreshToken) == false) {  // DB에 Refresh Token이 아직 등록되지 않았거나, 만료 또는 잘못된 토큰인 경우
            tokenResponseDto = tokenProvider.generateTokenDto(authentication);  // Access Token & Refresh Token 모두를 재발급.
            user.updateRefreshToken(tokenResponseDto.getRefreshToken());  // DB Refresh Token 업데이트.
        }
        else {  // DB에 만료되지않은 정상적인 Refresh Token을 갖고있는 경우
            tokenResponseDto = tokenProvider.generateAccessTokenByRefreshToken(authentication, refreshToken);  // 오직 Access Token 하나만을 재발급.
        }
        return tokenResponseDto;  // 로그인 성공. JWT 토큰 생성.
    }

    @Transactional
    @Override
    public void updatePassword(AuthDto.UpdateRequest updateRequestDto) {
        UsernamePasswordAuthenticationToken authenticationToken = toAuthentication(updateRequestDto.getEmail(), updateRequestDto.getPassword());
        Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);  // 로그인이 가능한 계정이 맞는지 검증.

        User user = userService.findUser(Long.valueOf(authentication.getName()));
        user.updatePassword(toEncodePassword(updateRequestDto.getNewPassword()));
    }

    @Transactional
    @Override
    public void withdrawal() {
        // 강제 Eager 조회 (N+1 문제 해결)
        Long loginUserId = SecurityUtil.getCurrentMemberId();
        User user = userRepository.findByIdToMemoWithEager(SecurityUtil.getCurrentMemberId()).orElseThrow(
                () -> new Exception404.NoSuchUser(String.format("userId = %d", loginUserId)));
        List<UserMemo> userMemoList = user.getUserMemoList();

        // 사용자와 메모와의 관계를 삭제하기 이전에, 먼저 해당 사용자가 보유한 메모들부터 미리 리스트에 담아둠.
        List<Memo> memoList = userMemoList.stream()  // User.usermemoList (N+1 쿼리 발생)
                .map(UserMemo::getMemo)
                .collect(Collectors.toList());

        // 부모 테이블인 User보다 먼저, 자식 테이블인 UserAndMemo에서 사용자와 메모와의 관계부터 삭제.
        userMemoBatchRepository.batchDelete(userMemoList);  // UserMemos - Batch Delete

        // 사용자와 메모와의 관계 삭제이후, 담아두었던 메모의 남은 사용자가 0명이라면, 해당 메모도 삭제.
        List<Memo> deleteMemoList = memoList.stream()
                .filter(memo -> memo.getUserMemoList().isEmpty())  // User.usermemoList.memo (N+1 쿼리 발생)
                .collect(Collectors.toList());
        memoBatchRepository.batchDelete(deleteMemoList);  // Memos - Batch Delete

        // 부모 테이블인 User보다 먼저, 자식 테이블인 Friendship에서 요청사용자와 친구와의 관계부터 삭제.
        Set<Friendship> deleteFriendshipSet = new HashSet<>();  // 중복제거를 위해 Set 사용.
        deleteFriendshipSet.addAll(user.getReceiveFriendshipList());
        deleteFriendshipSet.addAll(user.getSendFriendshipList());
        List<Friendship> deleteFriendshipList = new ArrayList<>(deleteFriendshipSet);
        friendshipBatchRepository.batchDelete(deleteFriendshipList);  // Friendships - Batch Delete

        // 최종적으로, 부모 테이블인 User를 삭제.
        userRepository.delete(user);
    }

    @Transactional
    @Override
    public AuthDto.TokenResponse reissue(AuthDto.ReissueRequest reissueRequestDto) {  // Refresh Token으로 Access Token 재발급 메소드

        // RequestDto로 전달받은 Token값들
        String accessToken = reissueRequestDto.getAccessToken();
        String refreshToken = reissueRequestDto.getRefreshToken();

        // Refresh Token 만료 및 유효성 검사
        Boolean tokenStatus = tokenProvider.checkTokenStatus(refreshToken);
        if(tokenStatus == null) {
            throw new JwtException("전달된 Refresh Token은 만료되었습니다.");
        }
        else if(tokenStatus == false) {
            throw new JwtException("전달된 Refresh Token은 유효하지 않습니다.");
        }

        // Access Token에서 userId 가져오기
        Authentication authentication = tokenProvider.getAuthentication(accessToken);
        Long userId = Long.valueOf(authentication.getName());

        // DB의 사용자 Refresh Token 값과, 전달받은 Refresh Token의 불일치 여부 검사
        String dbRefreshToken = userRepository.findRefreshTokenById(userId);
        if(dbRefreshToken == null || !(dbRefreshToken.equals(refreshToken))) {
            throw new Exception400.TokenBadRequest("전달된 Refresh Token은 DB 데이터와 일치하지 않습니다.");
        }

        AuthDto.TokenResponse tokenResponseDto = tokenProvider.generateAccessTokenByRefreshToken(authentication, refreshToken);
        return tokenResponseDto;
    }


    // ========== 유틸성 메소드 ========== //

    private String toEncodePassword(String password) {  // DI된 passwordEncoder 의존성 인스턴스 변수를 사용하므로, static으로는 선언하지 않는것이 권장됨.
        return passwordEncoder.encode(password);
    }

    private static UsernamePasswordAuthenticationToken toAuthentication(String email, String password) {  // 반환된 객체로 아이디와 비밀번호가 일치하는지 검증하는 로직에 활용이 가능함.
        return new UsernamePasswordAuthenticationToken(email, password);
    }
}
