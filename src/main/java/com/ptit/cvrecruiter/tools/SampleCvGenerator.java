package com.ptit.cvrecruiter.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Công cụ tạo CV PDF mẫu để kiểm thử hệ thống (dùng chung PDFBox).
 * Chạy: java -cp cv-recruiter.jar com.ptit.cvrecruiter.tools.SampleCvGenerator <thuMucXuat>
 */
public final class SampleCvGenerator {

    private SampleCvGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "sample-cvs");
        Files.createDirectories(out);

        write(out.resolve("cv_strong_backend.pdf"), List.of(
                "NGUYEN VAN BINH",
                "Email: binh.nguyen@example.com | Phone: 0901234567",
                "",
                "SUMMARY",
                "Backend developer with 2 years of experience building REST API services in Java.",
                "",
                "SKILLS",
                "- Programming: Java, OOP, design patterns, data structures and algorithms",
                "- Framework: Spring Boot, Spring MVC, Spring Data JPA",
                "- Database: MySQL, PostgreSQL, Redis; strong SQL query tuning; co so du lieu design",
                "- DevOps: Docker, basic Kubernetes, Git, GitHub Actions CI/CD",
                "- API: REST API design, JSON, microservices",
                "- Soft skills: teamwork, communication, problem solving",
                "- English: TOEIC 780, read technical documentation fluently",
                "",
                "EXPERIENCE",
                "ABC Software Company - Java Developer (2024 - present)",
                "- Built and maintained 10+ REST API endpoints with Spring Boot for an e-commerce system",
                "- Designed MySQL database schema and optimized SQL queries, reduced response time 40%",
                "- Containerized services with Docker and deployed to Linux servers",
                "- Collaborated with a 6-person team using Git and Agile/Scrum process",
                "",
                "EDUCATION",
                "Posts and Telecommunications Institute of Technology - Information Technology",
                "GPA 3.4/4.0"));

        write(out.resolve("cv_mid_support.pdf"), List.of(
                "TRAN THI MAI",
                "Email: mai.tran@example.com | Phone: 0912345678",
                "",
                "SUMMARY",
                "Junior developer with 1 year of PHP/Laravel experience, currently learning Java.",
                "",
                "SKILLS",
                "- Languages: PHP, a little Java from university coursework",
                "- Framework: Laravel, basic Spring knowledge",
                "- Database: MySQL; understand co so du lieu design, basic SQL joins",
                "- Tools: Git, Composer, Postman for API testing",
                "- Others: html, css, basic javascript",
                "- Soft skills: teamwork, lam viec nhom, customer communication",
                "",
                "EXPERIENCE",
                "XYZ Digital - Web Developer (2025 - present)",
                "- Developed company website features with Laravel and MySQL",
                "- Wrote REST API endpoints consumed by the mobile app team",
                "- Version control with Git, code review in team",
                "",
                "EDUCATION",
                "University of Technology - Software Engineering",
                "IELTS 5.5"));

        write(out.resolve("cv_weak_marketing.pdf"), List.of(
                "LE VAN CUONG",
                "Email: cuong.le@example.com | Phone: 0934567890",
                "",
                "SUMMARY",
                "Marketing specialist with 3 years of experience in digital campaigns.",
                "",
                "SKILLS",
                "- Digital marketing, Facebook Ads, Google Ads",
                "- Content writing, SEO optimization",
                "- Data analysis with Excel, basic reporting",
                "- Soft skills: presentation, communication",
                "",
                "EXPERIENCE",
                "Media House - Marketing Executive (2023 - present)",
                "- Managed social media campaigns with monthly budget 200 million VND",
                "- Grew page followers by 300% in one year",
                "",
                "EDUCATION",
                "National Economics University - Marketing"));

        System.out.println("Da tao CV mau tai: " + out.toAbsolutePath());
    }

    private static void write(Path file, List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 800;
                for (String line : lines) {
                    boolean heading = line.equals(line.toUpperCase()) && !line.isBlank()
                            && line.length() < 40;
                    cs.beginText();
                    cs.setFont(heading ? bold : regular, heading ? 12 : 10);
                    cs.newLineAtOffset(50, y);
                    cs.showText(line);
                    cs.endText();
                    y -= 15;
                }
            }
            doc.save(file.toFile());
        }
    }
}
