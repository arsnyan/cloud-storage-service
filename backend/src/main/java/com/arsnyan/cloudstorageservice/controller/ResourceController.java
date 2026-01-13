package com.arsnyan.cloudstorageservice.controller;

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
    @Operation(
        summary = "Get resource information"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Resource exists. Returns information about it",
            content = @Content(schema = @Schema(implementation = ResourceGetInfoResponseDto.class))
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
            description = "Resource is not found for this user"
        )
    })
    public ResponseEntity<@NonNull ResourceGetInfoResponseDto> getResourceInfo(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        var result = fileStorageService.getResourceInfo(user.getUsername(), path);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    @Operation(
        summary = "Remove resource"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Resource is removed"
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
            description = "Resource is not found for this user"
        )
    })
    public ResponseEntity<@NonNull Void> deleteResource(
        @RequestParam @Valid @ResourcePath String path,
        @AuthenticationPrincipal UserDetails user
    ) {
        fileStorageService.deleteResource(user.getUsername(), path);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(
        summary = "Download resource",
        description = "Produces either a file for resource or a zip archive for a folder with nested resources"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Resource is packed and returned as a file",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
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
            description = "Resource is not found for this user"
        )
    })
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
    @Operation(
        summary = "Move or rename resource"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Resource is moved or renamed. Returns information about a new moved resource",
            content = @Content(schema = @Schema(implementation = ResourceGetInfoResponseDto.class))
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
            description = "Resource is not found for this user"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Resource in output path already exists"
        )
    })
    public ResponseEntity<@NonNull ResourceGetInfoResponseDto> moveResource(
        @RequestParam @Valid @ResourcePath String from,
        @RequestParam @Valid @ResourcePath String to,
        @AuthenticationPrincipal UserDetails user
    ) {
        var result = fileStorageService.moveResource(user.getUsername(), from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search resources",
        description = "Searches resources for user across all of their resources"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Returns an array of all found resources",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceGetInfoResponseDto.class)))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid path or search path is not set"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User is not authenticated"
        )
    })
    public ResponseEntity<@NonNull List<ResourceGetInfoResponseDto>> searchResource(
        @RequestParam @NotBlank(message = "Search query must not be empty") String query,
        @AuthenticationPrincipal UserDetails user
    ) {
        var searchResults = fileStorageService.searchResources(user.getUsername(), query);
        return ResponseEntity.ok(searchResults);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload files"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Files are uploaded as resources",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceGetInfoResponseDto.class)))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid path or files upload had errors"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User is not authenticated"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Resource in output path already exists"
        )
    })
    public ResponseEntity<@NonNull List<ResourceGetInfoResponseDto>> uploadResource(
        @RequestParam @Valid @ResourcePath String path,
        @RequestParam("object") List<MultipartFile> files,
        @AuthenticationPrincipal UserDetails user
    ) {
        var result = fileStorageService.uploadResources(user.getUsername(), path, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
