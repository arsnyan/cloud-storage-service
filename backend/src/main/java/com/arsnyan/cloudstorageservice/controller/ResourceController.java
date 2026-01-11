package com.arsnyan.cloudstorageservice.controller;

import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.service.FileStorageService;
import com.arsnyan.cloudstorageservice.validation.ResourcePath;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
@Validated
public class ResourceController {
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<@NonNull ResourceGetInfoResponseDto> getResourceInfo(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        var result = fileStorageService.getResourceInfo(user.getUsername(), path);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    public ResponseEntity<@NonNull Void> deleteResource(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        fileStorageService.deleteResource(user.getUsername(), path);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<@NonNull Resource> downloadResource(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        var fileData = fileStorageService.getDownloadableResource(user.getUsername(), path);

        var contentDisposition = ContentDisposition
            .attachment()
            .name(fileData.filename())
            .build();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(contentDisposition);

        if (fileData.contentLength() > 0) {
            headers.setContentLength(fileData.contentLength());
        }

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(new InputStreamResource(fileData.stream()));
    }

    @GetMapping("/move")
    public ResponseEntity<@NonNull ResourceGetInfoResponseDto> moveResource(
        @RequestParam @Valid @ResourcePath String from,
        @RequestParam @Valid @ResourcePath String to,
        @AuthenticationPrincipal UserDetails user
    ) {
        var result = fileStorageService.moveResource(user.getUsername(), from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<@NonNull List<ResourceGetInfoResponseDto>> searchResource(
        @RequestParam @NotBlank(message = "Search query must not be empty") String query,
        @AuthenticationPrincipal UserDetails user
    ) {
        var searchResults = fileStorageService.searchResources(user.getUsername(), query);
        return ResponseEntity.ok(searchResults);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<@NonNull List<ResourceGetInfoResponseDto>> uploadResource(
        @RequestParam @Valid @ResourcePath String path,
        @RequestParam("object") List<MultipartFile> files,
        @AuthenticationPrincipal UserDetails user
    ) {
        var result = fileStorageService.uploadResources(user.getUsername(), path, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
