package com.example.reservation.exception;

/** 지금 상태에서는 할 수 없는 변경 (이미 취소됨, 이미 처리됨, 이미 시작함 등). */
public class InvalidStatusChangeException extends ReservationException {

    public InvalidStatusChangeException(String message) {
        super(message);
    }
}
