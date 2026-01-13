package com.arsnyan.cloudstorageservice.validation.validator;

import com.arsnyan.cloudstorageservice.validation.ResourcePath;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        if (normalized.startsWith("/") || normalized.contains("//")) {
            return false;
        }

        // prevent directory traversal
        if (INVALID_SEGMENTS.matcher(normalized).find()) {
            return false;
        }

        // basic URI syntax check - encode path segments to handle spaces and special characters
        try {
            String[] segments = normalized.split("/", -1);
            StringBuilder encodedPath = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (i > 0) {
                    encodedPath.append("/");
                }
                encodedPath.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8));
            }
            URI uri = URI.create(encodedPath.toString());
            return uri.getPath() != null && !uri.getPath().isEmpty();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
