package com.example.reservation.service;

import com.example.reservation.dto.ReservationForm;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;

/** 예약 테스트들이 같이 쓰는 데이터 준비 도우미. 이름이 겹치지 않도록 nanoTime 을 붙인다. */
public final class ReservationTestSupport {

    /** 테스트용 예약 날짜. 오늘 기준 일주일 뒤라 "지난 시간" 규칙에 걸리지 않는다. */
    public static final LocalDate DAY = LocalDate.now().plusDays(7);

    private ReservationTestSupport() {
    }

    public static Long insertMember(JdbcTemplate jdbc) {
        String loginId = "rsv" + System.nanoTime();
        jdbc.update("INSERT INTO member (login_id, password, name) VALUES (?, 'x', '테스터')", loginId);
        return jdbc.queryForObject("SELECT id FROM member WHERE login_id = ?", Long.class, loginId);
    }

    public static Long insertRoom(JdbcTemplate jdbc) {
        String name = "예약테스트룸" + System.nanoTime();
        jdbc.update("INSERT INTO room (name, location, capacity) VALUES (?, '별관', 6)", name);
        return jdbc.queryForObject("SELECT id FROM room WHERE name = ?", Long.class, name);
    }

    /**
     * 서비스 규칙(과거 불가 등)을 거치지 않고 SQL 로 직접 예약을 넣는다. 지난 예약을 만들 때 쓴다.
     * startHoursFromNow: 지금 기준 몇 시간 뒤에 시작하는지 (음수면 과거). 길이는 1시간.
     */
    public static Long insertReservation(JdbcTemplate jdbc, Long memberId, Long roomId,
                                         int startHoursFromNow, String status) {
        String purpose = "직접삽입" + System.nanoTime();
        jdbc.update("""
                INSERT INTO reservation (member_id, room_id, start_time, end_time, purpose, status)
                VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? HOUR),
                        DATE_ADD(DATE_ADD(NOW(), INTERVAL ? HOUR), INTERVAL 1 HOUR), ?, ?)
                """, memberId, roomId, startHoursFromNow, startHoursFromNow, purpose, status);
        return jdbc.queryForObject("SELECT id FROM reservation WHERE purpose = ?", Long.class, purpose);
    }

    public static String statusOf(JdbcTemplate jdbc, Long reservationId) {
        return jdbc.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, reservationId);
    }

    /** "10:00" 처럼 시:분 문자열로 받는다. */
    public static ReservationForm form(Long roomId, String start, String end) {
        ReservationForm f = new ReservationForm();
        f.setRoomId(roomId);
        f.setDate(DAY);
        f.setStartTime(LocalTime.parse(start));
        f.setEndTime(LocalTime.parse(end));
        f.setPurpose("주간 회의");
        return f;
    }
}
