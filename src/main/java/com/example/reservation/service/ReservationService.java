package com.example.reservation.service;

import com.example.reservation.domain.Reservation;
import com.example.reservation.domain.Room;
import com.example.reservation.dto.ReservationForm;
import com.example.reservation.dto.ReservationView;
import com.example.reservation.exception.InvalidReservationTimeException;
import com.example.reservation.exception.ReservationOverlapException;
import com.example.reservation.exception.RoomNotAvailableException;
import com.example.reservation.exception.RoomNotFoundException;
import com.example.reservation.mapper.ReservationMapper;
import com.example.reservation.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    static final int SLOT_MINUTES = 30;
    static final Duration MAX_DURATION = Duration.ofHours(4);

    private final ReservationMapper reservationMapper;
    private final RoomMapper roomMapper;

    /**
     * 예약 신청. 생성된 예약 id 를 돌려준다.
     *
     * 동시 신청 대비: room 행을 SELECT ... FOR UPDATE 로 잠근 뒤 겹침을 검사한다.
     * 같은 회의실에 대한 신청(그리고 회의실 비활성화)은 이 잠금 때문에 한 번에 하나씩 실행된다.
     *
     * 주의: 잠금은 반드시 트랜잭션의 "첫 번째 쿼리"여야 한다.
     * MySQL 기본 격리 수준(REPEATABLE READ)은 첫 일반 SELECT 시점에 스냅샷을 만든다.
     * 잠금보다 먼저 일반 SELECT 를 하면 스냅샷이 일찍 고정되어,
     * 앞 트랜잭션이 잠금을 풀며 커밋한 예약을 겹침 검사에서 보지 못한다.
     */
    @Transactional
    public Long create(Long memberId, ReservationForm form) {
        LocalDateTime start = form.getStartDateTime();
        LocalDateTime end = form.getEndDateTime();
        validateTime(start, end, LocalDateTime.now());   // DB 를 쓰지 않는 검사는 잠그기 전에

        Room room = roomMapper.findByIdForUpdate(form.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException(form.getRoomId()));
        if (!room.isActive()) {
            throw new RoomNotAvailableException();
        }
        if (reservationMapper.countOverlapping(room.getId(), start, end) > 0) {
            throw new ReservationOverlapException();
        }

        Reservation reservation = Reservation.newPending(memberId, room.getId(), start, end, form.getPurpose());
        reservationMapper.insert(reservation);
        return reservation.getId();
    }

    @Transactional(readOnly = true)
    public List<ReservationView> findMyReservations(Long memberId) {
        return reservationMapper.findByMemberId(memberId);
    }

    /** 시간 규칙. 같은 날 안에서만 예약되는 것은 폼 구조(날짜 1개)가 보장한다. */
    static void validateTime(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (!end.isAfter(start)) {
            throw new InvalidReservationTimeException("종료 시각은 시작 시각보다 늦어야 합니다.");
        }
        if (!isOnSlot(start) || !isOnSlot(end)) {
            throw new InvalidReservationTimeException("시간은 " + SLOT_MINUTES + "분 단위로 선택하세요.");
        }
        if (Duration.between(start, end).compareTo(MAX_DURATION) > 0) {
            throw new InvalidReservationTimeException("최대 " + MAX_DURATION.toHours() + "시간까지 예약할 수 있습니다.");
        }
        if (!start.isAfter(now)) {
            throw new InvalidReservationTimeException("지난 시간은 예약할 수 없습니다.");
        }
    }

    private static boolean isOnSlot(LocalDateTime t) {
        return t.getMinute() % SLOT_MINUTES == 0 && t.getSecond() == 0 && t.getNano() == 0;
    }
}
