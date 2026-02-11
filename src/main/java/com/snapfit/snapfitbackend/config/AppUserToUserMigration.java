package com.snapfit.snapfitbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * 앱 기동 시 1회: app_user 테이블이 있으면 데이터를 user 테이블로 복사 후 app_user 삭제.
 */
@Component
@Order(1)
public class AppUserToUserMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppUserToUserMigration.class);

    private final DataSource dataSource;

    public AppUserToUserMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            try (ResultSet rs = meta.getTables(catalog, null, "app_user", new String[]{"TABLE"})) {
                if (!rs.next()) {
                    return;
                }
            }
            log.info("Migration: app_user found, copying to user table and dropping app_user.");
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("INSERT INTO `user` (id, email, name, profile_image_url, provider, created_at, updated_at) " +
                    "SELECT id, email, name, profile_image_url, provider, created_at, updated_at FROM app_user " +
                    "ON DUPLICATE KEY UPDATE email=VALUES(email), name=VALUES(name), profile_image_url=VALUES(profile_image_url), provider=VALUES(provider), updated_at=VALUES(updated_at)");
            jdbc.execute("DROP TABLE app_user");
            log.info("Migration: app_user -> user completed.");
        } catch (Exception e) {
            log.warn("Migration app_user->user skipped or failed (non-fatal): {}", e.getMessage());
        }
    }
}
