package com.example.reservation.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** room 테이블과 1:1로 대응하는 객체. 삭제하지 않고 active = false 로 비활성화한다. */
@Getter
@NoArgsConstructor
public class Room {

    private Long id;
    private String name;
    private String location;
    private int capacity;
    private boolean active;
    private LocalDateTime createdAt;

    /** 등록용. active 는 DB 기본값(1)을 쓴다. */
    public static Room newRoom(String name, String location, int capacity) {
        Room room = new Room();
        room.name = name;
        room.location = location;
        room.capacity = capacity;
        return room;
    }

    /** 수정용. 어떤 회의실을 바꿀지 id 를 함께 담는다. */
    public static Room edited(Long id, String name, String location, int capacity) {
        Room room = newRoom(name, location, capacity);
        room.id = id;
        return room;
    }
}
