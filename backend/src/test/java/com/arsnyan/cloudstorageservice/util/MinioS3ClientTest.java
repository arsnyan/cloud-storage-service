package com.arsnyan.cloudstorageservice.util;

import com.arsnyan.cloudstorageservice.TestcontainersConfiguration;
import com.arsnyan.cloudstorageservice.exception.MinioWrappedException;
import io.minio.SnowballObject;
import io.minio.messages.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MinioS3ClientTest {
    @Autowired
    private MinioS3Client s3Client;

    private String testPrefix;

    @BeforeEach
    void setUp() {
        // So that there are no conflicts between test cases
        testPrefix = "test-" + UUID.randomUUID() + "/";
    }

    // Helper method for a simple test file in S3
    private void createTestFile(String path, String content) {
        var contentBytes = content.getBytes(StandardCharsets.UTF_8);
        var snowballObject = new SnowballObject(
            path,
            new ByteArrayInputStream(contentBytes),
            contentBytes.length,
            null
        );
        s3Client.uploadSnowballObject(List.of(snowballObject));
    }

    // For first Item from listObjects
    private Item getFirstItem(String path) throws Exception {
        var results = s3Client.listObjects(path, false);
        for (var result : results) {
            return result.get();
        }
        return null;
    }

    @Nested
    class UploadSnowballObjectTests {
        @Test
        void uploadSnowballObject_uploadsSuccessfully() {
            var objectPath = testPrefix + "testfile.txt";
            var content = "Hello, World!";
            var contentBytes = content.getBytes(StandardCharsets.UTF_8);

            var snowballObject = new SnowballObject(
                objectPath,
                new ByteArrayInputStream(contentBytes),
                contentBytes.length,
                null
            );

            assertDoesNotThrow(() -> s3Client.uploadSnowballObject(List.of(snowballObject)));

            var stat = s3Client.getStatObject(objectPath);
            assertNotNull(stat);
            assertThat(stat.size()).isEqualTo(contentBytes.length);
        }

        @Test
        void uploadSnowballObject_uploadsMultipleObjects() {
            var path1 = testPrefix + "file1.txt";
            var path2 = testPrefix + "file2.txt";
            var content1 = "Content 1";
            var content2 = "Content 2";

            var objects = List.of(
                new SnowballObject(path1, new ByteArrayInputStream(content1.getBytes()), content1.length(), null),
                new SnowballObject(path2, new ByteArrayInputStream(content2.getBytes()), content2.length(), null)
            );

            assertDoesNotThrow(() -> s3Client.uploadSnowballObject(objects));

            assertNotNull(s3Client.getStatObject(path1));
            assertNotNull(s3Client.getStatObject(path2));
        }

        @Test
        void uploadSnowballObject_throwsException_whenObjectsListIsNull() {
            assertThrows(MinioWrappedException.class, () -> s3Client.uploadSnowballObject(null));
        }
    }

    @Nested
    class CopyObjectTests {
        @Test
        void copyObject_copiesSuccessfully() throws Exception {
            var sourcePath = testPrefix + "source.txt";
            var destPath = testPrefix + "destination.txt";
            var content = "Copy me!";

            createTestFile(sourcePath, content);
            var sourceItem = getFirstItem(sourcePath);
            assertNotNull(sourceItem);

            assertDoesNotThrow(() -> s3Client.copyObject(sourceItem, destPath));

            var destStat = s3Client.getStatObject(destPath);
            assertNotNull(destStat);
            assertThat(destStat.size()).isEqualTo(content.length());
        }

        @Test
        void copyObject_preservesOriginalFile() throws Exception {
            var sourcePath = testPrefix + "original.txt";
            var destPath = testPrefix + "copy.txt";
            var content = "Original content";

            createTestFile(sourcePath, content);
            var sourceItem = getFirstItem(sourcePath);
            assertNotNull(sourceItem);

            s3Client.copyObject(sourceItem, destPath);

            // Verify original still exists
            var sourceStat = s3Client.getStatObject(sourcePath);
            assertNotNull(sourceStat);
            assertThat(sourceStat.size()).isEqualTo(content.length());
        }

        @Test
        void copyObject_throwsException_whenSourceDoesNotExist() {
            var fakeItem = new Item() {
                @Override
                public String objectName() {
                    return testPrefix + "nonexistent.txt";
                }
            };

            assertThrows(MinioWrappedException.class,
                () -> s3Client.copyObject(fakeItem, testPrefix + "dest.txt"));
        }
    }

    @Nested
    class GetObjectTests {
        @Test
        void getObject_returnsObjectContent() throws IOException {
            var objectPath = testPrefix + "readable.txt";
            var content = "Read this content";

            createTestFile(objectPath, content);

            var response = s3Client.getObject(objectPath);

            assertNotNull(response);
            var retrievedContent = new String(response.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(retrievedContent).isEqualTo(content);

            response.close();
        }

        @Test
        void getObject_throwsException_whenObjectDoesNotExist() {
            var nonExistentPath = testPrefix + "nonexistent.txt";

            assertThrows(MinioWrappedException.class, () -> s3Client.getObject(nonExistentPath));
        }

        @Test
        void getObject_throwsException_whenPathIsNull() {
            assertThrows(MinioWrappedException.class, () -> s3Client.getObject(null));
        }
    }

    @Nested
    class RemoveObjectTests {
        @Test
        void removeObject_removesSuccessfully() {
            var objectPath = testPrefix + "toBeDeleted.txt";
            var content = "Delete me";

            createTestFile(objectPath, content);

            // Verify it exists first
            assertNotNull(s3Client.getStatObject(objectPath));

            assertDoesNotThrow(() -> s3Client.removeObject(objectPath));

            // Verify it's gone
            assertNull(s3Client.getStatObject(objectPath));
        }

        @Test
        void removeObject_doesNotThrow_whenObjectDoesNotExist() {
            var nonExistentPath = testPrefix + "doesNotExist.txt";

            // MinIO's removeObject is idempotent - it doesn't throw if object doesn't exist
            assertDoesNotThrow(() -> s3Client.removeObject(nonExistentPath));
        }

        @Test
        void removeObject_throwsException_whenPathIsNull() {
            assertThrows(MinioWrappedException.class, () -> s3Client.removeObject(null));
        }
    }

    @Nested
    class ListObjectsTests {
        @Test
        void listObjects_returnsObjects() throws Exception {
            var folder = testPrefix + "listTest/";
            createTestFile(folder + "file1.txt", "Content 1");
            createTestFile(folder + "file2.txt", "Content 2");

            var results = s3Client.listObjects(folder, false);

            var objectNames = new ArrayList<>();
            for (var result : results) {
                objectNames.add(result.get().objectName());
            }

            assertThat(objectNames).hasSize(2);
            assertThat(objectNames).contains(folder + "file1.txt", folder + "file2.txt");
        }

        @Test
        void listObjects_returnsNestedObjects_whenRecursiveIsTrue() throws Exception {
            var folder = testPrefix + "recursive/";
            createTestFile(folder + "file1.txt", "Content 1");
            createTestFile(folder + "nested/file2.txt", "Content 2");

            var results = s3Client.listObjects(folder, true);

            var objectNames = new ArrayList<>();
            for (var result : results) {
                objectNames.add(result.get().objectName());
            }

            assertThat(objectNames).hasSize(2);
            assertThat(objectNames).contains(folder + "file1.txt", folder + "nested/file2.txt");
        }

        @Test
        void listObjects_returnsOnlyTopLevel_whenRecursiveIsFalse() throws Exception {
            var folder = testPrefix + "nonrecursive/";
            createTestFile(folder + "file1.txt", "Content 1");
            createTestFile(folder + "nested/file2.txt", "Content 2");

            var results = s3Client.listObjects(folder, false);

            var objectNames = new ArrayList<>();
            for (var result : results) {
                var item = result.get();
                // Non-recursive mode shows directories as prefixes
                objectNames.add(item.objectName());
            }

            assertThat(objectNames).contains(folder + "file1.txt");
        }

        @Test
        void listObjects_returnsEmpty_whenNoObjectsExist() {
            var emptyFolder = testPrefix + "emptyFolder/";

            var results = s3Client.listObjects(emptyFolder, false);

            var items = new ArrayList<>();
            for (var result : results) {
                try {
                    items.add(result.get());
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            assertThat(items).isEmpty();
        }
    }

    @Nested
    class GetStatObjectTests {
        @Test
        void getStatObject_returnsStats() {
            var objectPath = testPrefix + "stats.txt";
            var content = "Get my stats";

            createTestFile(objectPath, content);

            var stat = s3Client.getStatObject(objectPath);

            assertNotNull(stat);
            assertThat(stat.object()).isEqualTo(objectPath);
            assertThat(stat.size()).isEqualTo(content.length());
        }

        @Test
        void getStatObject_returnsNull_whenObjectDoesNotExist() {
            var nonExistentPath = testPrefix + "nonexistent.txt";

            var stat = s3Client.getStatObject(nonExistentPath);

            assertNull(stat);
        }

        @Test
        void getStatObject_throwsException_whenPathIsNull() {
            assertThrows(MinioWrappedException.class, () -> s3Client.getStatObject(null));
        }
    }

    @Nested
    class IsPathUnavailableTests {
        @Test
        void isPathUnavailable_returnsFalse_whenPathExists() {
            var objectPath = testPrefix + "exists.txt";
            createTestFile(objectPath, "I exist");

            var result = s3Client.isPathUnavailable(objectPath);

            assertFalse(result);
        }

        @Test
        void isPathUnavailable_returnsTrue_whenPathDoesNotExist() {
            var nonExistentPath = testPrefix + "doesNotExist.txt";

            boolean result = s3Client.isPathUnavailable(nonExistentPath);

            assertTrue(result);
        }

        @Test
        void isPathUnavailable_throwsException_whenPathIsNull() {
            assertThrows(MinioWrappedException.class, () -> s3Client.isPathUnavailable(null));
        }
    }

    @Nested
    class MakeFolderInS3Tests {
        @Test
        void makeFolderInS3_makesAFolder() {
            var folderName = testPrefix + "newFolder/";

            s3Client.makeFolderInS3(folderName);

            var stat = assertDoesNotThrow(() -> s3Client.getStatObject(folderName));

            assertNotNull(stat);
            assertThat(stat.size()).isEqualTo(0);
            assertThat(stat.object()).isEqualTo(folderName);
        }

        @Test
        void makeFolderInS3_makesNestedFolders() {
            var folderName = testPrefix + "parent/child/grandchild/";

            s3Client.makeFolderInS3(folderName);

            var stat = s3Client.getStatObject(folderName);

            assertNotNull(stat);
            assertThat(stat.size()).isEqualTo(0);
        }

        @Test
        void makeFolderInS3_doesntThrow_whenFolderAlreadyExists() {
            var folderName = testPrefix + "existingFolder/";

            s3Client.makeFolderInS3(folderName);

            // Creating same folder again should not throw
            assertDoesNotThrow(() -> s3Client.makeFolderInS3(folderName));
        }

        @Test
        void makeFolderInS3_throwsException_whenFolderNameIsNull() {
            assertThrows(MinioWrappedException.class, () -> s3Client.makeFolderInS3(null));
        }

        @Test
        void makeFolderInS3_throwsException_whenFolderNameIsEmpty() {
            assertThrows(MinioWrappedException.class, () -> s3Client.makeFolderInS3(""));
        }
    }

    @Nested
    class IntegrationTests {
        @Test
        void fullWorkflow_uploadCopyReadDelete() throws Exception {
            var sourcePath = testPrefix + "workflow/source.txt";
            var copyPath = testPrefix + "workflow/copy.txt";
            var content = "Full workflow test content";

            // 1. Upload
            createTestFile(sourcePath, content);
            assertNotNull(s3Client.getStatObject(sourcePath));

            // 2. Copy
            var sourceItem = getFirstItem(sourcePath);
            assertNotNull(sourceItem);
            s3Client.copyObject(sourceItem, copyPath);
            assertNotNull(s3Client.getStatObject(copyPath));

            // 3. Read
            try (var response = s3Client.getObject(copyPath)) {
                var retrieved = new String(response.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(retrieved).isEqualTo(content);
            }

            // 4. Delete
            s3Client.removeObject(sourcePath);
            s3Client.removeObject(copyPath);
            assertNull(s3Client.getStatObject(sourcePath));
            assertNull(s3Client.getStatObject(copyPath));
        }

        @Test
        void listAndDeleteMultipleObjects() throws Exception {
            var folder = testPrefix + "bulkDelete/";

            // Create multiple files
            for (int i = 0; i < 5; i++) {
                createTestFile(folder + "file" + i + ".txt", "Content " + i);
            }

            // List all files
            var results = s3Client.listObjects(folder, true);
            var paths = new ArrayList<String>();
            for (var result : results) {
                paths.add(result.get().objectName());
            }
            assertThat(paths).hasSize(5);

            // Delete all files
            for (var path : paths) {
                s3Client.removeObject(path);
            }

            // Verify all deleted
            var afterDelete = s3Client.listObjects(folder, true);
            var remaining = new ArrayList<Item>();
            for (var result : afterDelete) {
                remaining.add(result.get());
            }
            assertThat(remaining).isEmpty();
        }
    }
}