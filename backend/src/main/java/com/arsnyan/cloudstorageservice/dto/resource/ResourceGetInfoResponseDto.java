package com.arsnyan.cloudstorageservice.dto.resource;

import com.arsnyan.cloudstorageservice.model.ResourceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceGetInfoResponseDto(
    String path,
    String name,
    Long size,
    ResourceType type
) {}
