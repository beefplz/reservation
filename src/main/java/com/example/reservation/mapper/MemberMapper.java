package com.example.reservation.mapper;

import com.example.reservation.domain.Member;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface MemberMapper {

    /** 저장 후 생성된 id를 member.id에 채워준다 (useGeneratedKeys). */
    void insert(Member member);

    boolean existsByLoginId(String loginId);

    Optional<Member> findByLoginId(String loginId);
}
