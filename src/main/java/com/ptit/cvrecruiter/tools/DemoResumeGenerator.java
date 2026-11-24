package com.ptit.cvrecruiter.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

/**
 * Tạo CV PDF cho bộ dữ liệu demo từ các file văn bản trong demo-data/resumes.
 *
 * <p>Mỗi file .txt được kết xuất thành file .pdf cùng tên, đặt cạnh nó. Các dòng đầu
 * chọn mẫu trình bày và màu nhấn:
 * <pre>
 * &#64;style sidebar        modern | sidebar | banner | classic | timeline | elegant
 *                         plain | word | notepad | typewriter (CV gõ vội, không định dạng)
 * &#64;accent #0f766e       màu nhấn (tuỳ chọn)
 * </pre>
 *
 * <p>Cú pháp các dòng còn lại:
 * <pre>
 * # Họ tên               tên ở đầu trang
 * &gt; dòng phụ            chức danh; dòng có email/số điện thoại được coi là liên hệ
 * ## Mục                 tiêu đề mục
 * = Trái | Phải           dòng có thời gian (vị trí | thời gian)
 * - gạch đầu dòng
 * văn bản thường         tự xuống dòng theo bề rộng cột
 * </pre>
 *
 * <p>Tiếng Việt cần phông TrueType của Windows (Segoe UI, Times New Roman, Tahoma, Corbel,
 * Candara, Constantia, Consolas...). Các phông này đã được thử: chữ trích xuất lại từ PDF
 * khớp nguyên văn. Không dùng Calibri, Palatino (chữ ghép "ti", "tt" bị đọc sai) hay
 * Georgia, Trebuchet (thiếu dấu tiếng Việt). Trên máy không có các phông này, mọi mẫu
 * dùng DejaVu. Có thể chỉ định thư mục phông qua biến môi trường {@code DEMO_FONT_DIR}.
 *
 * <p>Chạy: {@code java -cp target/cv-recruiter.jar com.ptit.cvrecruiter.tools.DemoResumeGenerator demo-data/resumes}
 */
public final class DemoResumeGenerator {

    private static final float W = PDRectangle.A4.getWidth();
    private static final float H = PDRectangle.A4.getHeight();

