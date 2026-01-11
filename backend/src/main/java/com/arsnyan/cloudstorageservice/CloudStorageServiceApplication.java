package com.arsnyan.cloudstorageservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CloudStorageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudStorageServiceApplication.class, args);
    }
}
