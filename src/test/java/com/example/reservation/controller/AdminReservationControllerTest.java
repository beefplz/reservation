package com.example.reservation.controller;

import org.junit.jupiter.api.BeforeEach;
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

import static com.example.reservation.service.ReservationTestSupport.insertMember;
import static com.example.reservation.service.ReservationTestSupport.insertReservation;
import static com.example.reservation.service.ReservationTestSupport.insertRoom;
import static com.example.reservation.service.ReservationTestSupport.statusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 관리자 예약 화면의 권한과 흐름. 실제 SecurityConfig + 실제 DB. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminReservationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    Long reservationId;

    @BeforeEach
    void setUp() {
        reservationId = insertReservation(jdbc, insertMember(jdbc), insertRoom(jdbc), 24, "PENDING");
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER 는 예약 관리 화면, 승인, 거절 모두 403 (상태도 그대로)")
    void user_forbidden() throws Exception {
        mvc.perform(get("/admin/reservations")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/reservations/" + reservationId + "/approve").with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/reservations/" + reservationId + "/reject").with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(statusOf(jdbc, reservationId)).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("로그인 안 하면 로그인 화면으로")
    void anonymous_redirectToLogin() throws Exception {
        mvc.perform(get("/admin/reservations")).andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN 목록: JSP 로 forward, status 로 거르기")
    void admin_list() throws Exception {
        mvc.perform(get("/admin/reservations").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/WEB-INF/views/admin/reservation/list.jsp"))
                .andExpect(model().attribute("reservations",
                        everyItem(hasProperty("status", is(com.example.reservation.domain.ReservationStatus.PENDING)))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("없는 상태 이름으로 거르면 400")
    void admin_list_badStatus() throws Exception {
        mvc.perform(get("/admin/reservations").param("status", "WRONG"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN 승인 → APPROVED, 승인 대기 목록으로")
    void admin_approve() throws Exception {
        mvc.perform(post("/admin/reservations/" + reservationId + "/approve").with(csrf()))
                .andExpect(redirectedUrl("/admin/reservations?status=PENDING"))
                .andExpect(flash().attribute("successMessage", "예약을 승인했습니다."));
        assertThat(statusOf(jdbc, reservationId)).isEqualTo("APPROVED");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN 거절 → REJECTED")
    void admin_reject() throws Exception {
        mvc.perform(post("/admin/reservations/" + reservationId + "/reject").with(csrf()))
                .andExpect(redirectedUrl("/admin/reservations?status=PENDING"));
        assertThat(statusOf(jdbc, reservationId)).isEqualTo("REJECTED");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("이미 처리된 예약을 다시 승인하면 이유를 보여준다")
    void admin_approveTwice() throws Exception {
        mvc.perform(post("/admin/reservations/" + reservationId + "/approve").with(csrf()));

        mvc.perform(post("/admin/reservations/" + reservationId + "/approve").with(csrf()))
                .andExpect(flash().attribute("errorMessage", "이미 '승인' 상태인 예약입니다."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("없는 예약 승인은 404, CSRF 토큰 없으면 403")
    void admin_notFound_and_csrf() throws Exception {
        mvc.perform(post("/admin/reservations/-1/approve").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/reservations/" + reservationId + "/approve")).andExpect(status().isForbidden());
        assertThat(statusOf(jdbc, reservationId)).isEqualTo("PENDING");
    }
}
