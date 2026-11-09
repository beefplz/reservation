package com.example.reservation.exception;

public class RoomHasUpcomingReservationsException extends RuntimeException {

    public RoomHasUpcomingReservationsException(int count) {
        super("향후 예약 " + count + "건이 있어 비활성화할 수 없습니다.");
    }
}
