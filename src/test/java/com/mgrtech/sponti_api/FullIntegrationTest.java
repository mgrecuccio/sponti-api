package com.mgrtech.sponti_api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestDatabaseCleanerConfiguration.class})
public @interface FullIntegrationTest {
}
