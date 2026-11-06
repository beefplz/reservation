package com.example.reservation.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * member 테이블과 1:1로 대응하는 객체.
 * MyBatis가 기본 생성자 + 필드 리플렉션으로 값을 채우므로 setter 없이 둔다.
 */
@Getter
@NoArgsConstructor
public class Member {

    private Long id;
    private String loginId;
    private String password;   // BCrypt 해시
    private String name;
    private Role role;
    private LocalDateTime createdAt;

    /** 신규 가입용. role은 항상 USER (ADMIN은 DB에서 직접 지정). */
    public static Member newUser(String loginId, String encodedPassword, String name) {
        Member m = new Member();
        m.loginId = loginId;
        m.password = encodedPassword;
        m.name = name;
        m.role = Role.USER;
        return m;
    }
}
