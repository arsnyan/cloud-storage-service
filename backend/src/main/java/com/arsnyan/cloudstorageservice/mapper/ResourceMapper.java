package com.arsnyan.cloudstorageservice.mapper;

import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.exception.MinioWrappedException;
import com.arsnyan.cloudstorageservice.exception.ServerErrorException;
import com.arsnyan.cloudstorageservice.model.ResourceObject;
import com.google.common.collect.Streams;
import io.minio.Result;
import io.minio.SnowballObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import static com.arsnyan.cloudstorageservice.util.FileUtils.*;

@Slf4j
public class ResourceMapper {
    public static List<ResourceObject> mapResultsToResourceObjects(Iterable<Result<Item>> minioListResults) {
        return Streams.stream(minioListResults)
            .filter(Objects::nonNull)
            .map(ResourceMapper::mapRawObjectToItem)
            .map(item -> {
                var objectName = item.objectName();
                var resourceType = getResourceType(objectName);

                return new ResourceObject(
                    objectName,
                    resourceType
                );
            })
            .toList();
    }

    public static List<ResourceGetInfoResponseDto> listObjectsAsDto(
        Iterable<Result<Item>> objects,
        String excludePath
    ) {
        return Streams.stream(objects)
            .filter(Objects::nonNull)
            .map(ResourceMapper::mapRawObjectToItem)
            .filter(item -> !item.objectName().equals(excludePath))
            .map(ResourceMapper::mapItemToDto)
            .toList();
    }

    public static List<SnowballObject> mapToSnowballObjects(Long userId, String path, List<MultipartFile> files) {
        return files.stream()
            .filter(file -> file.getOriginalFilename() != null)
            .map(file -> {
                var pathToFile = path + file.getOriginalFilename();
                var resolvedPath = resolvePath(userId, pathToFile);

                try {
                    return new SnowballObject(
                        resolvedPath,
                        file.getInputStream(),
                        file.getSize(),
                        ZonedDateTime.now()
                    );
                } catch (IOException e) {
                    log.error("Failed to get input stream", e);
                    throw new ServerErrorException("Failed to get input stream", e);
                }
            })
            .toList();
    }

    public static List<ResourceGetInfoResponseDto> mapFilesToDto(String path, List<MultipartFile> files) {
        return files.stream()
            .map(file -> {
                var pathToFile = path + file.getOriginalFilename();

                return new ResourceGetInfoResponseDto(
                    getParentPath(pathToFile),
                    extractResourceName(pathToFile),
                    file.getSize(),
                    getResourceType(pathToFile)
                );
            })
            .toList();
    }

    public static ResourceGetInfoResponseDto mapItemToDto(Item item) {
        var leadingPath = getParentPath(item.objectName()).replaceAll("user-[^/]+-files/", "");
        var name = extractResourceName(item.objectName());
        var size = item.size();
        var resourceType = getResourceType(item.objectName());

        return new ResourceGetInfoResponseDto(
            leadingPath,
            name,
            size,
            resourceType
        );
    }

    public static Item mapRawObjectToItem(Result<Item> rawObject) {
        try {
            return rawObject.get();
        } catch (Exception e) {
            log.error("Failed to map object {} to Item: {}", rawObject, e.getMessage());
            throw MinioWrappedException.from(e);
        }
    }
}
