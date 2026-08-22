package com.simebarisic.goldtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GoldTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoldTrackerApplication.class, args);
    }
}
