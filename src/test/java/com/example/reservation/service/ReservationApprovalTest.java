package com.example.reservation.service;

import com.example.reservation.domain.ReservationStatus;
import com.example.reservation.dto.ReservationView;
import com.example.reservation.exception.InvalidStatusChangeException;
import com.example.reservation.exception.ReservationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.example.reservation.service.ReservationTestSupport.insertMember;
import static com.example.reservation.service.ReservationTestSupport.insertReservation;
import static com.example.reservation.service.ReservationTestSupport.insertRoom;
import static com.example.reservation.service.ReservationTestSupport.statusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 관리자 승인/거절 규칙: PENDING 에서만, 승인은 시작 전에만. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationApprovalTest {

    @Autowired ReservationService reservationService;
    @Autowired JdbcTemplate jdbc;

    Long memberId;
    Long roomId;

    @BeforeEach
    void setUp() {
        memberId = insertMember(jdbc);
        roomId = insertRoom(jdbc);
    }

    @Test
    @DisplayName("PENDING 예약을 승인하면 APPROVED")
    void approve_success() {
        Long id = insertReservation(jdbc, memberId, roomId, 24, "PENDING");

        reservationService.approve(id);

        assertThat(statusOf(jdbc, id)).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("PENDING 예약을 거절하면 REJECTED")
    void reject_success() {
        Long id = insertReservation(jdbc, memberId, roomId, 24, "PENDING");

        reservationService.reject(id);

        assertThat(statusOf(jdbc, id)).isEqualTo("REJECTED");
    }

    @ParameterizedTest(name = "[{index}] {0} 예약은 승인 불가")
    @CsvSource({"APPROVED", "REJECTED", "CANCELED"})
    @DisplayName("PENDING 이 아니면 승인할 수 없다 (상태도 그대로)")
    void approve_notPending(String status) {
        Long id = insertReservation(jdbc, memberId, roomId, 24, status);

        assertThatThrownBy(() -> reservationService.approve(id))
                .isInstanceOf(InvalidStatusChangeException.class)
                .hasMessageContaining("이미");
        assertThat(statusOf(jdbc, id)).isEqualTo(status);
    }

    @ParameterizedTest(name = "[{index}] {0} 예약은 거절 불가")
    @CsvSource({"APPROVED", "REJECTED", "CANCELED"})
    @DisplayName("PENDING 이 아니면 거절할 수 없다 (승인된 예약을 거절로 뒤집지 않는다)")
    void reject_notPending(String status) {
        Long id = insertReservation(jdbc, memberId, roomId, 24, status);

        assertThatThrownBy(() -> reservationService.reject(id))
                .isInstanceOf(InvalidStatusChangeException.class);
        assertThat(statusOf(jdbc, id)).isEqualTo(status);
    }

    @Test
    @DisplayName("시작 시각이 지난 PENDING 은 승인할 수 없지만 거절은 된다")
    void pastPending_rejectOnly() {
        Long id = insertReservation(jdbc, memberId, roomId, -2, "PENDING");

        assertThatThrownBy(() -> reservationService.approve(id))
                .isInstanceOf(InvalidStatusChangeException.class)
                .hasMessageContaining("지난");
        assertThat(statusOf(jdbc, id)).isEqualTo("PENDING");

        reservationService.reject(id);
        assertThat(statusOf(jdbc, id)).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("없는 예약이면 NotFound")
    void unknown() {
        assertThatThrownBy(() -> reservationService.approve(-1L)).isInstanceOf(ReservationNotFoundException.class);
        assertThatThrownBy(() -> reservationService.reject(-1L)).isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @DisplayName("관리자 목록은 상태별로 거를 수 있고, 신청자 정보가 함께 나온다")
    void findForAdmin_filter() {
        Long pending = insertReservation(jdbc, memberId, roomId, 24, "PENDING");
        Long approved = insertReservation(jdbc, memberId, roomId, 48, "APPROVED");

        assertThat(reservationService.findForAdmin(ReservationStatus.PENDING))
                .extracting(ReservationView::getId).contains(pending).doesNotContain(approved);
        assertThat(reservationService.findForAdmin(null))
                .extracting(ReservationView::getId).contains(pending, approved);

        ReservationView row = reservationService.findForAdmin(ReservationStatus.PENDING).stream()
                .filter(v -> v.getId().equals(pending)).findFirst().orElseThrow();
        assertThat(row.getMemberName()).isEqualTo("테스터");
        assertThat(row.getLoginId()).startsWith("rsv");
    }
}
