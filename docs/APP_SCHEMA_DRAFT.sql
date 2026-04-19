-- SnapFit application schema draft
-- 기준: 2026-04-03 엔티티/운영 문서 기준 초안
-- 주의: 현재 앱은 ddl-auto=update 사용 중이므로 이 파일은 운영 참조용 초안이다.

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NULL,
    name VARCHAR(255) NULL,
    profile_image_url VARCHAR(1000) NULL,
    provider VARCHAR(50) NULL,
    terms_version VARCHAR(50) NULL,
    privacy_version VARCHAR(50) NULL,
    marketing_opt_in BOOLEAN NULL,
    consented_at DATETIME NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL,
    INDEX idx_refresh_token_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS album (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    ratio VARCHAR(100) NOT NULL,
    title VARCHAR(255) NULL,
    cover_image_url VARCHAR(1000) NULL,
    cover_thumbnail_url VARCHAR(1000) NULL,
    cover_original_url VARCHAR(1000) NULL,
    cover_preview_url VARCHAR(1000) NULL,
    cover_layers_json LONGTEXT NULL,
    cover_theme VARCHAR(100) NULL,
    total_pages INT NULL,
    target_pages INT NULL,
    orders INT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    INDEX idx_album_user_updated (user_id, updated_at),
    INDEX idx_album_user_orders (user_id, orders)
);

CREATE TABLE IF NOT EXISTS album_page (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_id BIGINT NOT NULL,
    page_number INT NOT NULL,
    layers_json LONGTEXT NOT NULL,
    image_url VARCHAR(500) NULL,
    original_url VARCHAR(1000) NULL,
    preview_url VARCHAR(1000) NULL,
    thumbnail_url VARCHAR(500) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_album_page_album
        FOREIGN KEY (album_id) REFERENCES album(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_album_page_album_page_number
        UNIQUE (album_id, page_number)
);

CREATE TABLE IF NOT EXISTS album_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_id BIGINT NOT NULL,
    user_id VARCHAR(128) NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    invited_by VARCHAR(128) NULL,
    invite_token VARCHAR(64) NULL UNIQUE,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_album_member_album
        FOREIGN KEY (album_id) REFERENCES album(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_album_member_album_user
        UNIQUE (album_id, user_id)
);

CREATE TABLE IF NOT EXISTS template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NULL,
    sub_title VARCHAR(255) NULL,
    description TEXT NULL,
    cover_image_url VARCHAR(1000) NULL,
    preview_images_json LONGTEXT NULL,
    page_count INT NULL,
    like_count INT NULL,
    user_count INT NULL,
    is_best BOOLEAN NULL,
    is_premium BOOLEAN NULL,
    category VARCHAR(40) NULL,
    tags_json LONGTEXT NULL,
    weekly_score INT NULL,
    new_until DATETIME NULL,
    active BOOLEAN NULL,
    template_json LONGTEXT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    INDEX idx_template_active_score_created (active, weekly_score, created_at),
    INDEX idx_template_active_category_created (active, category, created_at),
    INDEX idx_template_new_until (new_until)
);

CREATE TABLE IF NOT EXISTS template_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    created_at DATETIME NULL,
    CONSTRAINT uk_template_like_template_user UNIQUE (template_id, user_id),
    INDEX idx_template_like_user_created (user_id, created_at)
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
    updated_at DATETIME NOT NULL,
    INDEX idx_user_subscription_status_expires (status, expires_at)
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
    updated_at DATETIME NOT NULL,
    INDEX idx_billing_order_user_created (user_id, created_at),
    INDEX idx_billing_order_status_created (status, created_at)
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
    print_submitted_at DATETIME NULL,
    courier VARCHAR(60) NULL,
    tracking_number VARCHAR(120) NULL,
    shipped_at DATETIME NULL,
    delivered_at DATETIME NULL,
    status VARCHAR(30) NOT NULL,
    progress DOUBLE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_orders_user_created (user_id, created_at),
    INDEX idx_orders_status_created (status, created_at)
);

CREATE TABLE IF NOT EXISTS notification_inbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(60) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(600) NOT NULL,
    target_topic VARCHAR(255) NULL,
    deeplink VARCHAR(500) NULL,
    payload_json LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_notification_inbox_created (created_at),
    INDEX idx_notification_inbox_type_created (type, created_at)
);

CREATE TABLE IF NOT EXISTS notification_read (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    read_at DATETIME NOT NULL,
    CONSTRAINT uk_notification_read_user UNIQUE (notification_id, user_id),
    CONSTRAINT fk_notification_read_inbox
        FOREIGN KEY (notification_id) REFERENCES notification_inbox(id)
        ON DELETE CASCADE,
    INDEX idx_notification_read_user_read_at (user_id, read_at)
);

CREATE TABLE IF NOT EXISTS support_inquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    category VARCHAR(80) NOT NULL,
    title VARCHAR(160) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    resolved_at DATETIME NULL,
    resolved_by VARCHAR(120) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_support_inquiries_user_created (user_id, created_at),
    INDEX idx_support_inquiries_status_created (status, created_at)
);
