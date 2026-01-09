package com.arsnyan.cloudstorageservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    public ResourceAlreadyExistsException(String message, Throwable cause) { super(message, cause); }

    public static ResourceAlreadyExistsException forPath(String path) {
        return new ResourceAlreadyExistsException("Resource already exists: " + path);
    }
}
