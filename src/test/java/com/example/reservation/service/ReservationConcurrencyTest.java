package com.example.reservation.service;

import com.example.reservation.exception.ReservationException;
import com.example.reservation.exception.RoomHasUpcomingReservationsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.reservation.service.ReservationTestSupport.form;
import static com.example.reservation.service.ReservationTestSupport.insertMember;
import static com.example.reservation.service.ReservationTestSupport.insertRoom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 여러 스레드가 동시에 요청할 때 room 행 잠금(FOR UPDATE)이 중복을 막는지 검증한다.
 *
 * 스레드끼리 서로의 커밋을 봐야 하므로 @Transactional(롤백)을 쓰지 않는다.
 * 대신 만든 데이터를 @AfterEach 에서 직접 지운다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReservationConcurrencyTest {

    static final int THREADS = 8;   // Hikari 기본 커넥션 풀(10개)보다 적게

    @Autowired ReservationService reservationService;
    @Autowired RoomService roomService;
    @Autowired JdbcTemplate jdbc;

    final List<Long> roomIds = new ArrayList<>();
    final List<Long> memberIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long roomId : roomIds) {
            jdbc.update("DELETE FROM reservation WHERE room_id = ?", roomId);
            jdbc.update("DELETE FROM room WHERE id = ?", roomId);
        }
        for (Long memberId : memberIds) {
            jdbc.update("DELETE FROM member WHERE id = ?", memberId);
        }
    }

    @Test
    @DisplayName("같은 회의실·같은 시간에 8명이 동시에 신청하면 정확히 1건만 성공한다")
    void sameSlot_onlyOneSucceeds() throws Exception {
        Long roomId = room();
        List<Long> members = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            members.add(member());
        }

        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        runConcurrently(THREADS, i -> () -> {
            try {
                reservationService.create(members.get(i), form(roomId, "10:00", "11:00"));
                success.incrementAndGet();
            } catch (ReservationException e) {
                rejected.incrementAndGet();
            }
            return null;
        });

        assertThat(success.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(THREADS - 1);
        assertThat(countReservations(roomId)).isEqualTo(1);
    }

    @Test
    @DisplayName("예약 신청과 회의실 비활성화가 동시에 와도, 비활성 회의실에 예약이 남지 않는다")
    void reserveVsDeactivate_neverBoth() throws Exception {
        Long memberId = member();

        // 실행 순서가 매번 달라지도록 여러 번 반복한다
        for (int round = 0; round < 20; round++) {
            Long roomId = room();
            runConcurrently(2, i -> () -> {
                try {
                    if (i == 0) {
                        reservationService.create(memberId, form(roomId, "10:00", "11:00"));
                    } else {
                        roomService.deactivate(roomId);
                    }
                } catch (ReservationException | RoomHasUpcomingReservationsException e) {
                    // 둘 중 하나는 거부될 수 있다 (어느 쪽이 먼저 잠금을 잡았느냐에 따라)
                }
                return null;
            });

            boolean active = roomService.getRoom(roomId).isActive();
            int reservations = countReservations(roomId);
            // 허용되는 결과: (활성 + 예약 1건) 또는 (비활성 + 예약 0건)
            assertThat(active ? reservations == 1 : reservations == 0)
                    .as("round %d: active=%s, reservations=%d", round, active, reservations)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("사용자 취소와 관리자 승인이 동시에 와도, 취소가 승인에 덮어써지지 않는다")
    void cancelVsApprove_cancelIsNeverLost() throws Exception {
        Long memberId = member();
        Long roomId = room();

        // 가능한 순서는 두 가지뿐이다:
        //  (a) 취소 → 승인: 승인은 PENDING 이 아니라서 거부. 최종 CANCELED
        //  (b) 승인 → 취소: APPROVED 도 취소할 수 있으므로 둘 다 성공. 최종 CANCELED
        // 어느 쪽이든 "취소 성공 + 최종 CANCELED" 여야 한다.
        // 막아야 할 버그: 승인이 "PENDING 확인 → (그 사이 취소 커밋) → APPROVED 저장" 으로 취소를 덮어쓰는 것.
        for (int round = 0; round < 20; round++) {
            Long id = ReservationTestSupport.insertReservation(jdbc, memberId, roomId, 24 + round * 2, "PENDING");
            AtomicInteger cancelSuccess = new AtomicInteger();
            AtomicInteger approveSuccess = new AtomicInteger();

            runConcurrently(2, i -> () -> {
                try {
                    if (i == 0) {
                        reservationService.cancel(memberId, id);
                        cancelSuccess.incrementAndGet();
                    } else {
                        reservationService.approve(id);
                        approveSuccess.incrementAndGet();
                    }
                } catch (ReservationException e) {
                    // 승인은 (a) 순서일 때 거부될 수 있다
                }
                return null;
            });

            assertThat(cancelSuccess.get()).as("round %d: 취소는 항상 성공", round).isEqualTo(1);
            assertThat(ReservationTestSupport.statusOf(jdbc, id))
                    .as("round %d: approveSuccess=%d", round, approveSuccess.get())
                    .isEqualTo("CANCELED");
        }
    }

    /** count 개의 작업을 CountDownLatch 로 "동시에" 출발시킨다. */
    private void runConcurrently(int count, java.util.function.IntFunction<Callable<Void>> taskFactory)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++) {
                Callable<Void> task = taskFactory.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();          // 모든 스레드가 준비될 때까지 대기
                    return task.call();
                }));
            }
            ready.await();
            start.countDown();              // 일제히 출발
            for (Future<Void> f : futures) {
                f.get(30, TimeUnit.SECONDS);  // 예상하지 못한 예외는 여기서 테스트 실패로 드러난다
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private Long room() {
        Long id = insertRoom(jdbc);
        roomIds.add(id);
        return id;
    }

    private Long member() {
        Long id = insertMember(jdbc);
        memberIds.add(id);
        return id;
    }

    private int countReservations(Long roomId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE room_id = ?", Integer.class, roomId);
    }
}
