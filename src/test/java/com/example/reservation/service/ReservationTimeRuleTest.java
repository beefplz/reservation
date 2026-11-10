package com.example.reservation.service;

import com.example.reservation.exception.InvalidReservationTimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 시간 규칙만 따로 검증한다. DB 가 필요 없고, 현재 시각(now)을 직접 넘겨서 결과가 항상 같다. */
class ReservationTimeRuleTest {

    static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 10, 9, 0);
    static final LocalDateTime DAY = LocalDateTime.of(2030, 1, 11, 0, 0);

    @Test
    @DisplayName("30분 단위, 4시간 이내, 미래 시각이면 통과")
    void valid() {
        assertThatCode(() -> ReservationService.validateTime(at(10, 0), at(11, 30), NOW)).doesNotThrowAnyException();
        assertThatCode(() -> ReservationService.validateTime(at(9, 0), at(13, 0), NOW))   // 정확히 4시간
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("종료가 시작보다 이르거나 같으면 거부")
    void endNotAfterStart() {
        assertThatThrownBy(() -> ReservationService.validateTime(at(11, 0), at(10, 0), NOW))
                .isInstanceOf(InvalidReservationTimeException.class).hasMessageContaining("늦어야");
        assertThatThrownBy(() -> ReservationService.validateTime(at(10, 0), at(10, 0), NOW))
                .isInstanceOf(InvalidReservationTimeException.class);
    }

    @Test
    @DisplayName("30분 단위가 아니면 거부")
    void notOnSlot() {
        assertThatThrownBy(() -> ReservationService.validateTime(at(10, 15), at(11, 0), NOW))
                .isInstanceOf(InvalidReservationTimeException.class).hasMessageContaining("30분");
        assertThatThrownBy(() -> ReservationService.validateTime(at(10, 0), at(10, 45), NOW))
                .isInstanceOf(InvalidReservationTimeException.class);
    }

    @Test
    @DisplayName("4시간을 넘으면 거부")
    void tooLong() {
        assertThatThrownBy(() -> ReservationService.validateTime(at(9, 0), at(13, 30), NOW))
                .isInstanceOf(InvalidReservationTimeException.class).hasMessageContaining("4시간");
    }

    @Test
    @DisplayName("지난 시간(또는 지금)이면 거부")
    void past() {
        LocalDateTime now = DAY.withHour(10);
        assertThatThrownBy(() -> ReservationService.validateTime(at(9, 0), at(10, 0), now))
                .isInstanceOf(InvalidReservationTimeException.class).hasMessageContaining("지난");
        assertThatThrownBy(() -> ReservationService.validateTime(at(10, 0), at(11, 0), now))
                .isInstanceOf(InvalidReservationTimeException.class);
    }

    private static LocalDateTime at(int hour, int minute) {
        return DAY.withHour(hour).withMinute(minute);
    }
}
