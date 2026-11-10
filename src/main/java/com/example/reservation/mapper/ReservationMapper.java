package com.example.reservation.mapper;

import com.example.reservation.domain.Reservation;
import com.example.reservation.dto.ReservationView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReservationMapper {

    void insert(Reservation reservation);

    /** 같은 회의실에서 시간이 겹치는 PENDING/APPROVED 예약 수. */
    int countOverlapping(@Param("roomId") Long roomId,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    List<ReservationView> findByMemberId(Long memberId);
}
