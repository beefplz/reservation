-- 회의실 예약 시스템 스키마 (MySQL 8)
-- 위치: src/main/resources/schema.sql

CREATE TABLE IF NOT EXISTS member (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    login_id    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,             -- BCrypt 해시 저장 (평문 금지)
    name        VARCHAR(50)  NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER', -- USER / ADMIN
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_login_id (login_id),
    CONSTRAINT ck_member_role CHECK (role IN ('USER', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS room (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    location    VARCHAR(100) NOT NULL,
    capacity    INT          NOT NULL,
    active      TINYINT(1)   NOT NULL DEFAULT 1,  -- 0이면 예약 불가 (삭제 대신 비활성화)
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_name (name),
    CONSTRAINT ck_room_capacity CHECK (capacity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS reservation (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL,
    room_id     BIGINT       NOT NULL,
    start_time  DATETIME     NOT NULL,
    end_time    DATETIME     NOT NULL,
    purpose     VARCHAR(200) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING / APPROVED / REJECTED / CANCELED
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservation_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_reservation_room   FOREIGN KEY (room_id)   REFERENCES room (id),
    CONSTRAINT ck_reservation_time   CHECK (end_time > start_time),
    CONSTRAINT ck_reservation_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELED')),
    -- 중복 예약 검사(같은 회의실 + 시간 겹침)용 인덱스
    KEY idx_reservation_room_time (room_id, start_time, end_time),
    -- 내 예약 목록 조회용 인덱스
    KEY idx_reservation_member (member_id, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 테스트용 회의실 데이터
INSERT INTO room (name, location, capacity) VALUES
    ('회의실 A', '본관 3층', 6),
    ('회의실 B', '본관 3층', 10),
    ('대회의실', '본관 5층', 30);
