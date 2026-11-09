package com.example.reservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** @ResponseStatus 덕분에 컨트롤러에서 잡지 않아도 404 응답이 된다. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(Long id) {
        super("회의실을 찾을 수 없습니다: " + id);
    }
}
