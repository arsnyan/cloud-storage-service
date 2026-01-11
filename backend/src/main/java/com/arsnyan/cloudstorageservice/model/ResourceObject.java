package com.arsnyan.cloudstorageservice.model;

public record ResourceObject(
    String name,
    ResourceType type
) {
    public boolean isDirectory() {
        return type == ResourceType.DIRECTORY;
    }

    public boolean isFile() {
        return type == ResourceType.FILE;
    }
}
