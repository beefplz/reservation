package com.example.reservation.service;

import com.example.reservation.domain.Room;
import com.example.reservation.dto.RoomForm;
import com.example.reservation.exception.DuplicateRoomNameException;
import com.example.reservation.exception.RoomHasUpcomingReservationsException;
import com.example.reservation.exception.RoomNotFoundException;
import com.example.reservation.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;

    @Transactional(readOnly = true)
    public List<Room> findActiveRooms() {
        return roomMapper.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<Room> findAllRooms() {
        return roomMapper.findAll();
    }

    @Transactional(readOnly = true)
    public Room getRoom(Long id) {
        return roomMapper.findById(id).orElseThrow(() -> new RoomNotFoundException(id));
    }

    @Transactional
    public Long create(RoomForm form) {
        if (roomMapper.existsByName(form.getName(), null)) {
            throw new DuplicateRoomNameException(form.getName());
        }
        Room room = Room.newRoom(form.getName(), form.getLocation(), form.getCapacity());
        try {
            roomMapper.insert(room);
        } catch (DuplicateKeyException e) {
            // 동시에 같은 이름을 등록해 UNIQUE 키(uk_room_name)에 걸린 경우
            throw new DuplicateRoomNameException(form.getName());
        }
        return room.getId();
    }

    @Transactional
    public void update(Long id, RoomForm form) {
        getRoom(id);
        if (roomMapper.existsByName(form.getName(), id)) {
            throw new DuplicateRoomNameException(form.getName());
        }
        try {
            roomMapper.update(Room.edited(id, form.getName(), form.getLocation(), form.getCapacity()));
        } catch (DuplicateKeyException e) {
            throw new DuplicateRoomNameException(form.getName());
        }
    }

    /**
     * 비활성화. 아직 끝나지 않은 예약이 있으면 거부한다.
     *
     * room 행을 먼저 잠그는 이유: "예약 검사 → UPDATE" 사이에 새 예약이 들어오면
     * 비활성 회의실에 예약이 남는다. 예약 생성도 같은 room 행을 잠그므로 둘은 차례로 실행된다.
     */
    @Transactional
    public void deactivate(Long id) {
        roomMapper.findByIdForUpdate(id).orElseThrow(() -> new RoomNotFoundException(id));

        int upcoming = roomMapper.countUpcomingReservations(id);
        if (upcoming > 0) {
            throw new RoomHasUpcomingReservationsException(upcoming);
        }
        roomMapper.updateActive(id, false);
    }

    @Transactional
    public void activate(Long id) {
        getRoom(id);
        roomMapper.updateActive(id, true);
    }
}
