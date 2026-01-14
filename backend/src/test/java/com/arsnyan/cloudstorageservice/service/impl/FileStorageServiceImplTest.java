package com.arsnyan.cloudstorageservice.service.impl;

import com.arsnyan.cloudstorageservice.dto.resource.ResourceGetInfoResponseDto;
import com.arsnyan.cloudstorageservice.exception.EntityAlreadyExistsException;
import com.arsnyan.cloudstorageservice.exception.MinioWrappedException;
import com.arsnyan.cloudstorageservice.exception.NoSuchEntityException;
import com.arsnyan.cloudstorageservice.model.ResourceType;
import com.arsnyan.cloudstorageservice.repository.UserRepository;
import com.arsnyan.cloudstorageservice.util.MinioS3Client;
import io.minio.Result;
import io.minio.SnowballObject;
import io.minio.messages.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MinioS3Client s3Client;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @Captor
    private ArgumentCaptor<List<SnowballObject>> snowballObjectsCaptor;

    private static final String USERNAME = "testuser";
    private static final Long USER_ID = 1L;
    private static final String USER_PREFIX = "user-1-files/";

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.getUserIdByUsername(USERNAME))
            .thenReturn(Optional.of(USER_ID));
    }

    @Nested
    @DisplayName("uploadResources")
    class UploadResourcesTests {

        @Test
        @DisplayName("should upload single file successfully")
        void uploadSingleFile_success() {
            var file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes()
            );
            var path = "";

            when(s3Client.isPathAvailable(USER_PREFIX + "test.txt")).thenReturn(false);

            var result = fileStorageService.uploadResources(USERNAME, path, List.of(file));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("test.txt");
            assertThat(result.getFirst().path()).isEqualTo("/");
            assertThat(result.getFirst().type()).isEqualTo(ResourceType.FILE);
            assertThat(result.getFirst().size()).isEqualTo(7L);

            verify(s3Client).uploadSnowballObject(snowballObjectsCaptor.capture());
            assertThat(snowballObjectsCaptor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("should upload multiple files successfully")
        void uploadMultipleFiles_success() {
            var file1 = new MockMultipartFile("file", "file1.txt", "text/plain", "content1".getBytes());
            var file2 = new MockMultipartFile("file", "file2.txt", "text/plain", "content2".getBytes());
            var path = "documents/";

            when(s3Client.isPathAvailable(anyString())).thenReturn(false);

            var result = fileStorageService.uploadResources(USERNAME, path, List.of(file1, file2));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ResourceGetInfoResponseDto::name)
                .containsExactly("file1.txt", "file2.txt");
            assertThat(result).allMatch(dto -> dto.path().equals("documents/"));

            verify(s3Client).uploadSnowballObject(snowballObjectsCaptor.capture());
            assertThat(snowballObjectsCaptor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("should throw EntityAlreadyExistsException when file already exists")
        void uploadFile_fileAlreadyExists_throwsException() {
            var file = new MockMultipartFile("file", "existing.txt", "text/plain", "content".getBytes());
            var path = "";

            when(s3Client.isPathAvailable(USER_PREFIX + "existing.txt")).thenReturn(true);

            assertThatThrownBy(() -> fileStorageService.uploadResources(USERNAME, path, List.of(file)))
                .isInstanceOf(MinioWrappedException.class)
                .hasCauseInstanceOf(EntityAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should create intermediate folders for nested file upload")
        void uploadNestedFile_createsIntermediateFolders() {
            var file = new MockMultipartFile(
                "file",
                "folder1/folder2/test.txt",
                "text/plain",
                "content".getBytes()
            );
            var path = "";

            when(s3Client.isPathAvailable(anyString())).thenReturn(false);
            when(s3Client.hasNamingConflict(anyString())).thenReturn(false);

            var result = fileStorageService.uploadResources(USERNAME, path, List.of(file));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("test.txt");
            assertThat(result.getFirst().path()).isEqualTo("folder1/folder2/");

            verify(s3Client).ensureFolderPlaceholderExists(USER_PREFIX + "folder1/");
            verify(s3Client).ensureFolderPlaceholderExists(USER_PREFIX + "folder1/folder2/");
        }

        @Test
        @DisplayName("should throw exception when folder name conflicts with existing file")
        void uploadNestedFile_folderNameConflictsWithFile_throwsException() {
            var file = new MockMultipartFile(
                "file",
                "existingFile/test.txt",
                "text/plain",
                "content".getBytes()
            );
            var path = "";

            when(s3Client.isPathAvailable(anyString())).thenReturn(false);
            when(s3Client.hasNamingConflict(USER_PREFIX + "existingFile/")).thenReturn(true);

            assertThatThrownBy(() -> fileStorageService.uploadResources(USERNAME, path, List.of(file)))
                .isInstanceOf(MinioWrappedException.class)
                .hasCauseInstanceOf(EntityAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should throw NoSuchEntityException when user not found")
        void uploadFile_userNotFound_throwsException() {
            var file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
            var unknownUser = "unknown";

            when(userRepository.getUserIdByUsername(unknownUser)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileStorageService.uploadResources(unknownUser, "", List.of(file)))
                .isInstanceOf(MinioWrappedException.class)
                .hasCauseInstanceOf(NoSuchEntityException.class);
        }

        @Test
        @DisplayName("should skip files with null original filename in snowball upload")
        void uploadFiles_nullFilename_skippedInSnowball() {
            var fileWithNullName = mock(MultipartFile.class);
            when(fileWithNullName.getOriginalFilename()).thenReturn(null);

            var validFile = new MockMultipartFile("file", "valid.txt", "text/plain", "content".getBytes());
            when(s3Client.isPathAvailable(anyString())).thenReturn(false);

            fileStorageService.uploadResources(USERNAME, "", List.of(fileWithNullName, validFile));

            verify(s3Client).uploadSnowballObject(snowballObjectsCaptor.capture());
            assertThat(snowballObjectsCaptor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("should upload file to specific path")
        void uploadFile_toSpecificPath_success() {
            var file = new MockMultipartFile("file", "report.pdf", "application/pdf", "pdf content".getBytes());
            var path = "documents/reports/";

            when(s3Client.isPathAvailable(USER_PREFIX + "documents/reports/report.pdf")).thenReturn(false);

            var result = fileStorageService.uploadResources(USERNAME, path, List.of(file));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("report.pdf");
            assertThat(result.getFirst().path()).isEqualTo("documents/reports/");

            verify(s3Client).uploadSnowballObject(snowballObjectsCaptor.capture());
            var uploadedPath = snowballObjectsCaptor.getValue().getFirst().name();
            assertThat(uploadedPath).isEqualTo(USER_PREFIX + "documents/reports/report.pdf");
        }

        @Test
        @DisplayName("should create deeply nested folder structure")
        void uploadDeeplyNestedFile_createsAllFolders() {
            var file = new MockMultipartFile(
                "file",
                "a/b/c/d/file.txt",
                "text/plain",
                "content".getBytes()
            );

            when(s3Client.isPathAvailable(anyString())).thenReturn(false);
            when(s3Client.hasNamingConflict(anyString())).thenReturn(false);

            fileStorageService.uploadResources(USERNAME, "", List.of(file));

            verify(s3Client).ensureFolderPlaceholderExists(USER_PREFIX + "a/");
            verify(s3Client).ensureFolderPlaceholderExists(USER_PREFIX + "a/b/");
            verify(s3Client).ensureFolderPlaceholderExists(USER_PREFIX + "a/b/c/");
            verify(s3Client).ensureFolderPlaceholderExists(USER_PREFIX + "a/b/c/d/");
        }
    }

    @Nested
    @DisplayName("createFolder")
    class CreateFolderTests {

        @Test
        @DisplayName("should create folder successfully")
        void createFolder_success() {
            var path = "newFolder/";

            when(s3Client.isPathAvailable(USER_PREFIX + path)).thenReturn(false);

            var result = fileStorageService.createFolder(USERNAME, path);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("newFolder/");
            assertThat(result.path()).isEqualTo("/");
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);

            verify(s3Client).makeFolderInS3(USER_PREFIX + path);
        }

        @Test
        @DisplayName("should create nested folder successfully")
        void createNestedFolder_success() {
            var path = "parent/child/";

            when(s3Client.isPathAvailable(USER_PREFIX + path)).thenReturn(false);

            var result = fileStorageService.createFolder(USERNAME, path);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("child/");
            assertThat(result.path()).isEqualTo("parent/");
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);

            verify(s3Client).makeFolderInS3(USER_PREFIX + path);
        }

        @Test
        @DisplayName("should throw EntityAlreadyExistsException when folder already exists")
        void createFolder_alreadyExists_throwsException() {
            var path = "existingFolder/";

            when(s3Client.isPathAvailable(USER_PREFIX + path)).thenReturn(true);

            assertThatThrownBy(() -> fileStorageService.createFolder(USERNAME, path))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Object already exists");

            verify(s3Client, never()).makeFolderInS3(anyString());
        }

        @Test
        @DisplayName("should throw NoSuchEntityException when user not found")
        void createFolder_userNotFound_throwsException() {
            var unknownUser = "unknown";

            when(userRepository.getUserIdByUsername(unknownUser)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileStorageService.createFolder(unknownUser, "folder/"))
                .isInstanceOf(NoSuchEntityException.class)
                .hasMessage("User not found");

            verify(s3Client, never()).makeFolderInS3(anyString());
        }

        @Test
        @DisplayName("should create deeply nested folder")
        void createDeeplyNestedFolder_success() {
            var path = "a/b/c/d/newFolder/";

            when(s3Client.isPathAvailable(USER_PREFIX + path)).thenReturn(false);

            var result = fileStorageService.createFolder(USERNAME, path);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("newFolder/");
            assertThat(result.path()).isEqualTo("a/b/c/d/");
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }
    }

    @Nested
    @DisplayName("moveResource")
    class MoveResourceTests {

        @Test
        @DisplayName("should move single file successfully")
        void moveSingleFile_success() {
            var from = "source.txt";
            var to = "destination.txt";
            var resolvedFrom = USER_PREFIX + from;
            var resolvedTo = USER_PREFIX + to;

            var sourceItem = createMockItem(resolvedFrom, false);
            var resultItems = createResultList(sourceItem);

            when(s3Client.listObjects(resolvedFrom, true)).thenReturn(resultItems);
            when(s3Client.isPathAvailable(resolvedTo)).thenReturn(false);
            when(s3Client.getStatObject(resolvedTo)).thenReturn(null);

            var result = fileStorageService.moveResource(USERNAME, from, to);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("destination.txt");
            assertThat(result.type()).isEqualTo(ResourceType.FILE);

            verify(s3Client).copyObject(sourceItem, resolvedTo);
            verify(s3Client).removeObject(resolvedFrom);
        }

        @Test
        @DisplayName("should move folder with contents successfully")
        void moveFolderWithContents_success() {
            var from = "sourceFolder/";
            var to = "destFolder/";
            var resolvedFrom = USER_PREFIX + from;
            var resolvedTo = USER_PREFIX + to;

            var folderItem = createMockItem(resolvedFrom, true);
            var fileItem = createMockItem(resolvedFrom + "file.txt", false);
            var resultItems = createResultList(folderItem, fileItem);

            when(s3Client.listObjects(resolvedFrom, true)).thenReturn(resultItems);
            when(s3Client.hasNamingConflict(anyString())).thenReturn(false);
            when(s3Client.isPathAvailable(anyString())).thenReturn(false);
            when(s3Client.getStatObject(resolvedTo)).thenReturn(null);

            var result = fileStorageService.moveResource(USERNAME, from, to);

            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);

            verify(s3Client).copyObject(eq(folderItem), eq(resolvedTo));
            verify(s3Client).copyObject(eq(fileItem), eq(resolvedTo + "file.txt"));
            verify(s3Client).removeObject(resolvedFrom);
            verify(s3Client).removeObject(resolvedFrom + "file.txt");
        }

        @Test
        @DisplayName("should throw NoSuchEntityException when source does not exist")
        void moveResource_sourceNotFound_throwsException() {
            var from = "nonexistent.txt";
            var resolvedFrom = USER_PREFIX + from;

            when(s3Client.listObjects(resolvedFrom, true)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> fileStorageService.moveResource(USERNAME, from, "destination.txt"))
                .isInstanceOf(NoSuchEntityException.class)
                .hasMessageContaining("Source resource not found");
        }

        @Test
        @DisplayName("should throw EntityAlreadyExistsException when destination file already exists")
        void moveFile_destinationExists_throwsException() {
            var from = "source.txt";
            var to = "existing.txt";
            var resolvedFrom = USER_PREFIX + from;
            var resolvedTo = USER_PREFIX + to;

            var sourceItem = createMockItem(resolvedFrom, false);
            var resultItems = createResultList(sourceItem);

            when(s3Client.listObjects(resolvedFrom, true)).thenReturn(resultItems);
            when(s3Client.isPathAvailable(resolvedTo)).thenReturn(true);

            assertThatThrownBy(() -> fileStorageService.moveResource(USERNAME, from, to))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Destination already exists");

            verify(s3Client, never()).copyObject(any(), anyString());
        }

        @Test
        @DisplayName("should throw EntityAlreadyExistsException when folder conflicts with existing file")
        void moveFolder_conflictsWithFile_throwsException() {
            var from = "sourceFolder/";
            var to = "existingFile/";
            var resolvedFrom = USER_PREFIX + from;
            var resolvedTo = USER_PREFIX + to;

            var folderItem = createMockItem(resolvedFrom, true);
            var resultItems = createResultList(folderItem);

            when(s3Client.listObjects(resolvedFrom, true)).thenReturn(resultItems);
            when(s3Client.hasNamingConflict(resolvedTo)).thenReturn(true);

            assertThatThrownBy(() -> fileStorageService.moveResource(USERNAME, from, to))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("File already exists");
        }

        @Test
        @DisplayName("should create parent folders when moving to nested destination")
        void moveFile_toNestedPath_createsParentFolders() {
            var from = "a.txt";
            var to = "very/deeply/nested/destination.txt";
            var resolvedFrom = USER_PREFIX + from;
            var resolvedTo = USER_PREFIX + to;

            var sourceItem = createMockItem(resolvedFrom, false);
            var resultItems = createResultList(sourceItem);

            when(s3Client.listObjects(resolvedFrom, true)).thenReturn(resultItems);
            when(s3Client.isPathAvailable(resolvedTo)).thenReturn(false);
            when(s3Client.hasNamingConflict(anyString())).thenReturn(false);
            when(s3Client.getStatObject(resolvedTo)).thenReturn(null);

            fileStorageService.moveResource(USERNAME, from, to);

            // Parent folders are created up to the length of userRootPath
            verify(s3Client).ensureFolderPlaceholderExists(USER_PREFIX + "very/deeply/nested/");
            verify(s3Client).ensureFolderPlaceholderExists(USER_PREFIX + "very/deeply/");
            verify(s3Client).copyObject(sourceItem, resolvedTo);
        }

        @Test
        @DisplayName("should throw NoSuchEntityException when user not found")
        void moveResource_userNotFound_throwsException() {
            var unknownUser = "unknown";

            when(userRepository.getUserIdByUsername(unknownUser)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileStorageService.moveResource(unknownUser, "source.txt", "dest.txt"))
                .isInstanceOf(NoSuchEntityException.class)
                .hasMessage("User not found");
        }

        @Test
        @DisplayName("should throw exception when parent folder creation conflicts with file")
        void moveFile_parentFolderConflictsWithFile_throwsException() {
            var from = "source.txt";
            var to = "existingFile/destination.txt";
            var resolvedFrom = USER_PREFIX + from;
            var resolvedTo = USER_PREFIX + to;

            var sourceItem = createMockItem(resolvedFrom, false);
            var resultItems = createResultList(sourceItem);

            when(s3Client.listObjects(resolvedFrom, true)).thenReturn(resultItems);
            when(s3Client.isPathAvailable(resolvedTo)).thenReturn(false);
            when(s3Client.hasNamingConflict(USER_PREFIX + "existingFile/")).thenReturn(true);

            assertThatThrownBy(() -> fileStorageService.moveResource(USERNAME, from, to))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessageContaining("cannot create parent folder");
        }

        @Test
        @DisplayName("should rename file in same directory")
        void renameFile_success() {
            var from = "folder/oldname.txt";
            var to = "folder/newname.txt";
            var resolvedFrom = USER_PREFIX + from;
            var resolvedTo = USER_PREFIX + to;

            var sourceItem = createMockItem(resolvedFrom, false);
            var resultItems = createResultList(sourceItem);

            when(s3Client.listObjects(resolvedFrom, true)).thenReturn(resultItems);
            when(s3Client.isPathAvailable(resolvedTo)).thenReturn(false);
            lenient().when(s3Client.hasNamingConflict(anyString())).thenReturn(false);
            when(s3Client.getStatObject(resolvedTo)).thenReturn(null);

            var result = fileStorageService.moveResource(USERNAME, from, to);

            assertThat(result.name()).isEqualTo("newname.txt");
            assertThat(result.path()).isEqualTo("folder/");

            verify(s3Client).copyObject(sourceItem, resolvedTo);
            verify(s3Client).removeObject(resolvedFrom);
        }

        @Test
        @DisplayName("should move nested folder structure preserving hierarchy")
        void moveNestedFolderStructure_success() {
            var from = "source/";
            var to = "dest/";
            var resolvedFrom = USER_PREFIX + from;
            var resolvedTo = USER_PREFIX + to;

            var folder1 = createMockItem(resolvedFrom, true);
            var subFolder = createMockItem(resolvedFrom + "sub/", true);
            var file1 = createMockItem(resolvedFrom + "file1.txt", false);
            var file2 = createMockItem(resolvedFrom + "sub/file2.txt", false);
            var resultItems = createResultList(folder1, subFolder, file1, file2);

            when(s3Client.listObjects(resolvedFrom, true)).thenReturn(resultItems);
            when(s3Client.hasNamingConflict(anyString())).thenReturn(false);
            when(s3Client.isPathAvailable(anyString())).thenReturn(false);
            when(s3Client.getStatObject(resolvedTo)).thenReturn(null);

            fileStorageService.moveResource(USERNAME, from, to);

            verify(s3Client).copyObject(folder1, resolvedTo);
            verify(s3Client).copyObject(subFolder, resolvedTo + "sub/");
            verify(s3Client).copyObject(file1, resolvedTo + "file1.txt");
            verify(s3Client).copyObject(file2, resolvedTo + "sub/file2.txt");
        }

        private Item createMockItem(String objectName, boolean isDir) {
            var item = mock(Item.class);
            lenient().when(item.objectName()).thenReturn(objectName);
            lenient().when(item.isDir()).thenReturn(isDir);
            return item;
        }

        private List<Result<Item>> createResultList(Item... items) {
            var results = new ArrayList<Result<Item>>();
            for (Item item : items) {
                @SuppressWarnings("unchecked")
                Result<Item> result = mock(Result.class);
                try {
                    when(result.get()).thenReturn(item);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                results.add(result);
            }
            return results;
        }
    }
}
