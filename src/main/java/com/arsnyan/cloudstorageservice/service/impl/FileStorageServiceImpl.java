package com.arsnyan.cloudstorageservice.service.impl;

import com.arsnyan.cloudstorageservice.dto.AddFolderResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.exception.NoSuchEntityException;
import com.arsnyan.cloudstorageservice.exception.ServerErrorException;
import com.arsnyan.cloudstorageservice.model.ResourceType;
import com.arsnyan.cloudstorageservice.repository.UserRepository;
import com.arsnyan.cloudstorageservice.service.FileStorageService;
import com.google.common.collect.Streams;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    private final MinioClient minioClient;
    private final UserRepository userRepository;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Value("${app.minio.root-bucket-name}")
    private String rootBucket;

    @Override
    public ResourceGetInfoResponseDto getDetailsForResource(String username, String path) {
        var userId = getUserIdByUsername(username);
        var userFolder = getUserFolder(userId);
        var fullPath = resolveKey(userFolder, path);

        try {
            var resourceType = getResourceType(path);
            var resourceName = getResourceName(path);
            var leadingPath = getLeadingPath(path);
            long objectSize = 0;

            if (resourceType == ResourceType.FILE) {
                var response = minioClient.getObjectAttributes(GetObjectAttributesArgs.builder()
                    .bucket(rootBucket)
                    .object(fullPath)
                    .objectAttributes(new String[] {"ObjectSize"})
                    .build()
                );
                objectSize = response.result().objectSize();
            }

            return new ResourceGetInfoResponseDto(
                leadingPath,
                resourceName,
                objectSize,
                resourceType
            );
        } catch (Exception e) {
            throw makeServerException(e);
        }
    }

    @Override
    public void deleteResource(String username, String path) {
        var userId = getUserIdByUsername(username);
        var userFolder = getUserFolder(userId);
        var fullPath = resolveKey(userFolder, path);

        try {
            removeObject(fullPath);

            if (getResourceType(path) == ResourceType.DIRECTORY) {
                var directoryContents = getDirectoryContents(userFolder, path);
                for (var objectPath : directoryContents) {
                    removeObject(objectPath);
                }
            }
        } catch (Exception e) {
            throw makeServerException(e);
        }
    }

    @Override
    public File produceFileForPath(String username, String path) {
        var userId = getUserIdByUsername(username);
        var userFolder = getUserFolder(userId);

        try {
            var fileName = getResourceName(path);
            var resourceType = getResourceType(path);

            return switch (resourceType) {
                case FILE -> {
                    var tempFile = File.createTempFile("minio-", "-" + new File(fileName).getName());

                    minioClient.downloadObject(DownloadObjectArgs.builder()
                        .bucket(rootBucket)
                        .object(resolveKey(userFolder, path))
                        .filename(tempFile.getAbsolutePath())
                        .build());

                    yield tempFile;
                }
                case DIRECTORY -> {
                    var nestedDirectoryContent = getDirectoryContents(userFolder, path);

                    var tempDir = Files.createTempDirectory("minio-download-dir-");
                    for (var contentPath : nestedDirectoryContent) {
                        var relativePath = contentPath.substring(userFolder.length());
                        var targetPath = tempDir.resolve(relativePath);

                        Files.createDirectories(targetPath.getParent());

                        minioClient.downloadObject(DownloadObjectArgs.builder()
                                .bucket(rootBucket)
                                .object(contentPath)
                                .filename(targetPath.toString())
                                .build());
                    }

                    var zipFile = File.createTempFile("minio-", ".zip");
                    zipDirectory(tempDir, zipFile);
                    deleteDirectoryRecursively(tempDir);

                    yield zipFile;
                }
            };
        } catch (Exception e) {
            throw makeServerException(e);
        }
    }

    @Override
    public ResourceGetInfoResponseDto moveResource(String username, String from, String to) {
        var userId = getUserIdByUsername(username);
        var userFolder = getUserFolder(userId);

        var sourceKey = resolveKey(userFolder, from);
        var targetKey = resolveKey(userFolder, to);

        try {
            var resourceType = getResourceType(from);

            switch (resourceType) {
                case FILE -> {
                    copyObject(sourceKey, targetKey);
                    removeObject(sourceKey);
                }
                case DIRECTORY -> {
                    var contents = getDirectoryContents(userFolder, from);

                    for (var objectPath : contents) {
                        var relativePath = objectPath.substring(sourceKey.length());
                        var newObjectPath = targetKey + relativePath;

                        copyObject(objectPath, newObjectPath);
                        removeObject(objectPath);
                    }
                }
            }

            return getDetailsForResource(username, to);
        } catch (Exception e) {
            throw makeServerException(e);
        }
    }

    @Override
    public List<ResourceGetInfoResponseDto> searchResources(String username, String query) {
        var userId = getUserIdByUsername(username);
        var userFolder = getUserFolder(userId);

        try {
            // MinIO doesn't have a search function, so brute-forcing it is
            var allObjects = getDirectoryContents(userFolder, query);
            var filteredObjects = allObjects.stream().filter(obj -> obj.contains(query));

            return filteredObjects.map(path -> {
                var type = getResourceType(path);
                long size = 0;

                if (type == ResourceType.FILE) {
                    try {
                        size = getDetailsForResource(username, path).size();
                    } catch (Exception ignored) {}
                }

                return new ResourceGetInfoResponseDto(
                    getLeadingPath(path),
                    getResourceName(path),
                    size,
                    type
                );
            }).toList();
        } catch (Exception e) {
            throw makeServerException(e);
        }
    }

    @Override
    public List<ResourceGetInfoResponseDto> uploadResources(String username, String path, List<MultipartFile> files) {
        var userId = getUserIdByUsername(username);
        var userFolder = getUserFolder(userId);

        try {
            var snowballObjects = new ArrayList<SnowballObject>();
            var responseDtos = new ArrayList<ResourceGetInfoResponseDto>();

            var resolvedBasePath = resolveKey(userFolder, path);

            if (!resolvedBasePath.endsWith("/")) resolvedBasePath += "/";

            for (MultipartFile file : files) {
                var originalFileName = file.getOriginalFilename();
                var resourceType = getResourceType(originalFileName);
                var objectName = resolvedBasePath + originalFileName;

                snowballObjects.add(new SnowballObject(
                    objectName,
                    file.getInputStream(),
                    file.getSize(),
                    ZonedDateTime.now()
                ));

                responseDtos.add(new ResourceGetInfoResponseDto(
                    path,
                    originalFileName,
                    file.getSize(),
                    resourceType
                ));
            }

            minioClient.uploadSnowballObjects(UploadSnowballObjectsArgs.builder()
                    .bucket(rootBucket)
                    .objects(snowballObjects)
                    .build());

            return responseDtos;
        } catch (Exception e) {
            throw makeServerException(e);
        }
    }

    @Override
    public List<ResourceGetInfoResponseDto> listAllObjectsForFolder(String username, String path) {
        var userId = getUserIdByUsername(username);
        var userFolder = getUserFolder(userId);

        var contents = getDirectoryContents(userFolder, path, false);
        return contents.stream().map(obj -> getDetailsForResource(username, obj)).toList();
    }

    @Override
    public AddFolderResponseDto makeFolder(String username, String path) {
        var userId = getUserIdByUsername(username);
        var userFolder = getUserFolder(userId);

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(rootBucket)
                    .object(resolveKey(userFolder, path))
                    .stream(new ByteArrayInputStream(new byte[] {}), 0, -1)
                    .build());

            return new AddFolderResponseDto(
                getLeadingPath(path),
                getResourceName(path),
                ResourceType.DIRECTORY
            );
        } catch (Exception e) {
            throw makeServerException(e);
        }
    }

    private Long getUserIdByUsername(String username) {
        return userRepository.getUserIdByUsername(username)
            .orElseThrow(() -> new NoSuchEntityException("User %s does not exist".formatted(username)));
    }

    private String getUserFolder(Long userId) {
        return "user-%s-files/".formatted(userId);
    }

    private List<String> getDirectoryContents(String userFolder, String path) {
        return getDirectoryContents(userFolder, path, true);
    }

    private List<String> getDirectoryContents(String userFolder, String path, boolean recursive) {
        return Streams.stream(minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(rootBucket)
                    .prefix(userFolder + path)
                    .recursive(recursive)
                    .build()
            ).iterator())
            .filter(Objects::nonNull)
            .map(objectItem -> {
                try {
                    return objectItem.get().objectName();
                } catch (Exception e) {
                    throw makeServerException(e);
                }
            })
            .toList();
    }

    private ResourceType getResourceType(String path) {
        if (path != null && path.endsWith("/")) {
            return ResourceType.DIRECTORY;
        }
        return ResourceType.FILE;
    }

    private String getResourceName(String path) {
        if (path == null || path.isEmpty()) return "";

        int endIndex = path.length();
        if (path.endsWith("/")) {
            endIndex--;
        }

        int lastSlash = path.lastIndexOf('/', endIndex - 1);
        if (lastSlash == -1) {
            return path;
        }
        return path.substring(lastSlash + 1);
    }

    private String getLeadingPath(String path) {
        int endIndex = path.length();
        if (path.endsWith("/")) {
            endIndex--;
        }

        int lastSlash = path.lastIndexOf('/', endIndex - 1);
        if (lastSlash == -1) {
            return "";
        }
        return path.substring(0, lastSlash + 1);
    }

    private void removeObject(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(rootBucket)
                .object(path)
                .build());
        } catch (Exception e) {
            throw makeServerException(e);
        }
    }

    private void copyObject(String source, String target) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(rootBucket)
                    .object(target)
                    .source(CopySource.builder()
                        .bucket(rootBucket)
                        .object(source)
                        .build())
                .build());
        } catch (Exception e) {
            throw makeServerException(e);
        }
    }

    private ServerErrorException makeServerException(Exception e) {
        log.error(e.getMessage(), e);
        if (e instanceof ErrorResponseException) {
            if (((ErrorResponseException) e).errorResponse().code().equals("NoSuchKey")) {
                throw new NoSuchEntityException(e.getMessage());
            }
        }
        return new ServerErrorException("MinioClient resulted with error:", e);
    }

    private void zipDirectory(Path sourceDir, File zipFile) throws IOException {
        try (var zos = new ZipOutputStream(new FileOutputStream(zipFile));
             var stream = Files.walk(sourceDir)) {

            stream
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        var zipEntryName = sourceDir.relativize(path).toString();
                        var zipEntry = new ZipEntry(zipEntryName);

                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
    }

    private void deleteDirectoryRecursively(Path path) {
        threadPoolTaskExecutor.execute(() -> {
            try (var walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (Exception e) {
                log.error("Error while trying to delete files from temp directory: {}", e.getMessage());
            }
        });
    }

    private String resolveKey(String userFolder, String path) {
        // Remove leading slash if present to avoid double slashes (e.g. /user-1 vs user-1)
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // If the path already starts with the user folder, return it as is
        if (path.startsWith(userFolder)) {
            return path;
        }

        // Otherwise, prepend the user folder
        return userFolder + path;
    }
}
