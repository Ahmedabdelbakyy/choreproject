package com.family.chores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // <--- Import this

@SpringBootApplication
@EnableScheduling // <--- Add this line
public class ChoresApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChoresApplication.class, args);
	}

}