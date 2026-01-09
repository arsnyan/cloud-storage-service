package com.arsnyan.cloudstorageservice.service.impl;

import com.arsnyan.cloudstorageservice.dto.AddFolderResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.exception.NoSuchEntityException;
import com.arsnyan.cloudstorageservice.exception.ResourceAlreadyExistsException;
import com.arsnyan.cloudstorageservice.exception.ServerErrorException;
import com.arsnyan.cloudstorageservice.model.Resource;
import com.arsnyan.cloudstorageservice.model.ResourceType;
import com.arsnyan.cloudstorageservice.model.User;
import com.arsnyan.cloudstorageservice.repository.ResourceRepository;
import com.arsnyan.cloudstorageservice.repository.UserRepository;
import com.arsnyan.cloudstorageservice.service.FileStorageService;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    private final MinioClient minioClient;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    @Value("${app.minio.root-bucket-name}")
    private String rootBucket;

    @Override
    @Transactional(readOnly = true)
    public ResourceGetInfoResponseDto getDetailsForResource(String username, String path) {
        var user = resolveUser(username);
        var resource = resolvePathToResource(user, path);
        return toDto(resource);
    }

    @Override
    @Transactional
    public void deleteResource(String username, String path) {

    }

    @Override
    public void deleteResourceById(UUID uuid) {

    }

    @Override
    public File produceFileForPath(String username, String path) {
        return null;
    }

    @Override
    public ResourceGetInfoResponseDto moveResource(String username, String from, String to) {
        return null;
    }

    @Override
    public List<ResourceGetInfoResponseDto> searchResources(String username, String query) {
        return List.of();
    }

    @Override
    public List<ResourceGetInfoResponseDto> uploadResources(String username, String path, List<MultipartFile> files) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceGetInfoResponseDto> listAllObjectsForFolder(String username, String path) {
        var user = resolveUser(username);

        List<Resource> children;
        if (path == null || path.isEmpty()) {
            children = resourceRepository.findByOwnerAndParentIsNull(user);
        } else {
            var parent = resolvePathToResource(user, path);

            if (parent.isFile()) {
                throw new NoSuchEntityException("Path is not a directory");
            }

            children = resourceRepository.findByOwnerAndParent(user, parent);
        }

        return children.stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    @Transactional
    public AddFolderResponseDto makeFolder(String username, String path) {
        var user = resolveUser(username);
        var name = extractName(path);
        var parentPath = extractParentPath(path);

        var parent = resolveParentPath(user, parentPath);

        boolean exists;
        if (parent == null) {
            exists = resourceRepository.existsByOwnerAndParentIsNullAndNameAndType(
                user, name, ResourceType.DIRECTORY
            );
        } else {
            exists = resourceRepository.existsByOwnerAndParentAndNameAndType(
                user, parent, name, ResourceType.DIRECTORY
            );
        }

        if (exists) {
            throw ResourceAlreadyExistsException.forPath(path);
        }

        var directory = Resource.directory(name, user, parent);
        directory = resourceRepository.save(directory);

        log.info("Created directory: {} for user: {}", path, username);

        return new AddFolderResponseDto(parentPath, name, ResourceType.DIRECTORY);
    }

    @Override
    @Transactional
    public Resource ensureDirectoryPath(User user, String directoryPath) {
        if (directoryPath == null || directoryPath.isEmpty()) {
            return null;
        }

        var segments = splitPathIntoSegments(directoryPath);
        Resource current = null;

        for (var segment : segments) {
            Optional<Resource> existing;
            if (current == null) {
                existing = resourceRepository.findByOwnerAndParentIsNullAndNameAndType(
                    user, segment, ResourceType.DIRECTORY
                );
            } else {
                existing = resourceRepository.findByOwnerAndParentAndNameAndType(
                    user, current, segment, ResourceType.DIRECTORY
                );
            }

            if (existing.isPresent()) {
                current = existing.get();
            } else {
                var newDir = Resource.directory(segment, user, current);
                current = resourceRepository.save(newDir);
                log.debug("Created intermediate directory: {} for user: {}", segment, user.getUsername());
            }
        }

        return current;
    }

    private List<String> splitPathIntoSegments(String path) {
        if (path == null || path.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.asList(path.split("/")).stream()
            .filter(segment -> !segment.isEmpty())
            .toList();
    }

    private boolean isDirectoryPath(String path) {
        return path != null && path.endsWith("/");
    }

    private String extractName(String path) {
        var segments = splitPathIntoSegments(path);
        if (segments.isEmpty()) {
            return "";
        }
        return segments.getLast();
    }

    private String extractParentPath(String path) {
        var segments = splitPathIntoSegments(path);
        if (segments.size() <= 1) {
            return "";
        }

        var parentPath = new StringBuilder();
        for (int i = 0; i < segments.size() - 1; i++) {
            parentPath.append(segments.get(i)).append("/");
        }
        return parentPath.toString();
    }

    private User resolveUser(String username) {
        return userRepository.getUserByUsername(username)
            .orElseThrow(() -> new NoSuchEntityException("User not found: " + username));
    }

    private Resource resolvePathToResource(User user, String path) {
        var segments = splitPathIntoSegments(path);
        if (sections.isEmpty()) {
            throw new NoSuchEntityException("Cannot resolve empty path. Use listRootContents for root listing");
        }

        boolean isDirectory = isDirectoryPath(path);
        Resource current;

        for (int i = 0; i < segments.size(); i++) {
            var segment = segments.get(i);
            boolean isLastSegment = (i == segments.size() - 1);

            var targetType = isLastSegment && !isDirectory
                ? ResourceType.FILE
                : ResourceType.DIRECTORY;

            Optional<Resource> found;
            if (current == null) {
                found = resourceRepository.findByOwnerAndParentIsNullAndNameAndType(
                    user, segment, targetType
                );
            } else {
                found = resourceRepository.findByOwnerAndParentAndNameAndType(
                    user, current, segment, targetType
                );
            }

            current = found.orElseThrow(() -> new NoSuchEntityException("Resource not found: " + path));
        }

        return current;
    }

    private Resource resolveParentPathToResource(User user, String parentPath) {
        if (parentPath == null || parentPath.isEmpty()) {
            return null;
        }

        if (!parentPath.endsWith("/")) {
            parentPath = parentPath + "/";
        }

        return resolvePathToResource(user, parentPath);
    }

    private ResourceGetInfoResponseDto toDto(Resource resource) {
        var parentPath = computeParentPath(resource);

        return new ResourceGetInfoResponseDto(
            parentPath,
            resource.getName(),
            resource.getSize(),
            resource.getType()
        );
    }

    private String computeParentPath(Resource resource) {
        var parentNames = new ArrayList<String>();
        var current = resource.getParent();

        while (current != null) {
            parentNames.add(current.getName());
            current = current.getParent();
        }

        Collections.reverse(parentNames);

        if (parentNames.isEmpty()) {
            return "";
        }

        return String.join("/", parentNames) + "/";
    }

    private String computeFileName(Resource resource) {
        var parentPath = computeParentPath(resource);
        var suffix = resource.isDirectory() ? "/" : "";
        return parentPath + resource.getName() + suffix;
    }
}


//private ServerErrorException makeServerException(Exception e) {
//    log.error(e.getMessage(), e);
//    if (e instanceof ErrorResponseException) {
//        if (((ErrorResponseException) e).errorResponse().code().equals("NoSuchKey")) {
//            throw new NoSuchEntityException(e.getMessage());
//        }
//    }
//    return new ServerErrorException("MinioClient resulted with error:", e);
//}