    private DemoResumeGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "demo-data/resumes");
        List<Path> sources;
        try (Stream<Path> s = Files.walk(root)) {
            sources = s.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
        }
        for (Path src : sources) {
            Path pdf = src.resolveSibling(src.getFileName().toString().replaceAll("\\.txt$", ".pdf"));
            Doc doc = Doc.parse(Files.readAllLines(src, StandardCharsets.UTF_8));
            if (doc.photoRef != null) {
                doc.photo = src.getParent().resolve(doc.photoRef).normalize();
                if (!Files.exists(doc.photo)) throw new IllegalArgumentException("Khong tim thay anh: " + doc.photo);
            }
            Files.deleteIfExists(pdf);
            render(doc, pdf);
            System.out.printf("  %-48s %-10s %s%n", root.relativize(pdf), doc.style, doc.photo != null ? "+ anh" : "");
        }
        System.out.println("Da tao " + sources.size() + " CV PDF trong " + root.toAbsolutePath());
    }

    /* ======================= Mô hình nội dung ======================= */

    private sealed interface Block permits Entry, Bullet, Para {
    }

    private record Entry(String left, String right) implements Block {
    }

    private record Bullet(String text) implements Block {
    }

    private record Para(String text) implements Block {
    }

    private record Section(String title, List<Block> blocks) {
    }

    private static final class Doc {
        String style = "modern";
        Color accent;
        String photoRef;
        Path photo;
        PDImageXObject image;
        String name = "";
        final List<String> roles = new ArrayList<>();
        final List<String> contacts = new ArrayList<>();
        final List<Section> sections = new ArrayList<>();
        final List<String> raw = new ArrayList<>();
        boolean vietnamese;

        static Doc parse(List<String> lines) {
            Doc d = new Doc();
            Section cur = null;
            for (String line : lines) {
                if (line.startsWith("@style")) {
                    d.style = line.substring(6).trim();
                    continue;
                }
                if (line.startsWith("@accent")) {
                    d.accent = Color.decode(line.substring(7).trim());
                    continue;
                }
                if (line.startsWith("@photo")) {
                    d.photoRef = line.substring(6).trim();
                    continue;
                }
                d.raw.add(line);
                if (line.matches(".*[ăâđêôơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ].*")) d.vietnamese = true;
                if (line.startsWith("# ")) {
                    d.name = line.substring(2).trim();
                } else if (line.startsWith("> ")) {
                    String t = line.substring(2).trim();
                    if (t.contains("@") || t.replaceAll("\\D", "").length() >= 8) d.contacts.add(t);
                    else d.roles.add(t);
                } else if (line.startsWith("## ")) {
                    cur = new Section(line.substring(3).trim(), new ArrayList<>());
                    d.sections.add(cur);
                } else if (!line.isBlank()) {
                    if (cur == null) {
                        cur = new Section("", new ArrayList<>());
                        d.sections.add(cur);
                    }
                    if (line.startsWith("= ")) {
                        String[] p = line.substring(2).split("\\|", 2);
                        cur.blocks().add(new Entry(p[0].trim(), p.length > 1 ? p[1].trim() : ""));
                    } else if (line.startsWith("- ")) {
                        cur.blocks().add(new Bullet(line.substring(2).trim()));
                    } else {
                        cur.blocks().add(new Para(line.trim()));
                    }
                }
            }
            return d;
        }

        /** Các mảnh liên hệ, tách theo dấu "·" hoặc "|". */
        List<String> contactItems() {
            List<String> out = new ArrayList<>();
            for (String c : contacts) {
                for (String part : c.split("\\s*[·|]\\s*")) {
                    if (!part.isBlank()) out.add(part.trim());
                }
            }
            return out;
        }
    }

    /* ======================= Phông chữ ======================= */

    private record FontSet(PDFont regular, PDFont bold) {
    }

    private static final class Fonts {
        private final PDDocument doc;
        private final Path dir;
        private final boolean windows;
        private final Map<String, PDFont> loaded = new HashMap<>();

        Fonts(PDDocument doc) {
            this.doc = doc;
            List<Path> dirs = new ArrayList<>();
            String custom = System.getenv("DEMO_FONT_DIR");
            if (custom != null) dirs.add(Path.of(custom));
            dirs.add(Path.of("C:/Windows/Fonts"));
            dirs.add(Path.of("/usr/share/fonts/truetype/dejavu"));
            Path found = null;
            for (Path d : dirs) {
                if (Files.exists(d.resolve("arial.ttf")) || Files.exists(d.resolve("DejaVuSans.ttf"))) {
                    found = d;
                    break;
                }
            }
            if (found == null) throw new IllegalStateException("Khong tim thay phong ho tro tieng Viet. Dat DEMO_FONT_DIR.");
            dir = found;
            windows = Files.exists(found.resolve("arial.ttf"));
        }

        /** Cặp phông thường/đậm theo tên Windows; thiếu thì lùi về Arial hoặc DejaVu. */
        FontSet get(String regular, String bold) throws IOException {
            return new FontSet(load(regular, false), load(bold, true));
        }

        FontSet mono() throws IOException {
            return windows ? get("cour.ttf", "courbd.ttf") : new FontSet(loadPath(dir.resolve("DejaVuSansMono.ttf")),
                    loadPath(dir.resolve("DejaVuSansMono-Bold.ttf")));
        }

        private PDFont load(String file, boolean bold) throws IOException {
            Path p = dir.resolve(file);
            if (!Files.exists(p)) {
                p = dir.resolve(windows ? (bold ? "arialbd.ttf" : "arial.ttf") : (bold ? "DejaVuSans-Bold.ttf" : "DejaVuSans.ttf"));
            }
            return loadPath(p);
        }

        private PDFont loadPath(Path p) throws IOException {
            PDFont f = loaded.get(p.toString());
            if (f == null) {
                f = PDType0Font.load(doc, p.toFile());
                loaded.put(p.toString(), f);
            }
            return f;
        }
    }

    /* ======================= Vẽ ======================= */

    /** Quản lý các trang; mỗi trang mới được trang trí (nền, dải màu) trước khi vẽ chữ. */
    private static final class Canvas {
        final PDDocument doc;
        final List<PDPageContentStream> streams = new ArrayList<>();
        IntConsumer decorate = i -> { };

        Canvas(PDDocument doc) {
            this.doc = doc;
        }

        PDPageContentStream page(int i) throws IOException {
            while (streams.size() <= i) {
                PDPage p = new PDPage(PDRectangle.A4);
                doc.addPage(p);
                streams.add(new PDPageContentStream(doc, p));
                decorate.accept(streams.size() - 1);
            }
            return streams.get(i);
        }

        void fill(int page, float x, float y, float w, float h, Color c) {
            try {
                PDPageContentStream cs = page(page);
                cs.setNonStrokingColor(c);
                cs.addRect(x, y, w, h);
                cs.fill();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        void line(int page, float x1, float y1, float x2, float y2, Color c, float width) throws IOException {
            PDPageContentStream cs = page(page);
            cs.setStrokingColor(c);
            cs.setLineWidth(width);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        void text(int page, float x, float baseline, String s, PDFont f, float size, Color c) throws IOException {
            PDPageContentStream cs = page(page);
            cs.beginText();
            cs.setFont(f, size);
            cs.setNonStrokingColor(c);
            cs.newLineAtOffset(x, baseline);
            cs.showText(s);
            cs.endText();
        }

        /** Ảnh chân dung cắt theo khung tròn hoặc chữ nhật, phủ kín khung (cắt bớt phần thừa). */
        void photo(int page, PDImageXObject img, float x, float y, float w, float h, boolean circle,
                   Color border, float borderWidth) throws IOException {
            PDPageContentStream cs = page(page);
            cs.saveGraphicsState();
            shape(cs, circle, x, y, w, h);
            cs.clip();
            float scale = Math.max(w / img.getWidth(), h / img.getHeight());
            float iw = img.getWidth() * scale;
            float ih = img.getHeight() * scale;
            cs.drawImage(img, x + (w - iw) / 2, y + (h - ih) / 2, iw, ih);
            cs.restoreGraphicsState();
            if (border != null) {
                cs.setStrokingColor(border);
                cs.setLineWidth(borderWidth);
                shape(cs, circle, x, y, w, h);
                cs.stroke();
            }
        }

        private static void shape(PDPageContentStream cs, boolean circle, float x, float y, float w, float h)
                throws IOException {
            if (!circle) {
                cs.addRect(x, y, w, h);
                return;
            }
            float k = 0.5523f;
            float rx = w / 2;
            float ry = h / 2;
            float cx = x + rx;
            float cy = y + ry;
            cs.moveTo(cx + rx, cy);
            cs.curveTo(cx + rx, cy + k * ry, cx + k * rx, cy + ry, cx, cy + ry);
            cs.curveTo(cx - k * rx, cy + ry, cx - rx, cy + k * ry, cx - rx, cy);
            cs.curveTo(cx - rx, cy - k * ry, cx - k * rx, cy - ry, cx, cy - ry);
            cs.curveTo(cx + k * rx, cy - ry, cx + rx, cy - k * ry, cx + rx, cy);
            cs.closePath();
        }

        void close() throws IOException {
            for (PDPageContentStream s : streams) s.close();
        }
    }

    private static float width(String s, PDFont f, float size) {
        try {
            return f.getStringWidth(s) / 1000 * size;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> wrap(String text, PDFont f, float size, float max) {
        List<String> rows = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String trial = cur.isEmpty() ? word : cur + " " + word;
            if (width(trial, f, size) > max && !cur.isEmpty()) {
                rows.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(trial);
            }
        }
        if (!cur.isEmpty()) rows.add(cur.toString());
        return rows;
    }

    private static Color tint(Color c, float amount) {
        return new Color(
                Math.round(c.getRed() + (255 - c.getRed()) * amount),
                Math.round(c.getGreen() + (255 - c.getGreen()) * amount),
                Math.round(c.getBlue() + (255 - c.getBlue()) * amount));
    }

    private static final Color INK = new Color(30, 32, 38);
    private static final Color GRAY = new Color(95, 99, 110);
    private static final Color LIGHT = new Color(205, 208, 214);

    /** Một cột chữ chảy từ trên xuống, tự sang trang. */
    private static final class Flow {
        final Canvas c;
        final float x;
        float w;
        final float top;
        final float bottom;
        int page;
        float y;

        Flow(Canvas c, float x, float w, float startY, float top, float bottom) {
            this.c = c;
            this.x = x;
            this.w = w;
            this.y = startY;
            this.top = top;
            this.bottom = bottom;
        }

        void need(float h) {
            if (y - h < bottom) {
                page++;
                y = top;
            }
        }

        /** Một dòng chữ; trả về toạ độ đường cơ sở. */
        float line(String s, PDFont f, float size, Color col, float dx) throws IOException {
            float lead = size * 1.38f;
            need(lead);
            float base = y - size;
            c.text(page, x + dx, base, s, f, size, col);
            y -= lead;
            return base;
        }

        void wrapped(String s, PDFont f, float size, Color col, float indent) throws IOException {
            for (String row : wrap(s, f, size, w - indent)) line(row, f, size, col, indent);
        }

        void bullet(String s, PDFont f, float size, Color text, Color mark, String sign, float indent) throws IOException {
            List<String> rows = wrap(s, f, size, w - indent);
            for (int i = 0; i < rows.size(); i++) {
                if (i == 0) {
                    float lead = size * 1.38f;
                    need(lead);
                    c.text(page, x + indent - size * 0.95f, y - size, sign, f, size, mark);
                }
                line(rows.get(i), f, size, text, indent);
            }
        }

        void gap(float h) {
            y -= h;
        }
    }

    /* ======================= Các mẫu ======================= */

    private static void render(Doc d, Path out) throws IOException {
        try (PDDocument pdf = new PDDocument()) {
            pdf.getDocumentInformation().setCreator("CV Screener demo data");
            Fonts fonts = new Fonts(pdf);
            Canvas c = new Canvas(pdf);
            if (d.photo != null) d.image = PDImageXObject.createFromFile(d.photo.toString(), pdf);
            switch (d.style) {
                case "plain" -> raw(d, c, fonts.get("arial.ttf", "arial.ttf"), 11, 60);
                case "word" -> raw(d, c, fonts.get("times.ttf", "times.ttf"), 12, 72);
                case "notepad" -> raw(d, c, fonts.get("consola.ttf", "consola.ttf"), 10, 50);
                case "typewriter" -> raw(d, c, fonts.mono(), 10, 56);
                case "sidebar" -> sidebar(d, c, fonts.get("corbel.ttf", "corbelb.ttf"));
                case "banner" -> banner(d, c, fonts.get("tahoma.ttf", "tahomabd.ttf"));
                case "classic" -> classic(d, c, fonts.get("times.ttf", "timesbd.ttf"));
                case "timeline" -> timeline(d, c, fonts.get("candara.ttf", "Candarab.ttf"));
                case "elegant" -> elegant(d, c, fonts.get("constan.ttf", "constanb.ttf"));
                default -> modern(d, c, fonts.get("segoeui.ttf", "segoeuib.ttf"));
            }
            c.page(0);
            c.close();
            pdf.save(out.toFile());
        }
    }

    /** CV gõ vội: in nguyên văn, không định dạng. */
    private static void raw(Doc d, Canvas c, FontSet f, float size, float margin) throws IOException {
        Flow flow = new Flow(c, margin, W - 2 * margin, H - margin, H - margin, margin);
        if (d.image != null) {
            // Ảnh dán vội ở góc trên, không khung, chữ bắt đầu bên dưới
            c.photo(0, d.image, margin, H - margin - 132, 132, 132, false, null, 0);
            flow.gap(132 + 14);
        }
        for (String line : d.raw) {
            if (line.isBlank()) flow.gap(size);
            else flow.wrapped(line, f.regular(), size, Color.BLACK, 0);
        }
    }

    /** Một cột, tiêu đề màu có đường kẻ, thời gian căn phải. */
    private static void modern(Doc d, Canvas c, FontSet f) throws IOException {
        Color accent = d.accent != null ? d.accent : new Color(37, 70, 170);
        float m = 52;
        Flow flow = new Flow(c, m, W - 2 * m, H - 50, H - m, m);
        float photoBottom = topRightPhoto(d, c, flow, W - m, H - 46, 84, true, tint(accent, 0.4f));
        flow.line(d.name, f.bold(), 25, accent, 0);
        flow.gap(4);
        for (String r : d.roles) flow.wrapped(r, f.regular(), 10, GRAY, 0);
        for (String r : d.contacts) flow.wrapped(r, f.regular(), 9.5f, GRAY, 0);
        endHeader(flow, W - 2 * m, photoBottom);
        for (Section s : d.sections) {
            flow.gap(12);
            if (!s.title().isEmpty()) {
                flow.need(40);
                flow.line(s.title().toUpperCase(), f.bold(), 11, accent, 0);
                c.line(flow.page, m, flow.y + 2, W - m, flow.y + 2, tint(accent, 0.55f), 0.7f);
                flow.gap(6);
            }
            body(flow, s, f, 9.5f, INK, accent, "•", 11, true);
        }
    }

    /**
     * Ảnh ở góc trên bên phải phần đầu CV; thu hẹp cột chữ của phần đầu để không đè lên ảnh.
     * Trả về toạ độ đáy ảnh (hoặc +∞ nếu CV không có ảnh).
     */
    private static float topRightPhoto(Doc d, Canvas c, Flow header, float right, float top, float size,
                                       boolean circle, Color border) throws IOException {
        if (d.image == null) return Float.MAX_VALUE;
        float w = circle ? size : size * 0.75f;
        c.photo(0, d.image, right - w, top - size, w, size, circle, border, circle ? 2f : 0.8f);
        header.w -= w + 18;
        return top - size;
    }

    /** Kết thúc phần đầu: trả lại bề rộng cột và đẩy xuống dưới ảnh nếu ảnh cao hơn chữ. */
    private static void endHeader(Flow flow, float fullWidth, float photoBottom) {
        flow.w = fullWidth;
        if (photoBottom != Float.MAX_VALUE) flow.y = Math.min(flow.y, photoBottom - 6);
    }

    /**
     * Thân mục dùng chung: dòng có thời gian, gạch đầu dòng, đoạn văn.
     * rightAligned = true: thời gian căn phải cùng dòng; false: xuống dòng dưới, màu nhấn.
     */
    private static void body(Flow flow, Section s, FontSet f, float size, Color ink, Color accent,
                             String sign, float indent, boolean rightAligned) throws IOException {
        for (Block b : s.blocks()) {
            if (b instanceof Entry e) {
                flow.gap(3);
                float rw = width(e.right(), f.regular(), size);
                boolean fits = width(e.left(), f.bold(), size + 0.5f) + rw + 18 < flow.w;
                if (rightAligned && fits) {
                    flow.need(size * 1.4f);
                    float base = flow.line(e.left(), f.bold(), size + 0.5f, ink, 0);
                    flow.c.text(flow.page, flow.x + flow.w - rw, base, e.right(), f.regular(), size, GRAY);
                } else {
                    flow.wrapped(e.left(), f.bold(), size + 0.5f, ink, 0);
                    if (!e.right().isEmpty()) flow.line(e.right(), f.regular(), size - 0.5f, rightAligned ? GRAY : accent, 0);
                }
            } else if (b instanceof Bullet bl) {
                flow.bullet(bl.text(), f.regular(), size, ink, accent, sign, indent);
            } else if (b instanceof Para p) {
                flow.wrapped(p.text(), f.regular(), size, ink, 0);
            }
        }
    }

    private static boolean sidebarSection(String title) {
        String t = title.toLowerCase();
        for (String k : List.of("kỹ năng", "skill", "học vấn", "education", "chứng chỉ", "certificate", "ngoại ngữ",
                "language", "liên hệ", "contact", "sở thích", "interest", "công cụ", "tools")) {
            if (t.contains(k)) return true;
        }
        return false;
    }

    /** Hai cột: cột trái có nền màu nhạt (liên hệ, kỹ năng, học vấn), cột phải là kinh nghiệm. */
    private static void sidebar(Doc d, Canvas c, FontSet f) throws IOException {
        Color accent = d.accent != null ? d.accent : new Color(15, 118, 110);
        Color bg = tint(accent, 0.9f);
        float side = 188;
        c.decorate = i -> c.fill(i, 0, 0, side, H, bg);

        // Tên đứng một mình ở trên cùng của cột phải, không có chữ nào cùng hàng
        float mx = side + 30;
        float mw = W - mx - 40;
        Flow main = new Flow(c, mx, mw, H - 46, H - 46, 46);
        main.line(d.name, f.bold(), 26, INK, 0);
        for (String r : d.roles) main.wrapped(r, f.regular(), 11.5f, accent, 0);
        main.gap(6);
        c.line(0, mx, main.y, mx + 36, main.y, accent, 2.2f);
        main.gap(6);

        float sideStart = main.y;
        if (d.image != null) {
            float dia = 118;
            float top = H - 40;
            c.photo(0, d.image, (side - dia) / 2, top - dia, dia, dia, true, Color.WHITE, 3.5f);
            sideStart = Math.min(sideStart, top - dia - 22);
        }
        Flow sideFlow = new Flow(c, 24, side - 44, sideStart, H - 46, 46);
        sideHeading(sideFlow, d.vietnamese ? "Liên hệ" : "Contact", f, accent);
        for (String item : d.contactItems()) sideFlow.wrapped(item, f.regular(), 8.8f, INK, 0);

        for (Section s : d.sections) {
            if (sidebarSection(s.title())) {
                sideFlow.gap(12);
                sideHeading(sideFlow, s.title(), f, accent);
                for (Block b : s.blocks()) {
                    if (b instanceof Entry e) {
                        sideFlow.gap(2);
                        sideFlow.wrapped(e.left(), f.bold(), 9, INK, 0);
                        if (!e.right().isEmpty()) sideFlow.line(e.right(), f.regular(), 8.5f, GRAY, 0);
                    } else if (b instanceof Bullet bl) {
                        sideFlow.bullet(bl.text(), f.regular(), 8.8f, INK, accent, "•", 9);
                    } else if (b instanceof Para p) {
                        sideFlow.wrapped(p.text(), f.regular(), 8.8f, INK, 0);
                    }
                }
            } else {
                main.gap(10);
                if (!s.title().isEmpty()) {
                    main.need(36);
                    main.line(s.title().toUpperCase(), f.bold(), 11, accent, 0);
                    main.gap(3);
                }
                body(main, s, f, 9.8f, INK, accent, "•", 10, false);
            }
        }
    }

    private static void sideHeading(Flow flow, String title, FontSet f, Color accent) throws IOException {
        flow.need(30);
        flow.line(title.toUpperCase(), f.bold(), 9.5f, accent, 0);
        flow.c.line(flow.page, flow.x, flow.y + 3, flow.x + flow.w, flow.y + 3, tint(accent, 0.6f), 0.6f);
        flow.gap(4);
    }

    /** Dải màu đậm ở đầu trang chứa tên và liên hệ, tiêu đề mục có ô vuông màu. */
    private static void banner(Doc d, Canvas c, FontSet f) throws IOException {
        Color accent = d.accent != null ? d.accent : new Color(30, 58, 138);
        float m = 50;
        // Tính chiều cao dải trước khi vẽ; có ảnh thì chữ trong dải hẹp lại
        float dia = 104;
        float headW = W - 2 * m - (d.image != null ? dia + 22 : 0);
        List<String> roleRows = new ArrayList<>();
        for (String r : d.roles) roleRows.addAll(wrap(r, f.regular(), 11, headW));
        List<String> contactRows = new ArrayList<>();
        for (String r : d.contacts) contactRows.addAll(wrap(r, f.regular(), 9, headW));
        float bandH = Math.max(40 + 30 + roleRows.size() * 15 + contactRows.size() * 13 + 18,
                d.image != null ? dia + 44 : 0);
        c.fill(0, 0, H - bandH, W, bandH, accent);
        if (d.image != null) {
            c.photo(0, d.image, W - m - dia, H - bandH / 2 - dia / 2, dia, dia, true, Color.WHITE, 3f);
        }

        Flow head = new Flow(c, m, headW, H - 38, H - 38, 40);
        head.line(d.name, f.bold(), 27, Color.WHITE, 0);
        head.gap(2);
        for (String r : roleRows) head.line(r, f.regular(), 11, tint(accent, 0.8f), 0);
        head.gap(3);
        for (String r : contactRows) head.line(r, f.regular(), 9, tint(accent, 0.7f), 0);

        Flow flow = new Flow(c, m, W - 2 * m, H - bandH - 22, H - 46, 46);
        for (Section s : d.sections) {
            if (!s.title().isEmpty()) {
                flow.need(36);
                float base = flow.line(s.title().toUpperCase(), f.bold(), 10.5f, accent, 14);
                c.fill(flow.page, m, base, 7, 7, accent);
                flow.gap(4);
            }
            body(flow, s, f, 9.4f, INK, accent, "•", 11, false);
            flow.gap(12);
        }
    }

    /** Kiểu truyền thống: Times New Roman, tên căn giữa, đường kẻ đen. */
    private static void classic(Doc d, Canvas c, FontSet f) throws IOException {
        float m = 60;
        Flow flow = new Flow(c, m, W - 2 * m, H - 52, H - m, m);
        String name = d.name.toUpperCase();
        float nameSize = 21;
        if (d.image != null) {
            // Kiểu CV Việt Nam: ảnh 3x4 có viền ở góc phải, thông tin căn trái bên cạnh
            float photoBottom = topRightPhoto(d, c, flow, W - m, H - 48, 104, false, Color.BLACK);
            flow.line(name, f.bold(), nameSize, Color.BLACK, 0);
            flow.gap(2);
            for (String r : d.roles) flow.wrapped(r, f.regular(), 11, INK, 0);
            for (String r : d.contacts) {
                for (String part : r.split("\\s*[·|]\\s*")) flow.wrapped(part, f.regular(), 10.5f, INK, 0);
            }
            endHeader(flow, W - 2 * m, photoBottom);
        } else {
            flow.need(nameSize * 1.4f);
            c.text(0, (W - width(name, f.bold(), nameSize)) / 2, flow.y - nameSize, name, f.bold(), nameSize, Color.BLACK);
            flow.gap(nameSize * 1.45f);
            for (String r : d.roles) centered(flow, r, f.regular(), 11);
            for (String r : d.contacts) centered(flow, r, f.regular(), 10.5f);
        }
        flow.gap(4);
        c.line(0, m, flow.y, W - m, flow.y, Color.BLACK, 1.2f);
        c.line(0, m, flow.y - 2.5f, W - m, flow.y - 2.5f, Color.BLACK, 0.4f);
        flow.gap(8);
        for (Section s : d.sections) {
            flow.gap(8);
            if (!s.title().isEmpty()) {
                flow.need(36);
                flow.line(s.title().toUpperCase(), f.bold(), 11.5f, Color.BLACK, 0);
                c.line(flow.page, m, flow.y + 3, W - m, flow.y + 3, Color.BLACK, 0.5f);
                flow.gap(4);
            }
            body(flow, s, f, 11, Color.BLACK, Color.BLACK, "–", 14, true);
        }
    }

    private static void centered(Flow flow, String s, PDFont f, float size) throws IOException {
        for (String row : wrap(s, f, size, flow.w)) {
            float lead = size * 1.38f;
            flow.need(lead);
            flow.c.text(flow.page, (W - width(row, f, size)) / 2, flow.y - size, row, f, size, INK);
            flow.y -= lead;
        }
    }

    /** Cột trái hẹp chứa tiêu đề mục và thời gian, cột phải chứa nội dung. */
    private static void timeline(Doc d, Canvas c, FontSet f) throws IOException {
        Color accent = d.accent != null ? d.accent : new Color(109, 40, 217);
        float m = 46;
        float leftW = 112;
        float gutter = 18;
        float cx = m + leftW + gutter;
        float cw = W - cx - m;

        Flow head = new Flow(c, m, W - 2 * m, H - 44, H - 44, m);
        float photoBottom = topRightPhoto(d, c, head, W - m, H - 40, 92, true, accent);
        head.line(d.name, f.bold(), 28, accent, 0);
        for (String r : d.roles) head.wrapped(r, f.regular(), 11.5f, INK, 0);
        for (String r : d.contacts) head.wrapped(r, f.regular(), 9.5f, GRAY, 0);
        endHeader(head, W - 2 * m, photoBottom);
        head.gap(8);
        c.line(0, m, head.y, W - m, head.y, accent, 1.4f);
        head.gap(14);

        Flow flow = new Flow(c, cx, cw, head.y, H - m, m);
        float rail = m + leftW + gutter / 2;
        for (Section s : d.sections) {
            flow.need(40);
            float sectionTop = flow.y;
            int sectionPage = flow.page;
            if (!s.title().isEmpty()) {
                for (String row : wrap(s.title().toUpperCase(), f.bold(), 9.8f, leftW)) {
                    c.text(sectionPage, m, sectionTop - 9.8f, row, f.bold(), 9.8f, accent);
                    sectionTop -= 9.8f * 1.35f;
                }
            }
            for (Block b : s.blocks()) {
                if (b instanceof Entry e) {
                    flow.gap(2);
                    flow.need(24);
                    if (!e.right().isEmpty()) {
                        // Thời gian ở cột trái, ngang hàng tiêu đề; nếu trùng chỗ tiêu đề mục thì đặt dưới
                        float y = Math.min(flow.y, flow.page == sectionPage ? sectionTop : flow.y);
                        int row = 0;
                        for (String part : wrap(e.right(), f.regular(), 9, leftW)) {
                            c.text(flow.page, m, y - 9 - row * 12, part, f.regular(), 9, GRAY);
                            row++;
                        }
                        if (flow.page == sectionPage) sectionTop = y - row * 12 - 4;
                    }
                    flow.wrapped(e.left(), f.bold(), 10.5f, INK, 0);
                } else if (b instanceof Bullet bl) {
                    flow.bullet(bl.text(), f.regular(), 10, INK, accent, "•", 10);
                } else if (b instanceof Para p) {
                    flow.wrapped(p.text(), f.regular(), 10, INK, 0);
                }
            }
            if (flow.page == sectionPage) flow.y = Math.min(flow.y, sectionTop);
            flow.gap(14);
        }
        // Đường dọc mảnh phân cách hai cột trên mỗi trang
        for (int i = 0; i <= flow.page; i++) {
            float topY = i == 0 ? head.y + 8 : H - m;
            c.line(i, rail, topY, rail, i == flow.page ? flow.y + 10 : m, LIGHT, 0.8f);
        }
    }

    /** Có chân: phông Constantia, dải màu dọc mép trái, tên chữ mảnh cỡ lớn. */
    private static void elegant(Doc d, Canvas c, FontSet f) throws IOException {
        Color accent = d.accent != null ? d.accent : new Color(146, 64, 14);
        c.decorate = i -> c.fill(i, 0, 0, 12, H, accent);
        float m = 62;
        Flow flow = new Flow(c, m, W - m - 50, H - 56, H - 56, 52);
        float photoBottom = Float.MAX_VALUE;
        if (d.image != null) {
            // Ảnh vuông có khung mảnh màu nhấn, lệch khỏi viền một chút
            float s = 96;
            float x = W - 50 - s;
            float y = H - 50 - s;
            c.photo(0, d.image, x, y, s, s, false, null, 0);
            c.line(0, x + 6, y - 6, x + s + 6, y - 6, accent, 0.8f);
            c.line(0, x + s + 6, y - 6, x + s + 6, y + s - 6, accent, 0.8f);
            flow.w -= s + 24;
            photoBottom = y - 6;
        }
        flow.line(d.name, f.regular(), 32, accent, 0);
        flow.gap(2);
        for (String r : d.roles) flow.wrapped(r, f.bold(), 11, INK, 0);
        for (String r : d.contacts) flow.wrapped(r, f.regular(), 9.5f, GRAY, 0);
        endHeader(flow, W - m - 50, photoBottom);
        for (Section s : d.sections) {
            flow.gap(14);
            if (!s.title().isEmpty()) {
                flow.need(36);
                String t = s.title().toUpperCase();
                float base = flow.line(t, f.bold(), 10.5f, INK, 0);
                float tw = width(t, f.bold(), 10.5f);
                c.line(flow.page, m + tw + 10, base + 3.5f, m + flow.w, base + 3.5f, tint(accent, 0.5f), 0.6f);
                flow.gap(3);
            }
            body(flow, s, f, 10, INK, accent, "·", 10, true);
        }
    }
}
