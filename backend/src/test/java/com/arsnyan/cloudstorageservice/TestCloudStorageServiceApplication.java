package com.arsnyan.cloudstorageservice;

import org.springframework.boot.SpringApplication;

public class TestCloudStorageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.from(CloudStorageServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
