package com.arsnyan.cloudstorageservice.service.impl;

import com.arsnyan.cloudstorageservice.dto.resource.AddFolderResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.FileDownloadResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.exception.EntityAlreadyExistsException;
import com.arsnyan.cloudstorageservice.exception.MinioWrappedException;
import com.arsnyan.cloudstorageservice.exception.NoSuchEntityException;
import com.arsnyan.cloudstorageservice.exception.ServerErrorException;
import com.arsnyan.cloudstorageservice.mapper.ResourceMapper;
import com.arsnyan.cloudstorageservice.model.ResourceType;
import com.arsnyan.cloudstorageservice.repository.UserRepository;
import com.arsnyan.cloudstorageservice.service.FileStorageService;
import com.arsnyan.cloudstorageservice.util.MinioS3Client;
import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.arsnyan.cloudstorageservice.mapper.ResourceMapper.*;
import static com.arsnyan.cloudstorageservice.util.FileUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    private final UserRepository userRepository;
    private final MinioS3Client s3Client;

    @Override
    public ResourceGetInfoResponseDto getResourceInfo(String username, String path) {
        var resolvedPath = resolvePath(getUserId(username), path);
        var objectStats = s3Client.getStatObject(resolvedPath);

        return new ResourceGetInfoResponseDto(
            getParentPath(path),
            extractResourceName(path),
            objectStats != null ? objectStats.size() : null,
            getResourceType(path)
        );
    }

    @Override
    public void deleteResource(String username, String path) {
        var resolvedPath = resolvePath(getUserId(username), path);

        if (!path.endsWith("/")) {
            s3Client.removeObject(resolvedPath);
        } else {
            var minioResults = s3Client.listObjects(resolvedPath, true);
            var resourceItems = mapResultsToResourceObjects(minioResults);
            resourceItems.forEach(item -> s3Client.removeObject(item.name()));
        }
    }

    @Override
    public FileDownloadResponseDto getDownloadableResource(String username, String path) {
        var resolvedPath = resolvePath(getUserId(username), path);
        var filename = extractResourceName(path);

        if (!path.endsWith("/")) {
            var objectStats = s3Client.getStatObject(resolvedPath);
            var stream = s3Client.getObject(resolvedPath);

            assert objectStats != null;
            return new FileDownloadResponseDto(
                stream,
                filename,
                objectStats.size(),
                objectStats.contentType()
            );
        }

        return streamFolderAsZip(username, path, resolvedPath);
    }

    @Override
    public ResourceGetInfoResponseDto moveResource(String username, String from, String to) {
        var resolvedPathFrom = resolvePath(getUserId(username), from);
        var resolvedPathTo = resolvePath(getUserId(username), to);

        var nestedObjects = Streams.stream(s3Client.listObjects(resolvedPathFrom, true))
            .map(ResourceMapper::mapRawObjectToItem)
            .toList();

        for (var item : nestedObjects) {
            var objectName = item.objectName();
            var relativePath = objectName.replaceFirst(resolvedPathFrom, "");
            var finalPath = resolvedPathTo + relativePath;

            if (s3Client.isPathUnavailable(finalPath)) {
                s3Client.copyObject(item, finalPath);
                s3Client.removeObject(objectName);
            } else {
                log.error("Failed to copy object {} to {}", item, finalPath);
                throw new EntityAlreadyExistsException("Object %s already exists".formatted(finalPath));
            }
        }

        return getResourceInfo(username, to);
    }

    @Override
    public List<ResourceGetInfoResponseDto> searchResources(String username, String query) {
        var allUserResources = listObjectsAsDto(s3Client.listObjects("", true), "");

        return allUserResources.stream()
            .filter(object -> object.name().contains(query))
            .toList();
    }

    @Override
    public List<ResourceGetInfoResponseDto> uploadResources(String username, String path, List<MultipartFile> files) {
        try {
            var snowballObjects = mapToSnowballObjects(getUserId(username), path, files);

            s3Client.uploadSnowballObject(snowballObjects);

            return mapFilesToDto(path, files);
        } catch (Exception e) {
            log.error("Failed to upload files for path {}: {}", path, e.getMessage());
            throw MinioWrappedException.from(e);
        }
    }

    @Override
    public List<ResourceGetInfoResponseDto> listFolderContents(String username, String path) {
        var resolvedPath = resolvePath(getUserId(username), path);
        return listObjectsAsDto(s3Client.listObjects(resolvedPath, false), resolvedPath);
    }

    @Override
    public AddFolderResponseDto createFolder(String username, String path) {
        var resolvedPath = resolvePath(getUserId(username), path);

        if (s3Client.isPathUnavailable(resolvedPath)) {
            s3Client.makeFolderInS3(resolvedPath);
        } else {
            throw new EntityAlreadyExistsException("Object %s already exists".formatted(resolvedPath));
        }

        return new AddFolderResponseDto(
            getParentPath(path),
            extractResourceName(path),
            ResourceType.DIRECTORY
        );
    }

    private Long getUserId(String username) {
        return userRepository.getUserIdByUsername(username)
            .orElseThrow(() -> new NoSuchEntityException("User not found"));
    }

    private FileDownloadResponseDto streamFolderAsZip(String username, String path, String resolvedPath) {
        try {
            var pipedInput = new PipedInputStream();
            var pipedOutput = new PipedOutputStream(pipedInput);

            var zipFilename = extractResourceName(path.substring(0, path.length() - 1)) + ".zip";

            CompletableFuture.runAsync(() -> {
                try (var zipStream = new ZipOutputStream(pipedOutput)) {
                    var items = Streams.stream(s3Client.listObjects(resolvedPath, true))
                        .map(ResourceMapper::mapRawObjectToItem)
                        .toList();

                    for (var item : items) {
                        if (item.isDir()) continue;

                        var absoluteKey = item.objectName();
                        var relativeZipPath = getRelativeZipPath(getUserId(username), absoluteKey);

                        var entry = new ZipEntry(relativeZipPath);
                        zipStream.putNextEntry(entry);

                        try (var entryStream = s3Client.getObject(absoluteKey)) {
                            entryStream.transferTo(zipStream);
                        }
                        zipStream.closeEntry();
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new ServerErrorException("Failed to create zip stream", e);
                }
            });

            return new FileDownloadResponseDto(
                pipedInput,
                zipFilename,
                -1L,
                "application/zip"
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}