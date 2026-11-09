package com.example.reservation.mapper;

import com.example.reservation.domain.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoomMapper {

    List<Room> findAllActive();

    List<Room> findAll();

    Optional<Room> findById(Long id);

    /** room 행을 잠그고 조회한다 (SELECT ... FOR UPDATE). @Transactional 안에서만 의미가 있다. */
    Optional<Room> findByIdForUpdate(Long id);

    /** excludeId 를 주면 그 회의실은 빼고 검사한다 (수정할 때 자기 이름은 중복이 아니므로). */
    boolean existsByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    void insert(Room room);

    void update(Room room);

    void updateActive(@Param("id") Long id, @Param("active") boolean active);

    /** 아직 끝나지 않은 PENDING/APPROVED 예약 수. 진행 중인 예약도 포함한다. */
    int countUpcomingReservations(Long roomId);
}
