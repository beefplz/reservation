package com.example.reservation.service;

import com.example.reservation.domain.Room;
import com.example.reservation.dto.RoomForm;
import com.example.reservation.exception.DuplicateRoomNameException;
import com.example.reservation.exception.RoomHasUpcomingReservationsException;
import com.example.reservation.exception.RoomNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실제 MySQL(reservation_test)에서 검증하고, 각 테스트가 끝나면 롤백한다. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoomServiceTest {

    @Autowired RoomService roomService;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("등록하면 활성 상태로 저장된다")
    void create_success() {
        Long id = roomService.create(form("테스트룸", "별관 1층", 8));

        Room saved = roomService.getRoom(id);
        assertThat(saved.getName()).isEqualTo("테스트룸");
        assertThat(saved.getLocation()).isEqualTo("별관 1층");
        assertThat(saved.getCapacity()).isEqualTo(8);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("이미 있는 이름으로 등록하면 거부")
    void create_duplicateName() {
        roomService.create(form("테스트룸", "별관 1층", 8));

        assertThatThrownBy(() -> roomService.create(form("테스트룸", "별관 2층", 4)))
                .isInstanceOf(DuplicateRoomNameException.class);
    }

    @Test
    @DisplayName("수정하면 값이 바뀐다. 자기 이름을 그대로 둬도 중복이 아니다")
    void update_success() {
        Long id = roomService.create(form("테스트룸", "별관 1층", 8));

        roomService.update(id, form("테스트룸", "별관 3층", 12));

        Room saved = roomService.getRoom(id);
        assertThat(saved.getLocation()).isEqualTo("별관 3층");
        assertThat(saved.getCapacity()).isEqualTo(12);
    }

    @Test
    @DisplayName("다른 회의실의 이름으로 수정하면 거부")
    void update_duplicateName() {
        roomService.create(form("테스트룸1", "별관 1층", 8));
        Long id = roomService.create(form("테스트룸2", "별관 2층", 8));

        assertThatThrownBy(() -> roomService.update(id, form("테스트룸1", "별관 2층", 8)))
                .isInstanceOf(DuplicateRoomNameException.class);
    }

    @Test
    @DisplayName("비활성화하면 행은 남고 사용자 목록에서만 빠진다. 재활성화하면 돌아온다")
    void deactivate_and_activate() {
        Long id = roomService.create(form("테스트룸", "별관 1층", 8));

        roomService.deactivate(id);
        assertThat(roomService.getRoom(id).isActive()).isFalse();
        assertThat(roomService.findActiveRooms()).extracting(Room::getId).doesNotContain(id);
        assertThat(roomService.findAllRooms()).extracting(Room::getId).contains(id);

        roomService.activate(id);
        assertThat(roomService.findActiveRooms()).extracting(Room::getId).contains(id);
    }

    @Test
    @DisplayName("앞으로 예약(PENDING)이 있으면 비활성화 거부")
    void deactivate_blockedByPendingReservation() {
        Long id = roomService.create(form("테스트룸", "별관 1층", 8));
        insertReservation(id, "PENDING", 1);

        assertThatThrownBy(() -> roomService.deactivate(id))
                .isInstanceOf(RoomHasUpcomingReservationsException.class)
                .hasMessageContaining("1건");
        assertThat(roomService.getRoom(id).isActive()).isTrue();
    }

    @Test
    @DisplayName("앞으로 예약(APPROVED)이 있어도 비활성화 거부")
    void deactivate_blockedByApprovedReservation() {
        Long id = roomService.create(form("테스트룸", "별관 1층", 8));
        insertReservation(id, "APPROVED", 1);

        assertThatThrownBy(() -> roomService.deactivate(id))
                .isInstanceOf(RoomHasUpcomingReservationsException.class);
    }

    @Test
    @DisplayName("이미 끝난 예약이나 취소/거절된 예약은 비활성화를 막지 않는다")
    void deactivate_allowedWithPastOrCanceledReservations() {
        Long id = roomService.create(form("테스트룸", "별관 1층", 8));
        insertReservation(id, "APPROVED", -2);
        insertReservation(id, "CANCELED", 1);
        insertReservation(id, "REJECTED", 1);

        assertThatCode(() -> roomService.deactivate(id)).doesNotThrowAnyException();
        assertThat(roomService.getRoom(id).isActive()).isFalse();
    }

    @Test
    @DisplayName("없는 회의실이면 RoomNotFoundException")
    void notFound() {
        assertThatThrownBy(() -> roomService.getRoom(-1L)).isInstanceOf(RoomNotFoundException.class);
        assertThatThrownBy(() -> roomService.deactivate(-1L)).isInstanceOf(RoomNotFoundException.class);
    }

    /** 예약 기능이 아직 없어서 SQL로 직접 넣는다. startDays: 오늘 기준 며칠 뒤에 시작하는지 (1시간짜리). */
    private void insertReservation(Long roomId, String status, int startDays) {
        String loginId = "roomtest" + System.nanoTime();
        jdbc.update("INSERT INTO member (login_id, password, name) VALUES (?, 'x', '테스터')", loginId);
        Long memberId = jdbc.queryForObject("SELECT id FROM member WHERE login_id = ?", Long.class, loginId);
        jdbc.update("""
                INSERT INTO reservation (member_id, room_id, start_time, end_time, purpose, status)
                VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? DAY),
                        DATE_ADD(DATE_ADD(NOW(), INTERVAL ? DAY), INTERVAL 1 HOUR), '테스트', ?)
                """, memberId, roomId, startDays, startDays, status);
    }

    private static RoomForm form(String name, String location, int capacity) {
        RoomForm f = new RoomForm();
        f.setName(name);
        f.setLocation(location);
        f.setCapacity(capacity);
        return f;
    }
}
