CREATE TABLE IF NOT EXISTS education_device_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_code VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_education_device_model_code (model_code)
);

CREATE TABLE IF NOT EXISTS member_activation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(128) NOT NULL,
    model_code VARCHAR(64) NOT NULL,
    user_id VARCHAR(128),
    activated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_member_activation_device_id (device_id),
    KEY idx_member_activation_model_code (model_code),
    KEY idx_member_activation_activated_at (activated_at)
);

INSERT INTO education_device_model (model_code, model_name, enabled)
VALUES ('EDU-PAD-2026', 'Education Tablet 2026', TRUE)
ON DUPLICATE KEY UPDATE model_name = VALUES(model_name), enabled = VALUES(enabled);

CREATE TABLE IF NOT EXISTS edu_benefit_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_code VARCHAR(64) NOT NULL,
    activity_name VARCHAR(128) NOT NULL,
    benefit_type VARCHAR(32) NOT NULL,
    benefit_value VARCHAR(128) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status TINYINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_code(activity_code)
);

CREATE TABLE IF NOT EXISTS edu_new_device_whitelist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id_hash VARCHAR(128) NOT NULL,
    model VARCHAR(64),
    activate_time TIMESTAMP,
    source VARCHAR(32),
    status TINYINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_device(device_id_hash),
    KEY idx_activate_time(activate_time)
);

CREATE TABLE IF NOT EXISTS edu_benefit_receive_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    device_id_hash VARCHAR(128) NOT NULL,
    sn VARCHAR(128),
    receive_status VARCHAR(32) NOT NULL,
    grant_status VARCHAR(32) NOT NULL,
    grant_order_no VARCHAR(64) NOT NULL,
    receive_time TIMESTAMP,
    success_time TIMESTAMP,
    fail_code VARCHAR(64),
    fail_reason VARCHAR(512),
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time TIMESTAMP,
    expire_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    UNIQUE KEY uk_activity_device(activity_id, device_id_hash),
    UNIQUE KEY uk_grant_order_no(grant_order_no),
    KEY idx_user_id(user_id),
    KEY idx_grant_status(grant_status, next_retry_time),
    KEY idx_expire_time(grant_status, expire_time)
);

CREATE TABLE IF NOT EXISTS edu_benefit_grant_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grant_order_no VARCHAR(64) NOT NULL,
    receive_record_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    device_id_hash VARCHAR(128) NOT NULL,
    benefit_type VARCHAR(32) NOT NULL,
    member_days INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time TIMESTAMP,
    request_body TEXT,
    response_body TEXT,
    fail_reason VARCHAR(512),
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    UNIQUE KEY uk_grant_task_order_no(grant_order_no),
    KEY idx_status_retry(status, next_retry_time)
);

INSERT INTO edu_benefit_activity (
    id, activity_code, activity_name, benefit_type, benefit_value, start_time, end_time, status
)
VALUES (
    10001, 'EDU_MEMBER_2026', 'Education Member 2026', 'EDU_MEMBER', '365',
    '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1
)
ON DUPLICATE KEY UPDATE
    activity_name = VALUES(activity_name),
    benefit_type = VALUES(benefit_type),
    benefit_value = VALUES(benefit_value),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    status = VALUES(status);

INSERT INTO edu_new_device_whitelist (device_id_hash, model, activate_time, source, status)
VALUES
    ('7162da4bf9452a5f9fdf84595057b00e322bef1bb765255fbe28a5020962effe', 'EDU-PAD-2026', '2026-05-01 00:00:00', 'FAKE_DEVICE_CENTER', 1),
    ('6721c3f2b24b242e7acc3e763172a5a6c0612e855dddfc7a7841790e488de786', 'EDU-PAD-2026', '2026-05-01 00:00:00', 'FAKE_DEVICE_CENTER', 1),
    ('e7072234c661e2a2067b34f9707b6e541f4913bfa635d912ebd452b40de77500', 'EDU-PAD-2026', '2026-05-01 00:00:00', 'FAKE_DEVICE_CENTER', 1)
ON DUPLICATE KEY UPDATE model = VALUES(model), status = VALUES(status);

CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_name VARCHAR(128) NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    status TINYINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coupon_status_time(status, start_time, end_time)
);

CREATE TABLE IF NOT EXISTS coupon_receive_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    claim_status VARCHAR(32) NOT NULL,
    claim_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    UNIQUE KEY uk_coupon_receive_coupon_user(coupon_id, user_id),
    KEY idx_coupon_receive_user_id(user_id)
);

INSERT INTO coupon (
    id, coupon_name, total_stock, available_stock, status, start_time, end_time
)
VALUES
    (10001, 'Spring Coupon 2026', 10, 10, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'),
    (10002, 'Sold Out Coupon 2026', 1, 0, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'),
    (10003, 'Disabled Coupon 2026', 10, 10, 0, '2026-01-01 00:00:00', '2026-12-31 23:59:59')
ON DUPLICATE KEY UPDATE
    coupon_name = VALUES(coupon_name),
    total_stock = VALUES(total_stock),
    available_stock = VALUES(available_stock),
    status = VALUES(status),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time);
