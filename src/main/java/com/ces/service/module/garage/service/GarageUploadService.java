package com.ces.service.module.garage.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded vehicle photos and documents on local disk. A near-copy of
 * {@code InventoryUploadService} rather than a shared/generalized one — the only real difference
 * is documents also need to accept PDFs, and duplicating ~60 lines beats coupling two independent
 * modules through a shared upload service neither owns.
 */
@Service
public class GarageUploadService {

    private static final long MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024; // 15MB — a scanned passport can be large
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "application/pdf", ".pdf");

    private final Path storageDir;
    private final String urlPrefix;

    public GarageUploadService(
            @Value("${ces.uploads.dir}") String uploadsDir,
            @Value("${ces.uploads.url-prefix}") String urlPrefix) {
        this.storageDir = Paths.get(uploadsDir).toAbsolutePath().normalize().resolve("garage");
        this.urlPrefix = urlPrefix;
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create garage uploads directory: " + this.storageDir, e);
        }
    }

    public record StoredFile(String fileName, String url, long size) {
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String extension = ALLOWED_CONTENT_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String storedName = UUID.randomUUID() + extension;
        Path target = storageDir.resolve(storedName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : storedName;
        return new StoredFile(originalName, urlPrefix + "/garage/" + storedName, file.getSize());
    }
}
