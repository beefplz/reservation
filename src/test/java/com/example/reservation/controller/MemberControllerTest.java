package com.example.reservation.controller;

import com.example.reservation.config.SecurityConfig;
import com.example.reservation.dto.SignupForm;
import com.example.reservation.exception.DuplicateLoginIdException;
import com.example.reservation.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 웹 계층만 띄워서 입력 검증과 화면 이동을 확인한다. 서비스는 Mock. */
@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
class MemberControllerTest {

    @Autowired MockMvc mvc;
    @MockBean MemberService memberService;

    @Test
    @DisplayName("가입 화면은 로그인 없이 열린다")
    void signupForm_permitAll() throws Exception {
        mvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/signup"));
    }

    @Test
    @DisplayName("정상 입력이면 로그인 화면으로 리다이렉트")
    void signup_success() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                        .param("loginId", "tester01")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("name", "홍길동"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verify(memberService).signup(any(SignupForm.class));
    }

    @Test
    @DisplayName("입력값이 잘못되면 가입 화면으로 돌아가고 서비스는 호출하지 않는다")
    void signup_invalidInput() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                        .param("loginId", "ab")              // 4자 미만
                        .param("password", "short")          // 8자 미만
                        .param("passwordConfirm", "different")
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("member/signup"))
                .andExpect(model().attributeHasFieldErrors("form",
                        "loginId", "password", "passwordMatching", "name"));
        verify(memberService, never()).signup(any());
    }

    @Test
    @DisplayName("중복 아이디면 loginId 필드 오류와 함께 가입 화면으로 돌아간다")
    void signup_duplicate() throws Exception {
        given(memberService.signup(any())).willThrow(new DuplicateLoginIdException("tester01"));

        mvc.perform(post("/signup").with(csrf())
                        .param("loginId", "tester01")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("name", "홍길동"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/signup"))
                .andExpect(model().attributeHasFieldErrorCode("form", "loginId", "duplicate"));
    }
}
