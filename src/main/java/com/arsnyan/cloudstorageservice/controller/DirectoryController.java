package com.arsnyan.cloudstorageservice.controller;

import com.arsnyan.cloudstorageservice.dto.AddFolderResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.service.FileStorageService;
import com.arsnyan.cloudstorageservice.validation.ResourcePath;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
public class DirectoryController {
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<@NonNull List<ResourceGetInfoResponseDto>> listObjects(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        var result = fileStorageService.listAllObjectsForFolder(user.getUsername(), path);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<@NonNull AddFolderResponseDto> addFolder(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileStorageService.makeFolder(user.getUsername(), path));
    }
}
