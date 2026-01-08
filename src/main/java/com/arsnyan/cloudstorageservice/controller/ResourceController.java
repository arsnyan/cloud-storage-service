package com.arsnyan.cloudstorageservice.controller;

import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.service.FileStorageService;
import com.arsnyan.cloudstorageservice.validation.ResourcePath;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<@NonNull ResourceGetInfoResponseDto> getResourceInfo(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        var username = user.getUsername();
        return ResponseEntity.ok(fileStorageService.getDetailsForResource(username, path));
    }

    @DeleteMapping
    public ResponseEntity<@NonNull Void> deleteResource(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        var username = user.getUsername();
        fileStorageService.deleteResource(username, path);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<@NonNull Resource> downloadResource(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        var username = user.getUsername();
        File producedFile = fileStorageService.produceFileForPath(username, path);
        var fileResource = new FileSystemResource(producedFile);

        var contentDisposition = ContentDisposition
            .attachment()
            .name(fileResource.getFilename())
            .build();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(contentDisposition);

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(fileResource);
    }

    @GetMapping("/move")
    public ResponseEntity<@NonNull ResourceGetInfoResponseDto> moveResource(
        @RequestParam @Valid @ResourcePath String from,
        @RequestParam @Valid @ResourcePath String to,
        @AuthenticationPrincipal UserDetails user
    ) {
        var username = user.getUsername();
        var result = fileStorageService.moveResource(username, from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<@NonNull List<ResourceGetInfoResponseDto>> searchResource(
        @RequestParam String query,
        @AuthenticationPrincipal UserDetails user
    ) {
        var username = user.getUsername();
        var searchResults = fileStorageService.searchResources(username, query);
        return ResponseEntity.ok(searchResults);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<@NonNull List<ResourceGetInfoResponseDto>> uploadResource(
        @RequestParam @Valid @ResourcePath String path,
        @RequestParam("object") List<MultipartFile> files,
        @AuthenticationPrincipal UserDetails user
    ) {
        var username = user.getUsername();
        var result = fileStorageService.uploadResources(username, path, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
