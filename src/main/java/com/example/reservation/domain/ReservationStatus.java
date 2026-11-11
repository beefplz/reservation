package com.example.reservation.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    PENDING("승인 대기"),
    APPROVED("승인"),
    REJECTED("거절"),
    CANCELED("취소");

    /** 화면 표시용 이름. JSP 에서 ${r.status.label} 로 쓴다. */
    private final String label;

    /** 시간을 차지하는 상태인지 (겹침 검사 대상, 취소 가능 대상). */
    public boolean isOccupying() {
        return this == PENDING || this == APPROVED;
    }
}
