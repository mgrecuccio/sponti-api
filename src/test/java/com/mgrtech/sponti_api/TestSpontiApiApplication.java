package com.mgrtech.sponti_api;

import org.springframework.boot.SpringApplication;

public class TestSpontiApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpontiApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
