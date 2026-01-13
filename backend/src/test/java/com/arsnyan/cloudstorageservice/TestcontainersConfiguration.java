package com.arsnyan.cloudstorageservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
            .withDatabaseName("cloud-storage-app")
            .withUsername("user")
            .withPassword("password");
    }

    @Bean
    public MinIOContainer minIOContainer() {
        return new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
            .withUserName("user")
            .withPassword("password");
    }

    @Bean
    DynamicPropertyRegistrar minioProperties(MinIOContainer container) {
        return registry -> {
            registry.add("app.minio.endpoint-url", container::getS3URL);
            registry.add("app.minio.credentials.access-key", container::getUserName);
            registry.add("app.minio.credentials.secret-key", container::getPassword);
            registry.add("app.minio.root-bucket-name", () -> "test-bucket");
        };
    }
}
