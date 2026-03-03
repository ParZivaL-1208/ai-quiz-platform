package com.quizzApp.quizzapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.quizzApp.quizzapp")
public class QuizzappApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuizzappApplication.class, args);

	}

}