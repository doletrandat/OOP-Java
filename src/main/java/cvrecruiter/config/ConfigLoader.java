package cvrecruiter.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;

/**
 * Đọc {@link AppConfig}: giá trị trong config.properties (nếu có) bị ghi đè bởi biến môi trường,
 * thiếu thì dùng mặc định để chạy được ngay.
 */
public class ConfigLoader {

    private final Path file;
    private final Function<String, String> env;

    public ConfigLoader() {
        this(Path.of("config.properties"), System::getenv);
    }

    /** Cho phép kiểm thử với file và biến môi trường giả. */
    public ConfigLoader(Path file, Function<String, String> env) {
        this.file = file;
        this.env = env;
    }

    public AppConfig load() {
        Properties props = readProperties();
        Source source = new Source(props, env);

        StorageSettings storage = new StorageSettings(
                Path.of(source.get("DATA_DIR", "data")),
                Path.of(source.get("UPLOADS_DIR", "uploads")),
                source.getInt("MAX_UPLOAD_MB", 15) * 1024 * 1024);
        createDirectories(storage);

        return new AppConfig(
                source.getInt("PORT", 8080),
                source.get("ADMIN_PASSWORD", "admin123"),
                loadAi(source),
                new ScoringWeights(
                        source.getDouble("AI_WEIGHT", ScoringWeights.DEFAULT_AI),
                        source.getDouble("CRITERIA_WEIGHT", ScoringWeights.DEFAULT_CRITERIA)),
                storage);
    }

    private AiSettings loadAi(Source source) {
        // Không hard-code khoá API trong mã nguồn: đặt trong config.properties (đã gitignore)
        // hoặc biến môi trường.
        return new AiSettings(
                source.get("AI_API_KEY", ""),
                source.get("AI_MODEL", AiSettings.DEFAULT_MODEL),
                source.getInt("AI_TIMEOUT_SECONDS", 60));
    }

    private Properties readProperties() {
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException ignored) {
                // Đọc lỗi thì dùng mặc định và biến môi trường
            }
        }
        return props;
    }

    private static void createDirectories(StorageSettings storage) {
        try {
            Files.createDirectories(storage.dataDir());
            Files.createDirectories(storage.uploadsDir());
        } catch (IOException e) {
            throw new IllegalStateException("Khong tao duoc thu muc du lieu: " + e.getMessage(), e);
        }
    }

    /** Nguồn giá trị: biến môi trường ưu tiên hơn file properties. */
    private record Source(Properties props, Function<String, String> env) {

        String get(String key, String fallback) {
            String fromEnv = env.apply(key);
            if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();
            String fromFile = props.getProperty(key);
            if (fromFile != null && !fromFile.isBlank()) return fromFile.trim();
            return fallback;
        }

        int getInt(String key, int fallback) {
            try {
                return Integer.parseInt(get(key, String.valueOf(fallback)));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        double getDouble(String key, double fallback) {
            try {
                return Double.parseDouble(get(key, String.valueOf(fallback)));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }
}
