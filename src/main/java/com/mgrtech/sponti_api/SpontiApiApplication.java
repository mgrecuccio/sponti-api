package com.mgrtech.sponti_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

@SpringBootApplication
@Modulith
public class SpontiApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpontiApiApplication.class, args);
	}

}
