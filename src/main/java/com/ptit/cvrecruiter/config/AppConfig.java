package com.ptit.cvrecruiter.config;

/**
 * Cấu hình ứng dụng (bất biến), gom theo từng nhóm trách nhiệm.
 * Đọc từ config.properties và biến môi trường bằng {@link ConfigLoader}.
 *
 * @param port          cổng HTTP
 * @param adminPassword mật khẩu đăng nhập trang quản trị
 * @param ai            nhà cung cấp AI
 * @param weights       trọng số ghép điểm
 * @param storage       nơi lưu dữ liệu và file CV
 */
public record AppConfig(int port, String adminPassword, AiSettings ai, ScoringWeights weights,
                        StorageSettings storage) {

    /** Đọc cấu hình từ config.properties trong thư mục chạy, ghi đè bằng biến môi trường. */
    public static AppConfig load() {
        return new ConfigLoader().load();
    }
}
