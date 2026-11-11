package com.example.reservation.dto;

import com.example.reservation.domain.ReservationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 내 예약 목록 한 줄. reservation 과 room 을 JOIN 한 결과를 담는다.
 * JSTL 의 fmt 태그는 java.time 을 지원하지 않아서, 화면용 문자열은 여기서 만든다.
 */
@Getter
@NoArgsConstructor
public class ReservationView {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Long id;
    private Long roomId;
    private String roomName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
    private ReservationStatus status;

    /** 화면에서 취소 버튼을 보여줄지. 실제 취소 가능 여부는 서비스(조건부 UPDATE)가 다시 판단한다. */
    public boolean isCancellable() {
        return status.isOccupying() && startTime.isAfter(LocalDateTime.now());
    }

    /** 예: "2026-11-11 10:00 ~ 11:30" (JSP 에서 ${r.timeRange}) */
    public String getTimeRange() {
        return startTime.format(DATE_TIME) + " ~ " + endTime.format(TIME);
    }
}
