package com.example.reservation.security;

import com.example.reservation.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 로그인 시 Spring Security 가 호출한다.
 * 여기서는 회원을 "찾기만" 하고, 비밀번호 비교는 Security 가 PasswordEncoder(BCrypt)로 한다.
 */
@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberMapper memberMapper;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        return memberMapper.findByLoginId(loginId)
                .map(LoginMember::new)
                .orElseThrow(() -> new UsernameNotFoundException(loginId));
    }
}
