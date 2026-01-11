package com.arsnyan.cloudstorageservice.validation;

import com.arsnyan.cloudstorageservice.validation.validator.URIPathConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = URIPathConstraintValidator.class)
public @interface ResourcePath {
    String message() default "Value is not a path";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
