package com.mrtasks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MrTasksApplication {
    public static void main(String[] args) {
        SpringApplication.run(MrTasksApplication.class, args);
    }
}
