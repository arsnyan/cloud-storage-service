package com.arsnyan.cloudstorageservice.exception;

public class MinioWrappedException extends RuntimeException {
    public MinioWrappedException(String message) {
        super(message);
    }

    public MinioWrappedException(String message, Throwable cause) {
        super(message, cause);
    }

    public static MinioWrappedException from(Exception e) {
        return new MinioWrappedException("Something went wrong with S3", e);
    }
}
