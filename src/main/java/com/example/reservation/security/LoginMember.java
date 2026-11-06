package com.example.reservation.security;

import com.example.reservation.domain.Member;
import com.example.reservation.domain.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 로그인한 회원 정보. 세션에 저장되고, 컨트롤러에서 @AuthenticationPrincipal 로 꺼내 쓴다.
 * 예약 기능에서 member_id 를 넣을 때 getId() 를 사용한다.
 */
@Getter
public class LoginMember implements UserDetails {

    private final Long id;
    private final String loginId;
    private final String password;
    private final String name;
    private final Role role;

    public LoginMember(Member member) {
        this.id = member.getId();
        this.loginId = member.getLoginId();
        this.password = member.getPassword();
        this.name = member.getName();
        this.role = member.getRole();
    }

    /** Spring Security 규칙상 권한 이름 앞에 "ROLE_" 을 붙인다. hasRole("ADMIN") 이 ROLE_ADMIN 을 찾는다. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
