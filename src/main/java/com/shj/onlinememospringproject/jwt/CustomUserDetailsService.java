package com.shj.onlinememospringproject.jwt;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.response.exception.Exception404;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Transactional
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 이 파라미터의 username은 로그인Email을 의미.

        return userRepository.findByEmail(username)
                .map(this::createUserDetails)
                .orElseThrow(() -> new Exception404.NoSuchUser(String.format("email = %s", username)));
    }

    private UserDetails createUserDetails(User user) {
        // 로그인아이디를 이용하여 User을 찾고, 찾은 User의 '사용자DB의PKid,비밀번호,권한'을 가지고 UserDetails 객체를 생성한다.

        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(user.getAuthority().toString());

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),  // 로그인Email 대신 User_PKid를 String으로 변환하여 집어넣음.
                user.getPassword(),
                Collections.singleton(grantedAuthority)
        );
    }
}