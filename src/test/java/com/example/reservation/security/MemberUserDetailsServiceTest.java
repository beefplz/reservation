package com.example.reservation.security;

import com.example.reservation.domain.Member;
import com.example.reservation.domain.Role;
import com.example.reservation.mapper.MemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberUserDetailsServiceTest {

    @Mock MemberMapper memberMapper;
    @InjectMocks MemberUserDetailsService service;

    @Test
    @DisplayName("USER 회원은 ROLE_USER 권한을 가진다")
    void user_hasRoleUser() {
        given(memberMapper.findByLoginId("tester01")).willReturn(Optional.of(member("tester01", Role.USER)));

        UserDetails user = service.loadUserByUsername("tester01");

        assertThat(user.getUsername()).isEqualTo("tester01");
        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("ADMIN 회원은 ROLE_ADMIN 권한을 가진다")
    void admin_hasRoleAdmin() {
        given(memberMapper.findByLoginId("admin01")).willReturn(Optional.of(member("admin01", Role.ADMIN)));

        UserDetails user = service.loadUserByUsername("admin01");

        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("없는 아이디면 UsernameNotFoundException")
    void unknown_throws() {
        given(memberMapper.findByLoginId("nobody")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    /** Member 는 setter 가 없어서(가입은 항상 USER) 테스트에서만 role 을 직접 바꾼다. */
    private static Member member(String loginId, Role role) {
        Member m = Member.newUser(loginId, "hashed", "이름");
        ReflectionTestUtils.setField(m, "id", 1L);
        ReflectionTestUtils.setField(m, "role", role);
        return m;
    }
}
