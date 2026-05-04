package com.finsimx;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinSimXApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinSimXApplication.class, args);
    }
}
