package com.example.reservation.service;

import com.example.reservation.domain.Member;
import com.example.reservation.dto.SignupForm;
import com.example.reservation.exception.DuplicateLoginIdException;
import com.example.reservation.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입. 가입한 회원의 id를 돌려준다.
     *
     * 1) 아이디 중복을 먼저 조회해서 친절한 메시지를 준다.
     * 2) 두 요청이 동시에 1)을 통과하는 경우는 DB의 UNIQUE 키(uk_member_login_id)가 막고,
     *    그때 나는 DuplicateKeyException도 같은 예외로 바꿔준다.
     */
    @Transactional
    public Long signup(SignupForm form) {
        if (memberMapper.existsByLoginId(form.getLoginId())) {
            throw new DuplicateLoginIdException(form.getLoginId());
        }

        Member member = Member.newUser(
                form.getLoginId(),
                passwordEncoder.encode(form.getPassword()),
                form.getName());

        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            throw new DuplicateLoginIdException(form.getLoginId());
        }
        return member.getId();
    }
}
