package com.example.reservation.exception;

public class ReservationOverlapException extends ReservationException {

    public ReservationOverlapException() {
        super("이미 예약된 시간입니다. 다른 시간을 선택하세요.");
    }
}
