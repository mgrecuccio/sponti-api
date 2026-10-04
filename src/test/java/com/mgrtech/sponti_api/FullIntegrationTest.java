package com.mgrtech.sponti_api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestDatabaseCleanerConfiguration.class})
@ContextConfiguration(initializers = FakeSmsBoxInitializer.class)
@Testcontainers(disabledWithoutDocker = true)
public @interface FullIntegrationTest {
}
