package com.ptit.cvrecruiter.matching;

import com.ptit.cvrecruiter.util.TextNormalizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Từ điển đồng nghĩa Việt - Anh cho các kỹ năng phổ biến.
 * Giúp khớp "Database" trong JD với "cơ sở dữ liệu" trong CV và ngược lại.
 */
public class SynonymDictionary {

    /** canonical -> danh sách biến thể (đã gồm cả chính nó). */
    private static final Map<String, List<String>> CANONICAL = new LinkedHashMap<>();

    /** biến thể -> canonical, để tra ngược khi HR nhập tên không trùng key. */
    private static final Map<String, String> REVERSE = new LinkedHashMap<>();

    static {
        add("database", "database", "databases", "cơ sở dữ liệu", "csdl",
                "hệ quản trị cơ sở dữ liệu", "dbms");
        add("sql", "sql", "mysql", "postgresql", "postgres", "sql server",
                "sqlite", "oracle db", "pl/sql", "t-sql");
        add("oop", "oop", "object oriented", "object-oriented",
                "lập trình hướng đối tượng", "hướng đối tượng");
        add("javascript", "javascript", "js", "ecmascript", "es6");
        add("typescript", "typescript", "ts");
        add("react", "react", "reactjs", "react.js", "react js");
        add("angular", "angular", "angularjs");
        add("vue", "vue", "vuejs", "vue.js");
        add("node.js", "node.js", "nodejs", "node js", "node");
        add("spring boot", "spring boot", "springboot", "spring");
        add("html", "html", "html5");
        add("css", "css", "css3");
        add("c#", "c#", "csharp", "c sharp");
        add("c++", "c++", "cpp");
        add(".net", ".net", "dotnet", "asp.net", ".net core");
        add("python", "python");
        add("php", "php", "laravel");
        add("java", "java", "core java");
        add("docker", "docker", "container", "container hóa", "container hoa");
        add("kubernetes", "kubernetes", "k8s");
        add("rest api", "rest api", "restful", "rest", "api", "webservice", "web service");
        add("git", "git", "github", "gitlab", "version control", "quản lý phiên bản");
        add("ci/cd", "ci/cd", "cicd", "ci cd", "tích hợp liên tục", "jenkins", "github actions");
        add("devops", "devops", "dev ops");
        add("microservices", "microservices", "microservice", "micro service",
                "kiến trúc microservices");
        add("aws", "aws", "amazon web services", "ec2", "s3");
        add("cloud computing", "cloud computing", "cloud", "điện toán đám mây",
                "tính toán đám mây", "đám mây");
        add("linux", "linux", "ubuntu", "centos");
        add("mongodb", "mongodb", "mongo", "nosql", "no sql");
        add("redis", "redis");
        add("testing", "testing", "test", "kiểm thử", "unit test", "kiểm thử đơn vị",
                "junit", "automation test");
        add("agile", "agile", "scrum", "kanban");
        add("machine learning", "machine learning", "học máy", "máy học", "ml");
        add("deep learning", "deep learning", "học sâu");
        add("nlp", "nlp", "xử lý ngôn ngữ tự nhiên");
        add("computer vision", "computer vision", "thị giác máy tính");
        add("data analysis", "data analysis", "data analytics", "phân tích dữ liệu");
        add("statistics", "statistics", "thống kê");
        add("excel", "excel", "bảng tính");
        add("power bi", "power bi", "powerbi");
        add("data structures", "data structures", "cấu trúc dữ liệu");
        add("algorithms", "algorithms", "algorithm", "giải thuật", "thuật toán");
        add("frontend", "frontend", "front-end", "front end", "phát triển giao diện");
        add("backend", "backend", "back-end", "back end");
        add("fullstack", "fullstack", "full stack", "full-stack");
        add("web development", "web development", "phát triển web", "lập trình web");
        add("english", "english", "tiếng anh", "ngoại ngữ", "ielts", "toeic");
        add("communication", "communication", "giao tiếp", "kỹ năng giao tiếp");
        add("teamwork", "teamwork", "làm việc nhóm", "kỹ năng làm việc nhóm");
        add("problem solving", "problem solving", "giải quyết vấn đề");
        add("critical thinking", "critical thinking", "tư duy phản biện");
        add("project management", "project management", "quản lý dự án");
        add("time management", "time management", "quản lý thời gian");
        add("leadership", "leadership", "lãnh đạo", "dẫn dắt đội nhóm");
        add("presentation", "presentation", "thuyết trình");
        add("reporting", "reporting", "báo cáo");
        add("design pattern", "design pattern", "mẫu thiết kế");
    }

    private static void add(String canonical, String... variants) {
        List<String> list = new ArrayList<>();
        for (String v : variants) {
            String norm = TextNormalizer.normalize(v);
            if (!norm.isEmpty() && !list.contains(norm)) {
                list.add(norm);
                REVERSE.putIfAbsent(norm, canonical);
            }
        }
        CANONICAL.put(TextNormalizer.normalize(canonical), list);
    }

    /**
     * Trả về danh sách các cách diễn đạt có thể có của một kỹ năng
     * (gốc + biến thể đồng nghĩa), đã chuẩn hoá.
     */
    public List<String> expand(String skillName) {
        String norm = TextNormalizer.normalize(skillName);
        if (norm.isEmpty()) return List.of();
        List<String> direct = CANONICAL.get(norm);
        if (direct != null) return direct;
        String canonical = REVERSE.get(norm);
        if (canonical != null) return CANONICAL.get(canonical);
        // Kỹ năng lạ: dùng chính tên đã chuẩn hoá (kèm bản gốc, đã bỏ dấu)
        List<String> fallback = new ArrayList<>();
        fallback.add(norm);
        String rawNorm = skillName.trim().toLowerCase(Locale.ROOT);
        if (!fallback.contains(rawNorm)) fallback.add(rawNorm);
        return fallback;
    }
}
