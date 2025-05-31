package com.victadore.webmafia.mafia_web_of_lies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MafiaWebOfLiesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MafiaWebOfLiesApplication.class, args);
	}

}
