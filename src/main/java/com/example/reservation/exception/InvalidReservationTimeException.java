package com.example.reservation.exception;

/** 시간 규칙(30분 단위, 최대 4시간, 과거 불가, 종료 > 시작) 위반. */
public class InvalidReservationTimeException extends ReservationException {

    public InvalidReservationTimeException(String message) {
        super(message);
    }
}
