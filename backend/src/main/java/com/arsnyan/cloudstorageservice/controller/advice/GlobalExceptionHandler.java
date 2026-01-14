package com.arsnyan.cloudstorageservice.controller.advice;

import com.arsnyan.cloudstorageservice.exception.EntityAlreadyExistsException;
import com.arsnyan.cloudstorageservice.exception.MinioWrappedException;
import com.arsnyan.cloudstorageservice.exception.NoSuchEntityException;
import com.arsnyan.cloudstorageservice.exception.ServerErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleEntityAlreadyExistsException(EntityAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            e.getMessage()
        );
    }
    
    @ExceptionHandler(MinioWrappedException.class)
    public ProblemDetail handleMinioWrappedException(MinioWrappedException e) {
        var detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            e.getMessage()
        );

        var cause = e.getCause();
        if (cause != null) {
            detail.setProperty("Cause", cause.getMessage());
        }

        return detail;
    }
    
    @ExceptionHandler(NoSuchEntityException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNoSuchEntityException(NoSuchEntityException e) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            e.getMessage()
        );
    }
    
    @ExceptionHandler(ServerErrorException.class)
    public ProblemDetail handleServerErrorException(ServerErrorException e) {
        var detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            e.getMessage()
        );

        detail.setTitle("Something went wrong on server side. Try again later or contact support.");

        var cause = e.getCause();
        if (cause != null) {
            detail.setProperty("Cause", cause.getMessage());
        }

        return detail;
    }
}
