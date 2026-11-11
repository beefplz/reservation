package com.example.reservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 예약이 없거나, 남의 예약에 접근한 경우.
 * 남의 예약에 403 을 주면 "그 예약이 존재한다"는 정보가 새므로 둘 다 404 로 응답한다.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long id) {
        super("예약을 찾을 수 없습니다: " + id);
    }
}
