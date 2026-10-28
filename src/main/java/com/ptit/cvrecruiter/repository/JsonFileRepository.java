package com.ptit.cvrecruiter.repository;

import com.google.gson.reflect.TypeToken;
import com.ptit.cvrecruiter.util.JsonUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Repository tổng quát lưu dữ liệu dạng danh sách trong một file JSON.
 * Ghi file theo kiểu ghi tạm rồi đổi tên để tránh hỏng dữ liệu.
 */
public class JsonFileRepository<T> {

    private static final class Wrapper<T> {
        List<T> items = new ArrayList<>();
    }

    private final Path file;
    private final Type wrapperType;
    private final Function<T, String> idGetter;
    private final List<T> items;
    private final Object lock = new Object();

    public JsonFileRepository(Path file, Class<T> type, Function<T, String> idGetter) {
        this.file = file;
        this.wrapperType = TypeToken.getParameterized(Wrapper.class, type).getType();
        this.idGetter = idGetter;
        this.items = load();
    }

    private List<T> load() {
        try {
            if (!Files.exists(file)) return new ArrayList<>();
            String json = Files.readString(file);
            // Phải dùng kiểu tham số hoá, nếu không Gson sẽ trả về LinkedTreeMap
            // và mọi lời gọi kiểu JobPosting/Application sẽ ném ClassCastException.
            Wrapper<T> wrapper = JsonUtil.gson().fromJson(json, wrapperType);
            return wrapper == null || wrapper.items == null ? new ArrayList<>() : new ArrayList<>(wrapper.items);
        } catch (IOException e) {
            throw new IllegalStateException("Khong doc duoc file du lieu " + file + ": " + e.getMessage(), e);
        }
    }

    private void persist() {
        try {
            Wrapper<T> wrapper = new Wrapper<>();
            wrapper.items = new ArrayList<>(items);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.createDirectories(file.getParent());
            Files.writeString(tmp, JsonUtil.pretty().toJson(wrapper));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Khong ghi duoc file du lieu " + file + ": " + e.getMessage(), e);
        }
    }

    public List<T> findAll() {
        synchronized (lock) {
            return new ArrayList<>(items);
        }
    }

    public Optional<T> findById(String id) {
        synchronized (lock) {
            return items.stream().filter(t -> idGetter.apply(t).equals(id)).findFirst();
        }
    }

    public T save(T entity) {
        synchronized (lock) {
            String id = idGetter.apply(entity);
            for (int i = 0; i < items.size(); i++) {
                if (idGetter.apply(items.get(i)).equals(id)) {
                    items.set(i, entity);
                    persist();
                    return entity;
                }
            }
            items.add(entity);
            persist();
            return entity;
        }
    }

    public boolean delete(String id) {
        synchronized (lock) {
            boolean removed = items.removeIf(t -> idGetter.apply(t).equals(id));
            if (removed) persist();
            return removed;
        }
    }

    public int count() {
        synchronized (lock) {
            return items.size();
        }
    }
}
