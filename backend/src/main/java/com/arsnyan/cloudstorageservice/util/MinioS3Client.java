package com.arsnyan.cloudstorageservice.util;

import com.arsnyan.cloudstorageservice.exception.MinioWrappedException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioS3Client implements S3Client {
    private final MinioClient minioClient;

    @Value("${app.minio.root-bucket-name}")
    private String rootBucket;

    public void uploadSnowballObject(List<SnowballObject> objects) {
        invoke(objects, o ->
            minioClient.uploadSnowballObjects(
                UploadSnowballObjectsArgs.builder()
                    .bucket(rootBucket)
                    .objects(objects)
                    .build()
            )
        );
    }

    public void copyObject(Item item, String pathTo) {
        var objectPath = item.objectName();

        try {
            minioClient.copyObject(
                CopyObjectArgs.builder()
                    .bucket(rootBucket)
                    .source(
                        CopySource.builder()
                            .bucket(rootBucket)
                            .object(objectPath)
                            .build()
                    )
                    .object(pathTo)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to copy an object at path {}: {}", objectPath, e.getMessage());
            throw MinioWrappedException.from(e);
        }
    }

    public GetObjectResponse getObject(String path) {
        return invoke(path, p ->
            minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(rootBucket)
                    .object(p)
                    .build()
            )
        );
    }

    public void removeObject(String path) {
        invoke(path, p -> {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(rootBucket)
                    .object(p)
                    .build()
            );

            return new Void[0];
        });
    }

    public Iterable<Result<Item>> listObjects(String path, boolean recursive) {
        return minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(rootBucket)
                .prefix(path)
                .recursive(recursive)
                .build()
        );
    }

    public StatObjectResponse getStatObject(String path) {
        try {
            return minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(rootBucket)
                    .object(path)
                    .build()
            );
        } catch (Exception e) {
            if (e instanceof ErrorResponseException && ((ErrorResponseException) e).errorResponse().code().equals("NoSuchKey")) {
                return null;
            }

            log.error("Getting stats for object {} failed: {}", path, e.getMessage());
            throw MinioWrappedException.from(e);
        }
    }

    public boolean isPathAvailable(String path) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(rootBucket)
                    .object(path)
                    .build()
            );

            return false;
        } catch (Exception e) {
            if (e instanceof ErrorResponseException && ((ErrorResponseException) e).errorResponse().code().equals("NoSuchKey")) {
                return true;
            }

            log.error(e.getMessage(), e);
            throw MinioWrappedException.from(e);
        }
    }

    public void makeFolderInS3(String path) {
        invoke(path, p ->
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(rootBucket)
                    .object(path)
                    .stream(InputStream.nullInputStream(), 0, -1)
                    .build()
            )
        );
    }

    private <Input, LambdaOutput> LambdaOutput invoke(Input path,
                                           ThrowingFunction<@NonNull Input, @NonNull LambdaOutput> function) {
        try {
            return function.apply(path);
        } catch (Exception e) {
            log.error("Failed to invoke method: {}", e.getMessage());
            throw MinioWrappedException.from(e);
        }
    }
}
