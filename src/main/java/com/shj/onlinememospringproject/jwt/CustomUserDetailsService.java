package com.shj.onlinememospringproject.jwt;

import com.shj.onlinememospringproject.domain.User;
import com.shj.onlinememospringproject.repository.UserRepository;
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
    public UserDetails loadUserByUsername(String username) {
        // 이 파라미터의 username은 로그인Email을 의미.

        return userRepository.findByEmail(username)
                .map(this::createUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("email = %s", username)));
                // !!! 하지만 이는 BadCredentialsException 예외로 다시 변환되어 throw 되므로, 여기서 위의 email 로그는 띄워지지않음 !!!
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