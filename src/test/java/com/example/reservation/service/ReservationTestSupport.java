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
