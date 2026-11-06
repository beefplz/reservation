package com.example.reservation.service;

import com.example.reservation.domain.Member;
import com.example.reservation.exception.DuplicateLoginIdException;
import com.example.reservation.mapper.MemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/**
 * 동시 가입 상황은 실제 DB로 재현하기 어려워서 Mock으로 검증한다.
 * "중복 조회는 통과했는데 INSERT에서 UNIQUE 키에 걸린" 경우를 흉내 낸다.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceUnitTest {

    @Mock MemberMapper memberMapper;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks MemberService memberService;

    @Test
    @DisplayName("동시 가입으로 UNIQUE 키 위반이 나도 DuplicateLoginIdException으로 바꿔준다")
    void signup_raceCondition_uniqueKeyViolation() {
        given(memberMapper.existsByLoginId("tester01")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("hashed");
        willThrow(new DuplicateKeyException("uk_member_login_id"))
                .given(memberMapper).insert(any(Member.class));

        assertThatThrownBy(() -> memberService.signup(MemberServiceTest.form("tester01", "password123", "홍길동")))
                .isInstanceOf(DuplicateLoginIdException.class);
    }
}
