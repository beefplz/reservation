package com.example.reservation.service;

import com.example.reservation.domain.ReservationStatus;
import com.example.reservation.dto.ReservationView;
import com.example.reservation.exception.ReservationOverlapException;
import com.example.reservation.exception.RoomNotAvailableException;
import com.example.reservation.exception.RoomNotFoundException;
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

import java.util.List;

import static com.example.reservation.service.ReservationTestSupport.form;
import static com.example.reservation.service.ReservationTestSupport.insertMember;
import static com.example.reservation.service.ReservationTestSupport.insertRoom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실제 MySQL(reservation_test)에서 중복 예약 규칙을 검증한다. 각 테스트는 롤백된다. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationServiceTest {

    @Autowired ReservationService reservationService;
    @Autowired RoomService roomService;
    @Autowired JdbcTemplate jdbc;

    Long memberId;
    Long roomId;

    @BeforeEach
    void setUp() {
        memberId = insertMember(jdbc);
        roomId = insertRoom(jdbc);
    }

    @Test
    @DisplayName("신청하면 PENDING 으로 저장되고 내 예약 목록에 보인다")
    void create_success() {
        Long id = reservationService.create(memberId, form(roomId, "10:00", "11:00"));

        List<ReservationView> mine = reservationService.findMyReservations(memberId);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getId()).isEqualTo(id);
        assertThat(mine.get(0).getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(mine.get(0).getRoomName()).startsWith("예약테스트룸");
        assertThat(mine.get(0).getTimeRange()).endsWith("10:00 ~ 11:00");
    }

    /** 기존 예약 10:00~11:00 에 대해 새 신청이 겹치는 여러 경우 */
    @ParameterizedTest(name = "[{index}] {0}~{1} 은 거부")
    @CsvSource({
            "10:00, 11:00",   // 완전히 같음
            "09:30, 10:30",   // 앞쪽이 겹침
            "10:30, 11:30",   // 뒤쪽이 겹침
            "09:00, 12:00",   // 기존 예약을 감쌈
    })
    @DisplayName("시간이 겹치면 거부")
    void create_overlap(String start, String end) {
        reservationService.create(memberId, form(roomId, "10:00", "11:00"));

        assertThatThrownBy(() -> reservationService.create(memberId, form(roomId, start, end)))
                .isInstanceOf(ReservationOverlapException.class);
    }

    @Test
    @DisplayName("기존 예약 안에 포함되는 신청도 거부")
    void create_overlap_inside() {
        reservationService.create(memberId, form(roomId, "10:00", "12:00"));

        assertThatThrownBy(() -> reservationService.create(memberId, form(roomId, "10:30", "11:30")))
                .isInstanceOf(ReservationOverlapException.class);
    }

    @Test
    @DisplayName("경계가 맞닿는 예약(앞뒤로 붙은 시간)은 허용")
    void create_adjacent_allowed() {
        reservationService.create(memberId, form(roomId, "10:00", "11:00"));

        assertThatCode(() -> reservationService.create(memberId, form(roomId, "11:00", "12:00")))
                .doesNotThrowAnyException();
        assertThatCode(() -> reservationService.create(memberId, form(roomId, "09:00", "10:00")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 회의실은 같은 시간이어도 허용")
    void create_otherRoom_allowed() {
        reservationService.create(memberId, form(roomId, "10:00", "11:00"));
        Long otherRoomId = insertRoom(jdbc);

        assertThatCode(() -> reservationService.create(memberId, form(otherRoomId, "10:00", "11:00")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("APPROVED 예약과 겹쳐도 거부")
    void create_overlapWithApproved() {
        Long id = reservationService.create(memberId, form(roomId, "10:00", "11:00"));
        setStatus(id, "APPROVED");

        assertThatThrownBy(() -> reservationService.create(memberId, form(roomId, "10:00", "11:00")))
                .isInstanceOf(ReservationOverlapException.class);
    }

    @ParameterizedTest(name = "[{index}] {0} 예약은 시간을 차지하지 않는다")
    @CsvSource({"CANCELED", "REJECTED"})
    @DisplayName("취소/거절된 예약과는 겹쳐도 허용")
    void create_overlapWithCanceledOrRejected_allowed(String status) {
        Long id = reservationService.create(memberId, form(roomId, "10:00", "11:00"));
        setStatus(id, status);

        assertThatCode(() -> reservationService.create(memberId, form(roomId, "10:00", "11:00")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비활성 회의실은 거부")
    void create_inactiveRoom() {
        roomService.deactivate(roomId);

        assertThatThrownBy(() -> reservationService.create(memberId, form(roomId, "10:00", "11:00")))
                .isInstanceOf(RoomNotAvailableException.class);
    }

    @Test
    @DisplayName("없는 회의실은 RoomNotFoundException")
    void create_unknownRoom() {
        assertThatThrownBy(() -> reservationService.create(memberId, form(-1L, "10:00", "11:00")))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    @DisplayName("내 예약 목록에는 다른 회원의 예약이 보이지 않는다")
    void findMyReservations_onlyMine() {
        Long otherMemberId = insertMember(jdbc);
        reservationService.create(otherMemberId, form(roomId, "10:00", "11:00"));

        assertThat(reservationService.findMyReservations(memberId)).isEmpty();
        assertThat(reservationService.findMyReservations(otherMemberId)).hasSize(1);
    }

    private void setStatus(Long reservationId, String status) {
        jdbc.update("UPDATE reservation SET status = ? WHERE id = ?", status, reservationId);
    }
}
