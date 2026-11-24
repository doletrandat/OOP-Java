package com.ptit.cvrecruiter.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/** Lưu CV trên ổ đĩa: mỗi vị trí một thư mục, mỗi file một tên ngẫu nhiên. */
public class LocalCvStorage implements CvStorage {

    private final Path root;

    public LocalCvStorage(Path root) {
        this.root = root;
    }

    @Override
    public Path store(String jobId, byte[] pdfBytes) {
        try {
            Path dir = root.resolve(jobId);
            Files.createDirectories(dir);
            Path file = dir.resolve(UUID.randomUUID() + ".pdf");
            Files.write(file, pdfBytes);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Khong luu duoc file CV: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // File có thể đã bị xoá tay; không chặn nghiệp vụ
        }
    }

    @Override
    public void deleteJobFolder(String jobId) {
        if (jobId == null || jobId.isBlank()) return;
        Path dir = root.resolve(jobId);
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::delete);
        } catch (IOException ignored) {
            // Dọn dẹp không được thì để lại thư mục rỗng
        }
    }
}
