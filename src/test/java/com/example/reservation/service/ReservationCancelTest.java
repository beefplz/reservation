package com.example.reservation.service;

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

import static com.example.reservation.service.ReservationTestSupport.form;
import static com.example.reservation.service.ReservationTestSupport.insertMember;
import static com.example.reservation.service.ReservationTestSupport.insertReservation;
import static com.example.reservation.service.ReservationTestSupport.insertRoom;
import static com.example.reservation.service.ReservationTestSupport.statusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 예약 취소 규칙: 본인 것만, PENDING/APPROVED 만, 시작 전에만. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationCancelTest {

    @Autowired ReservationService reservationService;
    @Autowired JdbcTemplate jdbc;

    Long memberId;
    Long roomId;

    @BeforeEach
    void setUp() {
        memberId = insertMember(jdbc);
        roomId = insertRoom(jdbc);
    }

    @ParameterizedTest(name = "[{index}] {0} 예약은 취소할 수 있다")
    @CsvSource({"PENDING", "APPROVED"})
    @DisplayName("시작 전인 본인의 PENDING/APPROVED 예약은 취소된다")
    void cancel_success(String status) {
        Long id = insertReservation(jdbc, memberId, roomId, 24, status);

        reservationService.cancel(memberId, id);

        assertThat(statusOf(jdbc, id)).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("취소하면 그 시간이 비어서 다른 사람이 같은 시간에 신청할 수 있다")
    void cancel_freesSlot() {
        Long id = reservationService.create(memberId, form(roomId, "10:00", "11:00"));
        reservationService.cancel(memberId, id);

        Long otherMemberId = insertMember(jdbc);
        assertThatCode(() -> reservationService.create(otherMemberId, form(roomId, "10:00", "11:00")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("남의 예약은 취소할 수 없고, 존재 여부를 숨기기 위해 NotFound")
    void cancel_othersReservation() {
        Long otherMemberId = insertMember(jdbc);
        Long id = insertReservation(jdbc, otherMemberId, roomId, 24, "PENDING");

        assertThatThrownBy(() -> reservationService.cancel(memberId, id))
                .isInstanceOf(ReservationNotFoundException.class);
        assertThat(statusOf(jdbc, id)).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("없는 예약이면 NotFound")
    void cancel_unknown() {
        assertThatThrownBy(() -> reservationService.cancel(memberId, -1L))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @ParameterizedTest(name = "[{index}] 이미 {0} 인 예약은 취소 불가")
    @CsvSource({"CANCELED", "REJECTED"})
    @DisplayName("이미 취소/거절된 예약은 취소할 수 없다")
    void cancel_alreadyClosed(String status) {
        Long id = insertReservation(jdbc, memberId, roomId, 24, status);

        assertThatThrownBy(() -> reservationService.cancel(memberId, id))
                .isInstanceOf(InvalidStatusChangeException.class)
                .hasMessageContaining("이미");
        assertThat(statusOf(jdbc, id)).isEqualTo(status);
    }

    @Test
    @DisplayName("이미 시작했거나 지난 예약은 취소할 수 없다")
    void cancel_started() {
        Long past = insertReservation(jdbc, memberId, roomId, -3, "APPROVED");
        Long ongoing = insertReservation(jdbc, memberId, roomId, 0, "APPROVED");   // 지금 막 시작

        assertThatThrownBy(() -> reservationService.cancel(memberId, past))
                .isInstanceOf(InvalidStatusChangeException.class)
                .hasMessageContaining("시작");
        assertThatThrownBy(() -> reservationService.cancel(memberId, ongoing))
                .isInstanceOf(InvalidStatusChangeException.class);
    }
}
