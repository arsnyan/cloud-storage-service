package com.arsnyan.cloudstorageservice.service.impl;

import com.arsnyan.cloudstorageservice.dto.resource.AddFolderResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.FileDownloadResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.exception.EntityAlreadyExistsException;
import com.arsnyan.cloudstorageservice.exception.MinioWrappedException;
import com.arsnyan.cloudstorageservice.exception.NoSuchEntityException;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
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
        var userId = getUserId(username);
        var resolvedPath = resolvePath(userId, path);
        var userRootPath = resolvePath(userId, "");

        if (!path.endsWith("/")) {
            s3Client.removeObject(resolvedPath);

            var parentPath = getParentPath(path);
            if (parentPath.length() > userRootPath.length()) {
                preserveParentFolder(username, parentPath);
            }
        } else {
            var minioResults = s3Client.listObjects(resolvedPath, true);
            var resourceItems = mapResultsToResourceObjects(minioResults);
            resourceItems.forEach(item -> s3Client.removeObject(item.name()));
        }
    }

    private void preserveParentFolder(String username, String parentPath) {
        if (!parentPath.isEmpty() && !parentPath.equals(resolvePath(getUserId(username), ""))) {
            var remainingItems = s3Client.listObjects(parentPath, false);
            var hasContents = Streams.stream(remainingItems)
                .map(ResourceMapper::mapRawObjectToItem)
                .anyMatch(item -> !item.objectName().equals(parentPath));

            if (!hasContents) {
                s3Client.ensureFolderPlaceholderExists(parentPath);
            }
        }
    }

    @Override
    public FileDownloadResponseDto getDownloadableResource(String username, String path) {
        var resolvedPath = resolvePath(getUserId(username), path);
        var filename = extractResourceName(path);

        if (!path.endsWith("/")) {
            var objectStats = s3Client.getStatObject(resolvedPath);
            if (objectStats == null) {
                throw new NoSuchEntityException("Resource not found");
            }

            var stream = s3Client.getObject(resolvedPath);

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
        var userId = getUserId(username);
        var resolvedPathFrom = resolvePath(userId, from);
        var resolvedPathTo = resolvePath(userId, to);
        var sourceParentPath = getParentPath(resolvedPathFrom);
        var userRootPath = resolvePath(userId, from);

        var nestedObjects = Streams.stream(s3Client.listObjects(resolvedPathFrom, true))
            .map(ResourceMapper::mapRawObjectToItem)
            .toList();

        if (nestedObjects.isEmpty()) {
            throw new NoSuchEntityException("Source resource not found %s".formatted(from));
        }

        var processedFolders = new HashSet<String>();
        for (var item : nestedObjects) {
            var objectName = item.objectName();
            var relativePath = objectName.replaceFirst(Pattern.quote(resolvedPathFrom), "");
            var finalPath = resolvedPathTo + relativePath;

            if (finalPath.endsWith("/")) {
                if (s3Client.hasNamingConflict(finalPath)) {
                    throw new EntityAlreadyExistsException("File already exists");
                }
                s3Client.ensureFolderPlaceholderExists(finalPath);
                processedFolders.add(finalPath);
            } else {
                if (s3Client.isPathAvailable(finalPath)) {
                    throw new EntityAlreadyExistsException("Destination already exists");
                }

                var parent = getParentPath(finalPath);

                while (parent.length() > userRootPath.length() && processedFolders.add(parent)) {
                    if (s3Client.hasNamingConflict(parent)) {
                        throw new EntityAlreadyExistsException("File already exists, cannot create parent folder");
                    }
                    s3Client.ensureFolderPlaceholderExists(parent);
                    parent = getParentPath(parent);
                }
            }

            s3Client.copyObject(item, finalPath);
            s3Client.removeObject(objectName);
        }

        if (sourceParentPath.length() > userRootPath.length()) {
            preserveParentFolder(username, sourceParentPath);
        }

        return getResourceInfo(username, to);
    }

    @Override
    public List<ResourceGetInfoResponseDto> searchResources(String username, String query) {
        var allUserResources = listObjectsAsDto(s3Client.listObjects(resolvePath(getUserId(username), ""), true), "");

        return allUserResources.stream()
            .filter(object -> object.name().contains(query))
            .toList();
    }

    @Override
    public List<ResourceGetInfoResponseDto> uploadResources(String username, String path, List<MultipartFile> files) {
        try {
            var userId = getUserId(username);

            for (MultipartFile file : files) {
                var filename = file.getOriginalFilename();
                if (filename == null) continue;

                var filePath = resolvePath(userId, path + filename);
                if (s3Client.isPathAvailable(filePath)) {
                    throw new EntityAlreadyExistsException("File %s already exists".formatted(filename));
                }

                if (filename.contains("/")) {
                    var parts = filename.split("/");
                    var folderBuilder = new StringBuilder(path);

                    for (int i = 0; i < parts.length - 1; i++) {
                        folderBuilder.append(parts[i]).append("/");
                        var folderPath = resolvePath(userId, folderBuilder.toString());

                        if (s3Client.hasNamingConflict(folderPath)) {
                            var folderName = extractResourceName(folderPath);
                            throw new EntityAlreadyExistsException("File %s already exists, cannot create folder"
                                .formatted(folderName));
                        }

                        s3Client.ensureFolderPlaceholderExists(folderPath);
                    }
                }
            }

            var snowballObjects = mapToSnowballObjects(userId, path, files);
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

        if (s3Client.isPathAvailable(resolvedPath)) {
            throw new EntityAlreadyExistsException("Object already exists");
        }

        s3Client.makeFolderInS3(resolvedPath);

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
        var zipFilename = extractResourceName(path.substring(0, path.length() - 1)) + ".zip";

        StreamingResponseBody streamingBody = outputStream -> {
            try (var zipStream = new ZipOutputStream(outputStream)) {
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
            }
        };

        return new FileDownloadResponseDto(
            null,
            zipFilename,
            -1L,
            "application/zip",
            streamingBody
        );
    }
}