package com.arsnyan.cloudstorageservice.service;

import com.arsnyan.cloudstorageservice.dto.resource.AddFolderResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.FileDownloadResponseDto;
import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    ResourceGetInfoResponseDto getResourceInfo(String username, String path);
    void deleteResource(String username, String path);
    FileDownloadResponseDto getDownloadableResource(String username, String path);
    ResourceGetInfoResponseDto moveResource(String username, String from, String to);
    List<ResourceGetInfoResponseDto> searchResources(String username, String query);
    List<ResourceGetInfoResponseDto> uploadResources(String username, String path, List<MultipartFile> files);
    List<ResourceGetInfoResponseDto> listFolderContents(String username, String path);
    AddFolderResponseDto createFolder(String username, String path);
}
