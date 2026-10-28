package com.ptit.cvrecruiter.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Gson dùng chung cho toàn ứng dụng. */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private static final Gson PRETTY = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static Gson pretty() {
        return PRETTY;
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
}
