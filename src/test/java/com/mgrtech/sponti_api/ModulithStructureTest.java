package com.mgrtech.sponti_api;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModulithStructureTest {

    @Test
    void verifyModules() {
        ApplicationModules.of(SpontiApiApplication.class).verify();
    }
}
