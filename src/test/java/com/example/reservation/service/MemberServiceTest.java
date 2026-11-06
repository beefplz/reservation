package com.example.reservation.service;

import com.example.reservation.domain.Member;
import com.example.reservation.domain.Role;
import com.example.reservation.dto.SignupForm;
import com.example.reservation.exception.DuplicateLoginIdException;
import com.example.reservation.mapper.MemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 MySQL(reservation_test)에 붙여서 서비스 + 매퍼 + SQL을 함께 검증한다.
 * 각 테스트는 @Transactional 로 끝나면 롤백되어 데이터가 남지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberMapper memberMapper;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("가입하면 USER 권한으로 저장되고 비밀번호는 BCrypt 해시로 저장된다")
    void signup_success() {
        Long id = memberService.signup(form("tester01", "password123", "홍길동"));

        Member saved = memberMapper.findByLoginId("tester01").orElseThrow();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getName()).isEqualTo("홍길동");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 있는 아이디로 가입하면 DuplicateLoginIdException")
    void signup_duplicateLoginId() {
        memberService.signup(form("tester01", "password123", "홍길동"));

        assertThatThrownBy(() -> memberService.signup(form("tester01", "otherpass99", "김철수")))
                .isInstanceOf(DuplicateLoginIdException.class);
    }

    static SignupForm form(String loginId, String password, String name) {
        SignupForm f = new SignupForm();
        f.setLoginId(loginId);
        f.setPassword(password);
        f.setPasswordConfirm(password);
        f.setName(name);
        return f;
    }
}
