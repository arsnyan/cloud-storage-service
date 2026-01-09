package com.arsnyan.cloudstorageservice.scheduler;

import com.arsnyan.cloudstorageservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrphanFileRemovalTask {
    private final FileStorageService fileStorageService;
    private final FileEntityService fileEntityService;

    @Scheduled(cron = "0 0 * * * *")
    public void run() {
        var staleFiles = fileEntityService.getStaleFiles();

        for (var file : staleFiles) {
            fileStorageService.deleteResourceById(file.getFileId());
        }

        var staleIds = staleFiles.stream().map(FileEntity::getFileId).toList();
        fileEntityService.batchDeleteByIds(staleIds);
    }
}
