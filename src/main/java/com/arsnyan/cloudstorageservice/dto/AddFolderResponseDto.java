package com.arsnyan.cloudstorageservice.dto;

import com.arsnyan.cloudstorageservice.model.ResourceType;

public record AddFolderResponseDto(
    String path,
    String name,
    ResourceType type
) {}
