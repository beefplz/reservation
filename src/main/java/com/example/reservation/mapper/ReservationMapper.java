package com.example.reservation.mapper;

import com.example.reservation.domain.Reservation;
import com.example.reservation.domain.ReservationStatus;
import com.example.reservation.dto.ReservationView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ReservationMapper {

    void insert(Reservation reservation);

    /** 같은 회의실에서 시간이 겹치는 PENDING/APPROVED 예약 수. */
    int countOverlapping(@Param("roomId") Long roomId,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    List<ReservationView> findByMemberId(Long memberId);

    Optional<Reservation> findById(Long id);

    /** 본인 예약이고, PENDING/APPROVED 이고, 아직 시작 전일 때만 CANCELED 로 바꾼다. 바뀐 행 수를 돌려준다. */
    int cancel(@Param("id") Long id, @Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    /** 현재 상태가 from 일 때만 to 로 바꾼다. startAfter 가 null 이 아니면 그 이후에 시작하는 예약만. */
    int changeStatus(@Param("id") Long id,
                     @Param("from") ReservationStatus from,
                     @Param("to") ReservationStatus to,
                     @Param("startAfter") LocalDateTime startAfter);

    /** status 가 null 이면 전체. */
    List<ReservationView> findAllForAdmin(@Param("status") ReservationStatus status);
}
