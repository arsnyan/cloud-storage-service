package com.arsnyan.cloudstorageservice.dto.resource;

import com.arsnyan.cloudstorageservice.model.ResourceType;

public record AddFolderResponseDto(
    String path,
    String name,
    ResourceType type
) {}
