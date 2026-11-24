package cvrecruiter.config;

import java.nio.file.Path;

/**
 * Nơi lưu dữ liệu.
 *
 * @param dataDir        thư mục chứa jobs.json và applications.json
 * @param uploadsDir     thư mục chứa file CV đã tải lên
 * @param maxUploadBytes dung lượng tối đa của một file CV
 */
public record StorageSettings(Path dataDir, Path uploadsDir, int maxUploadBytes) {

    public Path jobsFile() {
        return dataDir.resolve("jobs.json");
    }

    public Path applicationsFile() {
        return dataDir.resolve("applications.json");
    }
}
