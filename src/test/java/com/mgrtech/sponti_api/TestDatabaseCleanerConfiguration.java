package com.mgrtech.sponti_api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseCleanerConfiguration {

    @Bean
    DatabaseCleaner databaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource);
    }
}