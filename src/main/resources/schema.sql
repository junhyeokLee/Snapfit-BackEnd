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

CREATE TABLE IF NOT EXISTS notification_inbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(60) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(600) NOT NULL,
    target_topic VARCHAR(255) NULL,
    deeplink VARCHAR(500) NULL,
    payload_json LONGTEXT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS notification_read (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    read_at DATETIME NOT NULL,
    CONSTRAINT uk_notification_read_user UNIQUE (notification_id, user_id),
    CONSTRAINT fk_notification_read_inbox
        FOREIGN KEY (notification_id) REFERENCES notification_inbox(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_subscription (
    user_id VARCHAR(128) PRIMARY KEY,
    plan_code VARCHAR(40) NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME NULL,
    expires_at DATETIME NULL,
    next_billing_at DATETIME NULL,
    last_order_id VARCHAR(64) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS billing_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(128) NOT NULL,
    plan_code VARCHAR(40) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    checkout_url VARCHAR(1000) NULL,
    reserve_id VARCHAR(120) NULL,
    transaction_id VARCHAR(120) NULL,
    fail_reason VARCHAR(600) NULL,
    created_at DATETIME NOT NULL,
    approved_at DATETIME NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(128) NOT NULL,
    title VARCHAR(220) NOT NULL,
    amount INT NOT NULL,
    album_id BIGINT NULL,
    page_count INT NULL,
    recipient_name VARCHAR(120) NULL,
    recipient_phone VARCHAR(40) NULL,
    zip_code VARCHAR(20) NULL,
    address_line1 VARCHAR(255) NULL,
    address_line2 VARCHAR(255) NULL,
    delivery_memo VARCHAR(255) NULL,
    payment_method VARCHAR(40) NULL,
    payment_confirmed_at DATETIME NULL,
    print_vendor VARCHAR(40) NULL,
    print_vendor_order_id VARCHAR(120) NULL,
    print_package_json_url VARCHAR(1000) NULL,
    print_file_pdf_url VARCHAR(1000) NULL,
    print_file_zip_url VARCHAR(1000) NULL,
    print_asset_count INT NULL,
    print_package_generated_at DATETIME NULL,
    print_submitted_at DATETIME NULL,
    courier VARCHAR(60) NULL,
    tracking_number VARCHAR(120) NULL,
    shipped_at DATETIME NULL,
    delivered_at DATETIME NULL,
    status VARCHAR(30) NOT NULL,
    progress DOUBLE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
