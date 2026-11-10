package com.example.reservation.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** reservation 테이블과 1:1로 대응하는 객체. */
@Getter
@NoArgsConstructor
public class Reservation {

    private Long id;
    private Long memberId;
    private Long roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 신규 신청. 관리자가 승인하기 전까지 PENDING 이다. */
    public static Reservation newPending(Long memberId, Long roomId,
                                         LocalDateTime startTime, LocalDateTime endTime, String purpose) {
        Reservation r = new Reservation();
        r.memberId = memberId;
        r.roomId = roomId;
        r.startTime = startTime;
        r.endTime = endTime;
        r.purpose = purpose;
        r.status = ReservationStatus.PENDING;
        return r;
    }
}
