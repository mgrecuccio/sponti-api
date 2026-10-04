package com.mgrtech.sponti_api;

import com.mgrtech.sponti_api.TestcontainersConfiguration;
import com.mgrtech.sponti_api.TestDatabaseCleanerConfiguration;
import org.springframework.core.annotation.AliasFor;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApplicationModuleTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestDatabaseCleanerConfiguration.class, TestObservabilityConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
public @interface ModuleIntegrationTest {

    @AliasFor(annotation = ApplicationModuleTest.class, attribute = "mode")
    ApplicationModuleTest.BootstrapMode mode() default ApplicationModuleTest.BootstrapMode.STANDALONE;
}
