package com.arsnyan.cloudstorageservice.util;

import com.arsnyan.cloudstorageservice.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileUtilsTest {
    @Test
    void getResourceType_shouldReturnDirectory_whenPathEndsWithSlash() {
        assertEquals(ResourceType.DIRECTORY, FileUtils.getResourceType("folder/"));
        assertEquals(ResourceType.DIRECTORY, FileUtils.getResourceType("path/to/folder/"));
        assertEquals(ResourceType.DIRECTORY, FileUtils.getResourceType("/"));
    }

    @Test
    void getResourceType_shouldReturnFile_whenPathDoesNotEndWithSlash() {
        assertEquals(ResourceType.FILE, FileUtils.getResourceType("file.txt"));
        assertEquals(ResourceType.FILE, FileUtils.getResourceType("path/to/file.txt"));
        assertEquals(ResourceType.FILE, FileUtils.getResourceType("noextension"));
    }

    @Test
    void getResourceType_shouldReturnFile_whenPathIsEmpty() {
        assertEquals(ResourceType.FILE, FileUtils.getResourceType(""));
    }

    @Test
    void getRelativeZipPath_shouldStripUserPrefix_whenPathStartsWithUserPrefix() {
        Long userId = 42L;
        String absolutePath = "user-42-files/documents/file.txt";

        String result = FileUtils.getRelativeZipPath(userId, absolutePath);

        assertEquals("documents/file.txt", result);
    }

    @Test
    void getRelativeZipPath_shouldReturnOriginalPath_whenPathDoesNotStartWithUserPrefix() {
        Long userId = 42L;
        String absolutePath = "other-prefix/documents/file.txt";

        String result = FileUtils.getRelativeZipPath(userId, absolutePath);

        assertEquals("other-prefix/documents/file.txt", result);
    }

    @Test
    void getRelativeZipPath_shouldReturnEmptyString_whenPathIsExactlyUserPrefix() {
        Long userId = 42L;
        String absolutePath = "user-42-files/";

        String result = FileUtils.getRelativeZipPath(userId, absolutePath);

        assertEquals("", result);
    }

    @Test
    void getRelativeZipPath_shouldNotStripPrefix_whenUserIdDoesNotMatch() {
        Long userId = 42L;
        String absolutePath = "user-99-files/documents/file.txt";

        String result = FileUtils.getRelativeZipPath(userId, absolutePath);

        assertEquals("user-99-files/documents/file.txt", result);
    }

    @Test
    void resolvePath_shouldPrependUserPrefix() {
        Long userId = 42L;
        String path = "documents/file.txt";

        String result = FileUtils.resolvePath(userId, path);

        assertEquals("user-42-files/documents/file.txt", result);
    }

    @Test
    void resolvePath_shouldHandleEmptyPath() {
        Long userId = 42L;

        String result = FileUtils.resolvePath(userId, "");

        assertEquals("user-42-files/", result);
    }

    @Test
    void resolvePath_shouldHandleRootPath() {
        Long userId = 1L;

        String result = FileUtils.resolvePath(userId, "/");

        assertEquals("user-1-files//", result);
    }

    @Test
    void getParentPath_shouldReturnParentDirectory_forFilePath() {
        assertEquals("path/to/", FileUtils.getParentPath("path/to/file.txt"));
        assertEquals("/", FileUtils.getParentPath("file.txt"));
    }

    @Test
    void getParentPath_shouldReturnParentDirectory_forDirectoryPath() {
        assertEquals("path/to/", FileUtils.getParentPath("path/to/folder/"));
        assertEquals("/", FileUtils.getParentPath("folder/"));
    }

    @Test
    void getParentPath_shouldReturnRoot_forNullOrEmptyPath() {
        assertEquals("/", FileUtils.getParentPath(null));
        assertEquals("/", FileUtils.getParentPath(""));
    }

    @Test
    void getParentPath_shouldReturnRoot_forRootPath() {
        assertEquals("/", FileUtils.getParentPath("/"));
    }

    @Test
    void getParentPath_shouldHandleNestedPaths() {
        assertEquals("a/b/c/", FileUtils.getParentPath("a/b/c/d/"));
        assertEquals("a/b/", FileUtils.getParentPath("a/b/c.txt"));
    }

    @Test
    void extractResourceName_shouldReturnFileName_forFilePath() {
        assertEquals("file.txt", FileUtils.extractResourceName("path/to/file.txt"));
        assertEquals("document.pdf", FileUtils.extractResourceName("document.pdf"));
    }

    @Test
    void extractResourceName_shouldReturnDirectoryNameWithSlash_forDirectoryPath() {
        assertEquals("folder/", FileUtils.extractResourceName("path/to/folder/"));
        assertEquals("documents/", FileUtils.extractResourceName("documents/"));
    }

    @Test
    void extractResourceName_shouldReturnPath_whenNoDelimiter() {
        assertEquals("filename", FileUtils.extractResourceName("filename"));
        assertEquals("file.txt", FileUtils.extractResourceName("file.txt"));
    }

    @Test
    void extractResourceName_shouldHandleDeeplyNestedPaths() {
        assertEquals("deep.txt", FileUtils.extractResourceName("a/b/c/d/e/deep.txt"));
        assertEquals("nested/", FileUtils.extractResourceName("a/b/c/d/e/nested/"));
    }

    @Test
    void extractResourceName_shouldHandleRootDirectory() {
        assertEquals("/", FileUtils.extractResourceName("/"));
    }

    @ParameterizedTest
    @CsvSource({
        "folder/, DIRECTORY",
        "path/to/dir/, DIRECTORY",
        "/, DIRECTORY",
        "file.txt, FILE",
        "path/to/file, FILE",
        "'', FILE"
    })
    void getResourceType_parameterized(String path, ResourceType expected) {
        assertEquals(expected, FileUtils.getResourceType(path));
    }

    @ParameterizedTest
    @CsvSource({
        "1, user-1-files/test.txt, test.txt",
        "123, user-123-files/a/b/c.txt, a/b/c.txt",
        "1, other/path.txt, other/path.txt",
        "42, user-42-files/, ''"
    })
    void getRelativeZipPath_parameterized(Long userId, String absolutePath, String expected) {
        assertEquals(expected, FileUtils.getRelativeZipPath(userId, absolutePath));
    }

    @ParameterizedTest
    @CsvSource({
        "path/to/file.txt, path/to/",
        "file.txt, /",
        "a/b/c/, a/b/",
        "folder/, /",
        "'', /"
    })
    void getParentPath_parameterized(String path, String expected) {
        assertEquals(expected, FileUtils.getParentPath(path));
    }

    @ParameterizedTest
    @CsvSource({
        "path/to/file.txt, file.txt",
        "documents/, documents/",
        "a/b/c/folder/, folder/",
        "standalone, standalone"
    })
    void extractResourceName_parameterized(String path, String expected) {
        assertEquals(expected, FileUtils.extractResourceName(path));
    }
}