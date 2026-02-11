-- 프로필 등 유저 정보 영구 저장 (재시작 후에도 프로필 이미지 URL 유지)
-- MySQL 예약어이므로 백틱으로 테이블명 지정 (DROP 하지 않음 → 데이터 유지)
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NULL,
    name VARCHAR(255) NULL,
    profile_image_url VARCHAR(1000) NULL,
    provider VARCHAR(50) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL
);

DROP TABLE IF EXISTS album_member;

CREATE TABLE IF NOT EXISTS album_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_id BIGINT NOT NULL,
    user_id VARCHAR(128) NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    invited_by VARCHAR(128) NULL,
    invite_token VARCHAR(64) UNIQUE,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_album_member_album
        FOREIGN KEY (album_id) REFERENCES album(id)
        ON DELETE CASCADE
);

-- 인덱스는 JPA/DB 마이그레이션 단계에서 별도로 관리
