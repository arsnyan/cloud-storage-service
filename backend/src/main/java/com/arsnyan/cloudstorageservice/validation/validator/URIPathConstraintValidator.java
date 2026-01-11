package com.arsnyan.cloudstorageservice.validation.validator;

import com.arsnyan.cloudstorageservice.validation.ResourcePath;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;
import java.net.URI;

public class URIPathConstraintValidator implements ConstraintValidator<ResourcePath, String> {
    private static final Pattern INVALID_SEGMENTS = Pattern.compile("(^|/)\\.\\.(?:/|$)");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String normalized = value.trim();

        // must be a relative path
        if (normalized.startsWith("/")) {
            return false;
        }

        // prevent directory traversal
        if (INVALID_SEGMENTS.matcher(normalized).find()) {
            return false;
        }

        // basic URI syntax check
        try {
            URI uri = URI.create(normalized);
            return uri.getPath() != null && !uri.getPath().isEmpty();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
