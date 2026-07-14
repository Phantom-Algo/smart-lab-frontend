package com.zhangjintao.project.smart_lab_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SmartLabBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartLabBackendApplication.class, args);
	}

}
