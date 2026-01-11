package com.arsnyan.cloudstorageservice.util;

import com.arsnyan.cloudstorageservice.model.ResourceType;

public class FileUtils {
    public static ResourceType getResourceType(String path) {
        if (path.endsWith("/")) {
            return ResourceType.DIRECTORY;
        } else {
            return ResourceType.FILE;
        }
    }

    public static String getRelativeZipPath(Long userId, String absoluteObjectKey) {
        var userPrefix = "user-%s-files/".formatted(userId);

        if (absoluteObjectKey.startsWith(userPrefix)) {
            return absoluteObjectKey.substring(userPrefix.length());
        }

        return absoluteObjectKey;
    }

    public static String resolvePath(Long userId, String path) {
        return "user-%s-files/%s".formatted(userId, path);
    }

    public static String getParentPath(String path) {
        if (path == null || path.isEmpty()) return "/";

        var normalized = path.endsWith("/") && path.length() > 1
            ? path.substring(0, path.length() - 1)
            : path;

        var lastSlash = normalized.lastIndexOf("/");
        if (lastSlash < 0) return "/";

        return normalized.substring(0, lastSlash + 1);
    }

    public static String extractResourceName(String path) {
        var delimiterCount = path.split("/", -1).length - 1;
        if (delimiterCount == 0) return path;

        var lastDelimiter = path.lastIndexOf("/");

        if (!path.endsWith("/")) {
            return path.substring(lastDelimiter + 1);
        }

        var pathWithoutLastDelimiter = path.substring(0, lastDelimiter);
        var secondToLastDelimiter = pathWithoutLastDelimiter.lastIndexOf("/") + 1;

        return path.substring(secondToLastDelimiter, lastDelimiter + 1);
    }
}
