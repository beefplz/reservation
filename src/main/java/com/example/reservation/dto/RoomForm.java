package com.example.reservation.dto;

import com.example.reservation.domain.Room;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 회의실 등록/수정 화면의 입력값. */
@Getter
@Setter
public class RoomForm {

    @NotBlank(message = "회의실 이름을 입력하세요.")
    @Size(max = 50, message = "회의실 이름은 50자 이내입니다.")
    private String name;

    @NotBlank(message = "위치를 입력하세요.")
    @Size(max = 100, message = "위치는 100자 이내입니다.")
    private String location;

    @NotNull(message = "수용 인원을 입력하세요.")
    @Min(value = 1, message = "수용 인원은 1명 이상입니다.")
    @Max(value = 100, message = "수용 인원은 100명 이하입니다.")
    private Integer capacity;

    /** 수정 화면에 기존 값을 채울 때 쓴다. */
    public static RoomForm from(Room room) {
        RoomForm form = new RoomForm();
        form.name = room.getName();
        form.location = room.getLocation();
        form.capacity = room.getCapacity();
        return form;
    }
}
