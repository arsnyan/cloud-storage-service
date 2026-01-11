package com.arsnyan.cloudstorageservice.dto.resource;

import java.io.InputStream;

public record FileDownloadResponseDto(
    InputStream stream,
    String filename,
    long contentLength,
    String contentType
) {}
