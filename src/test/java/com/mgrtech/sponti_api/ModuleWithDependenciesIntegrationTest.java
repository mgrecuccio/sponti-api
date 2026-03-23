package com.mgrtech.sponti_api;

import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestDatabaseCleanerConfiguration.class})
public @interface ModuleWithDependenciesIntegrationTest {
}