package com.example.reservation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 SecurityConfig + 실제 DB 로 회의실 화면의 권한과 흐름을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoomAccessIntegrationTest {

    private static final String ROOM_NAME = "권한테스트룸";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("로그인 안 하면 /rooms 는 로그인 화면으로")
    void rooms_requiresLogin() throws Exception {
        mvc.perform(get("/rooms"))
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER 는 회의실 목록을 볼 수 있고, JSP 로 forward 된다")
    void rooms_userCanList() throws Exception {
        // MockMvc 는 JSP 를 실제로 그리지 않고 forward 주소만 기록한다.
        // → JSP 뷰 리졸버(prefix/suffix)가 맞게 동작하는지는 forwardedUrl 로 확인
        mvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(view().name("room/list"))
                .andExpect(forwardedUrl("/WEB-INF/views/room/list.jsp"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER 는 관리자 화면과 변경 요청이 모두 403")
    void admin_forbiddenForUser() throws Exception {
        Long id = insertRoom();

        mvc.perform(get("/admin/rooms")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/rooms/new")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/rooms").with(csrf())
                        .param("name", "몰래등록").param("location", "x").param("capacity", "5"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/rooms/" + id + "/deactivate").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN 이 등록하면 관리 목록과 사용자 목록에 보인다")
    void admin_create() throws Exception {
        mvc.perform(post("/admin/rooms").with(csrf())
                        .param("name", ROOM_NAME).param("location", "별관").param("capacity", "5"))
                .andExpect(redirectedUrl("/admin/rooms"));

        mvc.perform(get("/admin/rooms")).andExpect(content().string(containsString(ROOM_NAME)));
        // /rooms 는 JSP 라 응답 본문이 비어 있으므로, 화면에 넘기는 모델(rooms)로 확인
        mvc.perform(get("/rooms"))
                .andExpect(model().attribute("rooms", hasItem(hasProperty("name", is(ROOM_NAME)))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("입력값이 잘못되면 등록 화면으로 돌아간다")
    void admin_create_invalid() throws Exception {
        mvc.perform(post("/admin/rooms").with(csrf())
                        .param("name", "").param("location", "").param("capacity", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/room/form"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "location", "capacity"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("이름이 중복되면 name 필드 오류와 함께 돌아간다")
    void admin_create_duplicate() throws Exception {
        insertRoom();

        mvc.perform(post("/admin/rooms").with(csrf())
                        .param("name", ROOM_NAME).param("location", "별관").param("capacity", "5"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("form", "name", "duplicate"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("비활성화하면 사용자 목록에서 빠지고 관리 목록에는 남는다")
    void admin_deactivate() throws Exception {
        Long id = insertRoom();

        mvc.perform(post("/admin/rooms/" + id + "/deactivate").with(csrf()))
                .andExpect(redirectedUrl("/admin/rooms"));

        mvc.perform(get("/rooms"))
                .andExpect(model().attribute("rooms", not(hasItem(hasProperty("name", is(ROOM_NAME))))));
        mvc.perform(get("/admin/rooms")).andExpect(content().string(containsString(ROOM_NAME)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("CSRF 토큰 없는 비활성화 요청은 403")
    void admin_deactivate_withoutCsrf() throws Exception {
        Long id = insertRoom();

        mvc.perform(post("/admin/rooms/" + id + "/deactivate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("없는 회의실 수정 화면은 404")
    void admin_edit_notFound() throws Exception {
        mvc.perform(get("/admin/rooms/-1/edit"))
                .andExpect(status().isNotFound());
    }

    private Long insertRoom() {
        jdbc.update("INSERT INTO room (name, location, capacity) VALUES (?, '본관', 5)", ROOM_NAME);
        return jdbc.queryForObject("SELECT id FROM room WHERE name = ?", Long.class, ROOM_NAME);
    }
}
