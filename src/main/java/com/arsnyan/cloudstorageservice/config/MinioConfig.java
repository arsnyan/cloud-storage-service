package com.arsnyan.cloudstorageservice.config;

import com.arsnyan.cloudstorageservice.exception.ServerErrorException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class MinioConfig {
    @Value("${app.minio.endpoint-url}")
    private String endpointUrl;

    @Value("${app.minio.credentials.access-key}")
    private String accessKey;

    @Value("${app.minio.credentials.secret-key}")
    private String secretKey;

    @Value("${app.minio.root-bucket-name}")
    private String rootBucket;

    @Bean
    public MinioClient minioClient() {
        try {
            var minioClient = MinioClient.builder()
                .endpoint(endpointUrl)
                .credentials(accessKey, secretKey)
                .build();

            var rootPathExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(rootBucket).build());

            if (!rootPathExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(rootBucket).build());
            }

            return minioClient;
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ServerErrorException("Minio Error", e);
        }
    }
}
