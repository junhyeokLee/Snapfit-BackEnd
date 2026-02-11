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
