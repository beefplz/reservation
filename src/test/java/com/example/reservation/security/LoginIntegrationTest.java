package com.example.reservation.security;

import com.example.reservation.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.reservation.dto.SignupForm;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 실제 DB(reservation_test) + 실제 SecurityConfig 로 로그인과 권한 규칙을 검증한다.
 * MockMvc 는 테스트와 같은 스레드에서 돌아서, @Transactional 안에서 가입한 회원이 로그인 시에도 보이고 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired MemberService memberService;

    @BeforeEach
    void setUp() {
        SignupForm form = new SignupForm();
        form.setLoginId("tester01");
        form.setPassword("password123");
        form.setPasswordConfirm("password123");
        form.setName("홍길동");
        memberService.signup(form);
    }

    @Test
    @DisplayName("맞는 아이디/비밀번호면 로그인되고 / 로 이동")
    void login_success() throws Exception {
        mvc.perform(formLogin("/login").userParameter("loginId").user("tester01").password("password123"))
                .andExpect(authenticated().withUsername("tester01").withRoles("USER"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 /login?error")
    void login_wrongPassword() throws Exception {
        mvc.perform(formLogin("/login").userParameter("loginId").user("tester01").password("wrongpass"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("없는 아이디도 같은 /login?error (아이디 존재 여부를 알려주지 않음)")
    void login_unknownId() throws Exception {
        mvc.perform(formLogin("/login").userParameter("loginId").user("nobody99").password("password123"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("로그인 안 하고 / 에 가면 로그인 화면으로 리다이렉트")
    void home_requiresLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @DisplayName("로그아웃하면 /login?logout")
    void logout_redirect() throws Exception {
        mvc.perform(logout())
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @DisplayName("로그인 화면과 가입 화면은 로그인 없이 열린다")
    void loginAndSignupPages_permitAll() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/signup")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER 는 /admin/** 에 접근하면 403")
    void admin_forbiddenForUser() throws Exception {
        mvc.perform(get("/admin/rooms"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN 은 /admin/** 권한 검사를 통과한다")
    void admin_allowedForAdmin() throws Exception {
        mvc.perform(get("/admin/rooms"))
                .andExpect(status().isOk());
    }
}
