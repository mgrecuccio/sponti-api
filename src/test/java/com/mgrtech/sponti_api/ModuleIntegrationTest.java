package com.mgrtech.sponti_api.support;

import com.mgrtech.sponti_api.TestcontainersConfiguration;
import com.mgrtech.sponti_api.TestDatabaseCleanerConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApplicationModuleTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestDatabaseCleanerConfiguration.class})
public @interface ModuleIntegrationTest {
}