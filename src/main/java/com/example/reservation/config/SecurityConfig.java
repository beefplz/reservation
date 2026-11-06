package com.example.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 로그인 없이 접근 가능한 경로
                        .requestMatchers("/signup", "/login", "/css/**", "/js/**", "/error").permitAll()
                        // 관리자 기능(회의실 관리, 예약 승인)은 ADMIN 만
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")              // 직접 만든 화면 (GET /login → LoginController)
                        .usernameParameter("loginId")     // 폼의 아이디 input name
                        // POST /login 처리는 Security 가 한다. 실패 시 기본값 /login?error 로 이동.
                        // 성공 시: 로그인 전에 가려던 페이지가 있으면 그곳, 없으면 "/"
                        .defaultSuccessUrl("/"))
                // POST /logout (CSRF 토큰 필요). 성공 시 기본값 /login?logout 로 이동.
                .logout(Customizer.withDefaults());
        return http.build();
    }
}
