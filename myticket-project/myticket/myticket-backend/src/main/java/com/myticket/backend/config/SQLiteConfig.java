package com.myticket.backend.config;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SQLite-specific configuration.
 * Enables WAL (Write-Ahead Logging) mode for better concurrency support.
 */
@Configuration
public class SQLiteConfig {

    @Bean
    CommandLineRunner enableWalMode(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute("PRAGMA journal_mode=WAL");
        };
    }
}
