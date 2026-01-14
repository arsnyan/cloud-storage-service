package com.arsnyan.cloudstorageservice.dto.resource;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

public record FileDownloadResponseDto(
    InputStream stream,
    String filename,
    long contentLength,
    String contentType,
    StreamingResponseBody streamingResponseBody
) {
    public FileDownloadResponseDto(InputStream stream, String filename, long contentLength, String contentType) {
        this(stream, filename, contentLength, contentType, null);
    }
}
