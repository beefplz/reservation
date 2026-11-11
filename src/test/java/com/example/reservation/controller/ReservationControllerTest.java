package com.example.reservation.controller;

import com.example.reservation.security.LoginMember;
import com.example.reservation.security.MemberUserDetailsService;
import com.example.reservation.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static com.example.reservation.service.ReservationTestSupport.DAY;
import static com.example.reservation.service.ReservationTestSupport.insertMember;
import static com.example.reservation.service.ReservationTestSupport.insertRoom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 실제 SecurityConfig + 실제 DB 로 예약 화면의 흐름과 권한을 검증한다.
 * @AuthenticationPrincipal 로 LoginMember 를 받으므로, @WithMockUser 대신 실제 LoginMember 로 로그인시킨다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReservationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired MemberUserDetailsService userDetailsService;
    @Autowired ReservationService reservationService;

    Long memberId;
    Long roomId;
    LoginMember loginMember;

    @BeforeEach
    void setUp() {
        memberId = insertMember(jdbc);
        roomId = insertRoom(jdbc);
        String loginId = jdbc.queryForObject("SELECT login_id FROM member WHERE id = ?", String.class, memberId);
        loginMember = (LoginMember) userDetailsService.loadUserByUsername(loginId);
    }

    @Test
    @DisplayName("로그인 안 하면 예약 화면은 로그인 화면으로")
    void requiresLogin() throws Exception {
        mvc.perform(get("/reservations/new")).andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(get("/reservations/my")).andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @DisplayName("신청 화면은 JSP 로 forward 되고, 선택한 회의실이 미리 채워진다")
    void createForm() throws Exception {
        mvc.perform(get("/reservations/new").param("roomId", roomId.toString()).with(user(loginMember)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/WEB-INF/views/reservation/form.jsp"))
                .andExpect(model().attribute("form", org.hamcrest.Matchers.hasProperty("roomId",
                        org.hamcrest.Matchers.is(roomId))))
                .andExpect(model().attributeExists("rooms"));
    }

    @Test
    @DisplayName("신청하면 내 예약으로 리다이렉트되고, 신청자는 로그인한 본인이다 (폼의 memberId 는 무시)")
    void create_success_memberFromLogin() throws Exception {
        Long otherMemberId = insertMember(jdbc);

        mvc.perform(validRequest("10:00", "11:00").param("memberId", otherMemberId.toString()))
                .andExpect(redirectedUrl("/reservations/my"))
                .andExpect(flash().attributeExists("successMessage"));

        Long savedMemberId = jdbc.queryForObject(
                "SELECT member_id FROM reservation WHERE room_id = ?", Long.class, roomId);
        assertThat(savedMemberId).isEqualTo(memberId);
    }

    @Test
    @DisplayName("겹치는 시간이면 신청 화면으로 돌아가 이유를 보여준다")
    void create_overlap() throws Exception {
        mvc.perform(validRequest("10:00", "11:00")).andExpect(redirectedUrl("/reservations/my"));

        mvc.perform(validRequest("10:30", "11:30"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/WEB-INF/views/reservation/form.jsp"))
                .andExpect(model().attribute("errorMessage", "이미 예약된 시간입니다. 다른 시간을 선택하세요."));
    }

    @Test
    @DisplayName("시간 규칙을 어기면 이유를 보여준다")
    void create_invalidTime() throws Exception {
        mvc.perform(validRequest("11:00", "10:00"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "종료 시각은 시작 시각보다 늦어야 합니다."));
    }

    @Test
    @DisplayName("필수값이 비면 필드 오류와 함께 돌아간다")
    void create_missingFields() throws Exception {
        mvc.perform(post("/reservations").with(csrf()).with(user(loginMember))
                        .param("roomId", "").param("date", "").param("startTime", "")
                        .param("endTime", "").param("purpose", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "roomId", "date", "startTime", "endTime", "purpose"));
    }

    @Test
    @DisplayName("CSRF 토큰 없는 신청은 403")
    void create_withoutCsrf() throws Exception {
        mvc.perform(post("/reservations").with(user(loginMember))
                        .param("roomId", roomId.toString()).param("date", DAY.toString())
                        .param("startTime", "10:00").param("endTime", "11:00").param("purpose", "회의"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내 예약 화면에는 본인 예약만 넘어간다")
    void myReservations_onlyMine() throws Exception {
        Long otherMemberId = insertMember(jdbc);
        reservationService.create(otherMemberId,
                com.example.reservation.service.ReservationTestSupport.form(roomId, "13:00", "14:00"));
        mvc.perform(validRequest("10:00", "11:00"));

        mvc.perform(get("/reservations/my").with(user(loginMember)))
                .andExpect(forwardedUrl("/WEB-INF/views/reservation/my.jsp"))
                .andExpect(model().attribute("reservations", hasSize(1)));
    }

    @Test
    @DisplayName("본인 예약을 취소하면 내 예약으로 돌아가고 상태가 CANCELED")
    void cancel_success() throws Exception {
        Long id = reservationService.create(memberId,
                com.example.reservation.service.ReservationTestSupport.form(roomId, "10:00", "11:00"));

        mvc.perform(post("/reservations/" + id + "/cancel").with(csrf()).with(user(loginMember)))
                .andExpect(redirectedUrl("/reservations/my"))
                .andExpect(flash().attribute("successMessage", "예약을 취소했습니다."));
        assertThat(jdbc.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, id))
                .isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("남의 예약 취소 요청은 404")
    void cancel_othersReservation_404() throws Exception {
        Long otherMemberId = insertMember(jdbc);
        Long id = reservationService.create(otherMemberId,
                com.example.reservation.service.ReservationTestSupport.form(roomId, "10:00", "11:00"));

        mvc.perform(post("/reservations/" + id + "/cancel").with(csrf()).with(user(loginMember)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이미 취소한 예약을 다시 취소하면 이유를 보여준다")
    void cancel_twice() throws Exception {
        Long id = reservationService.create(memberId,
                com.example.reservation.service.ReservationTestSupport.form(roomId, "10:00", "11:00"));
        reservationService.cancel(memberId, id);

        mvc.perform(post("/reservations/" + id + "/cancel").with(csrf()).with(user(loginMember)))
                .andExpect(redirectedUrl("/reservations/my"))
                .andExpect(flash().attribute("errorMessage", "이미 '취소' 상태인 예약입니다."));
    }

    @Test
    @DisplayName("CSRF 토큰 없는 취소 요청은 403")
    void cancel_withoutCsrf() throws Exception {
        Long id = reservationService.create(memberId,
                com.example.reservation.service.ReservationTestSupport.form(roomId, "10:00", "11:00"));

        mvc.perform(post("/reservations/" + id + "/cancel").with(user(loginMember)))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder validRequest(String start, String end) {
        return post("/reservations").with(csrf()).with(user(loginMember))
                .param("roomId", roomId.toString())
                .param("date", DAY.toString())
                .param("startTime", start)
                .param("endTime", end)
                .param("purpose", "주간 회의");
    }
}
