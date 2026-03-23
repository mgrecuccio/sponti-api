package com.mgrtech.sponti_api;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseCleaner(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void clean() {
        assertSafeDatabase();

        List<String> tables = jdbcTemplate.queryForList("""
                SELECT tablename
                FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')
                """, String.class);

        if (tables.isEmpty()) {
            return;
        }

        String truncateSql = tables.stream()
                .map(table -> "\"" + table + "\"")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute("TRUNCATE TABLE " + truncateSql + " RESTART IDENTITY CASCADE");
    }

    private void assertSafeDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            String databaseName = jdbcTemplate.queryForObject("select current_database()", String.class);

            boolean looksLikeTestDb =
                    jdbcUrl != null
                            && (
                            jdbcUrl.contains("sponti_test")
                                    || jdbcUrl.contains("jdbc:tc:")
                                    || jdbcUrl.contains("localhost")
                    )
                            && "sponti_test".equals(databaseName);

            if (!looksLikeTestDb) {
                throw new IllegalStateException(
                        "Refusing to clean non-test database. " +
                                "jdbcUrl=" + jdbcUrl + ", databaseName=" + databaseName
                );
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not verify database before cleanup", e);
        }
    }
}