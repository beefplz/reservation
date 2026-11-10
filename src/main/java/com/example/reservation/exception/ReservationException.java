package com.example.reservation.exception;

/**
 * 예약 규칙 위반으로 신청을 거부할 때의 공통 부모.
 * 컨트롤러는 이 타입 하나만 잡아서 메시지를 화면에 보여준다.
 */
public abstract class ReservationException extends RuntimeException {

    protected ReservationException(String message) {
        super(message);
    }
}
