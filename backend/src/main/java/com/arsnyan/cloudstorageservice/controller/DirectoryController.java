package com.arsnyan.cloudstorageservice.controller;

import com.arsnyan.cloudstorageservice.dto.resource.AddFolderResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.service.FileStorageService;
import com.arsnyan.cloudstorageservice.validation.ResourcePath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
@Validated
public class DirectoryController {
    private final FileStorageService fileStorageService;

    @GetMapping
    @Operation(
        summary = "Get contents of a folder resource"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Returns contents of a folder resource",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceGetInfoResponseDto.class)))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid path or path is not set"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User is not authenticated"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Folder resource is not found for this user"
        )
    })
    public ResponseEntity<@NonNull List<ResourceGetInfoResponseDto>> listObjects(
        @RequestParam(required = false) @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        var result = fileStorageService.listFolderContents(user.getUsername(), path);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(
        summary = "Add a folder resource"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Returns info about new folder resource",
            content = @Content(schema = @Schema(implementation = AddFolderResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid path or path is not set"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User is not authenticated"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Folder resource is not found for this user"
        )
    })
    public ResponseEntity<@NonNull AddFolderResponseDto> addFolder(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        var result = fileStorageService.createFolder(user.getUsername(), path);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
