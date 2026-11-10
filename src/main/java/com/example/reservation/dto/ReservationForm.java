package com.example.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 예약 신청 화면의 입력값.
 * 날짜 하나 + 시작/종료 시각이라 구조상 "같은 날 안에서만" 예약된다.
 * 신청자(memberId)는 폼으로 받지 않는다 → 로그인 정보에서 꺼낸다.
 */
@Getter
@Setter
public class ReservationForm {

    @NotNull(message = "회의실을 선택하세요.")
    private Long roomId;

    @NotNull(message = "날짜를 선택하세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)       // "2026-11-11"
    private LocalDate date;

    @NotNull(message = "시작 시각을 선택하세요.")
    @DateTimeFormat(pattern = "HH:mm")                    // "10:00"
    private LocalTime startTime;

    @NotNull(message = "종료 시각을 선택하세요.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotBlank(message = "사용 목적을 입력하세요.")
    @Size(max = 200, message = "사용 목적은 200자 이내입니다.")
    private String purpose;

    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(date, startTime);
    }

    public LocalDateTime getEndDateTime() {
        return LocalDateTime.of(date, endTime);
    }
}
