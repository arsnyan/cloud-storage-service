package com.arsnyan.cloudstorageservice.service;

import com.arsnyan.cloudstorageservice.dto.AddFolderResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface FileStorageService {
    ResourceGetInfoResponseDto getDetailsForResource(String username, String path);
    void deleteResource(String username, String path);
    File produceFileForPath(String username, String path);
    ResourceGetInfoResponseDto moveResource(String username, String from, String to);
    List<ResourceGetInfoResponseDto> searchResources(String username, String query);
    List<ResourceGetInfoResponseDto> uploadResources(String username, String path, List<MultipartFile> files);
    List<ResourceGetInfoResponseDto> listAllObjectsForFolder(String username, String path);
    AddFolderResponseDto makeFolder(String username, String path);
}
