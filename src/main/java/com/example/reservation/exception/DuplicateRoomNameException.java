package com.example.reservation.exception;

public class DuplicateRoomNameException extends RuntimeException {

    public DuplicateRoomNameException(String name) {
        super("이미 사용 중인 회의실 이름입니다: " + name);
    }
}
