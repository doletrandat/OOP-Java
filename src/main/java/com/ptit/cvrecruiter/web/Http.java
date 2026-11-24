package com.ptit.cvrecruiter.web;

import com.ptit.cvrecruiter.util.JsonUtil;
import com.google.gson.JsonParseException;
import com.ptit.cvrecruiter.common.ValidationException;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Các tiện ích đọc/ghi HTTP dùng chung cho các controller. */
public final class Http {

    /** Kết quả parse multipart: các trường text + một file (CV PDF). */
    public record MultipartData(Map<String, String> fields, String fileName, byte[] fileBytes) {
    }

    private static final Pattern ATTR_QUOTED = Pattern.compile("(\\w+)=\"([^\"]*)\"");
    private static final Pattern ATTR_PLAIN = Pattern.compile("(\\w+)=([^;\\s]+)");

    private Http() {
    }

    public static void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = JsonUtil.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange ex, int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        try {
            sendJson(ex, status, body);
        } catch (IOException ignored) {
            // Kênh response đã đóng thì bỏ qua
        }
    }

    /** Gửi dữ liệu nhị phân (PDF, ảnh). */
    public static void sendBytes(HttpExchange ex, String contentType, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Đọc body JSON thành đối tượng; body rỗng được coi như {@code {}}. */
    public static <T> T readJson(HttpExchange ex, Class<T> type) throws IOException {
        String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return JsonUtil.gson().fromJson(raw.isBlank() ? "{}" : raw, type);
        } catch (JsonParseException e) {
            throw new ValidationException("Du lieu JSON khong hop le.");
        }
    }

    /** Đọc và phân tích multipart/form-data, tách trường text và file. */
    public static MultipartData readMultipart(HttpExchange ex, int maxBytes) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("multipart/form-data")) {
            throw new ValidationException("Request phai la multipart/form-data.");
        }
        String boundary = null;
        for (String piece : contentType.split(";")) {
            String t = piece.trim();
            if (t.toLowerCase(Locale.ROOT).startsWith("boundary=")) {
                boundary = t.substring(9).replace("\"", "");
            }
        }
        if (boundary == null || boundary.isBlank()) {
            throw new ValidationException("Thieu boundary trong multipart request.");
        }

        byte[] data = ex.getRequestBody().readNBytes(maxBytes + 1);
        if (data.length > maxBytes) {
            throw new ValidationException("Du lieu upload qua lon.");
        }
        byte[] delim = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

        Map<String, String> fields = new LinkedHashMap<>();
        String fileName = null;
        byte[] fileBytes = null;

        for (byte[] segment : split(data, delim)) {
            if (segment.length < 4) continue;
            if (segment[0] == '-' && segment[1] == '-') continue; // ranh giới kết thúc "--"
            int start = (segment[0] == '\r' && segment[1] == '\n') ? 2 : 0;
            int headerEnd = indexOf(segment, new byte[]{'\r', '\n', '\r', '\n'}, start);
            if (headerEnd < 0) continue;

            String headers = new String(segment, start, headerEnd - start, StandardCharsets.UTF_8);
            int contentStart = headerEnd + 4;
            int contentEnd = Math.max(contentStart, segment.length - 2); // bỏ \r\n cuối
            byte[] content = Arrays.copyOfRange(segment, contentStart, contentEnd);

            String name = attribute(headers, "name");
            String filename = attribute(headers, "filename");
            if (filename != null && !filename.isBlank()) {
                if (fileBytes == null) {
                    fileName = filename.replace('\\', '/');
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                    fileBytes = content;
                }
            } else if (name != null) {
                fields.put(name, new String(content, StandardCharsets.UTF_8).trim());
            }
        }
        return new MultipartData(fields, fileName, fileBytes);
    }

    private static List<byte[]> split(byte[] data, byte[] delim) {
        List<byte[]> parts = new ArrayList<>();
        ByteArrayOutputStream current = new ByteArrayOutputStream();
        for (int i = 0; i < data.length; i++) {
            if (matchesAt(data, i, delim)) {
                parts.add(current.toByteArray());
                current.reset();
                i += delim.length - 1;
            } else {
                current.write(data[i]);
            }
        }
        parts.add(current.toByteArray());
        return parts;
    }

    private static boolean matchesAt(byte[] data, int pos, byte[] delim) {
        if (pos + delim.length > data.length) return false;
        for (int j = 0; j < delim.length; j++) {
            if (data[pos + j] != delim[j]) return false;
        }
        return true;
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        for (int i = from; i <= data.length - pattern.length; i++) {
            if (matchesAt(data, i, pattern)) return i;
        }
        return -1;
    }

    private static String attribute(String headers, String attr) {
        Matcher quoted = ATTR_QUOTED.matcher(headers);
        while (quoted.find()) {
            if (quoted.group(1).equalsIgnoreCase(attr)) return quoted.group(2);
        }
        Matcher plain = ATTR_PLAIN.matcher(headers);
        while (plain.find()) {
            if (plain.group(1).equalsIgnoreCase(attr)) return plain.group(2);
        }
        return null;
    }
}
