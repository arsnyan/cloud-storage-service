package com.arsnyan.cloudstorageservice.validation.validator;

import com.arsnyan.cloudstorageservice.validation.ResourcePath;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class URIPathConstraintValidator implements ConstraintValidator<ResourcePath, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        if (value.startsWith("/")) {
            return false;
        }

        var isFolder = value.endsWith("/");
        var isFile = Pattern.matches("/.+[^/]$", value);

        return isFolder || isFile;
    }
}
