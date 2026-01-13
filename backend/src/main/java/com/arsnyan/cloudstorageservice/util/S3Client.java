package com.arsnyan.cloudstorageservice.util;

import io.minio.GetObjectResponse;
import io.minio.Result;
import io.minio.SnowballObject;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;

import java.util.List;

public interface S3Client {
    void uploadSnowballObject(List<SnowballObject> objects);

    void copyObject(Item item, String pathTo);

    GetObjectResponse getObject(String path);

    void removeObject(String path);

    Iterable<Result<Item>> listObjects(String path, boolean recursive);

    StatObjectResponse getStatObject(String path);

    boolean isPathUnavailable(String path);

    void makeFolderInS3(String path);
}
