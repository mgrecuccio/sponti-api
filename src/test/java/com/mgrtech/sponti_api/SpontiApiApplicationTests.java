package com.mgrtech.sponti_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import({TestcontainersConfiguration.class, TestcontainersConfiguration.class})
@SpringBootTest
class SpontiApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
