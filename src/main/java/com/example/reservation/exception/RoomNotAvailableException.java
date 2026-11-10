package com.example.reservation.exception;

/** 비활성화된 회의실에 예약하려는 경우. */
public class RoomNotAvailableException extends ReservationException {

    public RoomNotAvailableException() {
        super("사용할 수 없는 회의실입니다.");
    }
}
