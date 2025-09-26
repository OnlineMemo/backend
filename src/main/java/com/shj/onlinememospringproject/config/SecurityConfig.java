package com.shj.onlinememospringproject.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shj.onlinememospringproject.jwt.JwtFilter;
import com.shj.onlinememospringproject.jwt.TokenProvider;
import com.shj.onlinememospringproject.jwt.handler.JwtAccessDeniedHandler;
import com.shj.onlinememospringproject.jwt.handler.JwtAuthenticationEntryPoint;
import com.shj.onlinememospringproject.jwt.handler.JwtExceptionFilter;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger/**"};

    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Value("${server.env}")
    private String serverEnv;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> {
                    sessionManagement
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(authorizeRequests -> {
                    authorizeRequests
                            // < default >
                            // .requestMatchers("/**").permitAll()  // Test 용도
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/").denyAll()  // "/"에 대한 POST 요청을 막음. (이처럼 위쪽에 작성해야 정상 적용가능.)

                            // < All (User, Admin) >
                            .requestMatchers("/", "/error", "/favicon.ico", "/webjars/**", "/health", "/test").permitAll()
                            .requestMatchers(serverEnv.equals("prod") ? new String[]{} : SWAGGER_PATHS).permitAll()  // 프로덕션 환경의 경우, Swagger 401 응답 처리함.
                            .requestMatchers("/login", "/signup", "/password", "/reissue").permitAll()

                            // < Admin >
                            .requestMatchers("/back-office/**").hasAuthority("ROLE_ADMIN")

                            .anyRequest().hasAnyAuthority("ROLE_USER", "ROLE_ADMIN");  // permit 지정한 경로들 외에는 전부 USER나 ADMIN 권한이 있어야지 URI를 이용 가능함.
                })

                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling
                            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                            .accessDeniedHandler(jwtAccessDeniedHandler);
                })

                .addFilterBefore(new JwtFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtExceptionFilter(objectMapper), JwtFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> corsList = Arrays.asList("*");  // "http://localhost:3000"
        if(serverEnv.equals("prod")) corsList = Arrays.asList("https://www.onlinememo.kr");

        config.setAllowedOriginPatterns(corsList);
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean  // Security와 직접적인 관련은 없으나, 이 또한 웹요청 파서이므로 이곳에 빈을 등록함.
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer.newBuilder()
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.DEVICE_CLASS)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .hideMatcherLoadStats()
                .withCache(500)
                .build();
    }
}
