# CV Screener — Hệ thống quản lý CV hỗ trợ tuyển dụng

Ứng dụng Java thuần (OOP) với giao diện web, là **công cụ nội bộ dành cho HR**: HR tạo vị trí tuyển dụng,
tự tải CV PDF (do ứng viên gửi qua email/kênh khác) lên hệ thống, hệ thống đối chiếu CV với JD và
xếp hạng ứng viên. Điểm tổng kết hợp **ba nguồn đánh giá độc lập**:

1. **Khớp kỹ năng bằng từ khoá + từ điển đồng nghĩa Việt–Anh** (luôn chạy, không cần mạng).
2. **Tiêu chí bổ sung do HR cấu hình** (kinh nghiệm, trình độ, ngoại ngữ, chứng chỉ...).
3. **Phân tích ngữ nghĩa bằng AI** (tuỳ chọn, dùng DeepSeek).

> 🔒 Ứng viên **không trực tiếp nộp CV** trên hệ thống này. Mọi thao tác đều diễn ra trong trang
> quản trị `/admin` và yêu cầu đăng nhập.

> ⚖️ Điểm số chỉ mang tính **tham khảo**. AI không tự đưa ra quyết định tuyển dụng; HR là người
> quyết định mời phỏng vấn hay từ chối.

---

## 1. Tính năng

Hệ thống chỉ có một giao diện duy nhất: **trang quản trị nội bộ `/admin`**.

- Đăng nhập bằng mật khẩu quản trị.
- Tạo / sửa / đóng–mở / xoá vị trí tuyển dụng; nhập JD và danh sách kỹ năng
  (Bắt buộc = trọng số 2, Lợi thế = trọng số 1).
- Cấu hình **tiêu chí bổ sung** cho từng vị trí: số năm kinh nghiệm, trình độ đào tạo,
  chứng chỉ ngoại ngữ (TOEIC/IELTS/JLPT…), chứng chỉ nghề nghiệp, tiêu chí tự do — mỗi tiêu chí
  có trọng số 1–3. Hệ thống đối chiếu tự động với CV và hiển thị giá trị thực tế + câu trích dẫn.
- **Tải CV PDF lên trực tiếp** cho từng vị trí: nhập họ tên/email/điện thoại ứng viên,
  kéo–thả hoặc chọn file; hệ thống trích xuất và chấm điểm ngay.
- Xem danh sách ứng viên **xếp hạng theo điểm tổng**.
- Mở chi tiết hồ sơ: điểm tổng, điểm từ khoá, điểm tiêu chí bổ sung, điểm AI,
  từng kỹ năng/tiêu chí khớp hoặc chưa thấy, **câu trích dẫn nguyên văn làm bằng chứng**,
  nhận xét và cảnh báo của AI.
- Khu vực **Đối chiếu JD ↔ CV** trong hồ sơ: hai cột song song — bên trái là yêu cầu HR
  đưa ra (kỹ năng + trọng số, tiêu chí + yêu cầu), bên phải là kết quả quét CV tương ứng
  (trạng thái, giá trị thực tế, câu trích dẫn), xếp cùng hàng để so sánh trực tiếp.
  Kèm ô xem lại toàn văn JD và bộ đếm “x/y kỹ năng khớp · a/b tiêu chí đạt”.
- Xem file CV gốc (PDF) ngay trong ứng dụng: modal hiển thị **từng trang CV** dưới dạng ảnh
  (server kết xuất bằng PDFBox, xem được trên mọi trình duyệt kể cả khi trình duyệt chặn PDF nhúng),
  kèm nút **Tải PDF gốc**, chấm lại một hồ sơ hoặc **chấm lại toàn bộ** sau khi sửa JD.
- Đăng nhập bằng mật khẩu admin; token lưu trong `localStorage` và gửi qua header `X-Auth-Token`.
- Ghi nhận **quyết định của HR** (Chưa xử lý / Mời phỏng vấn / Từ chối) kèm ghi chú nội bộ.

**Xử lý CV**
- Trích xuất văn bản bằng Apache PDFBox, kiểm tra chữ ký `%PDF-` để chặn file giả mạo.
- Từ điển đồng nghĩa: `Database` ↔ `cơ sở dữ liệu`, `OOP` ↔ `lập trình hướng đối tượng`,
  `teamwork` ↔ `làm việc nhóm`, `JS` ↔ `JavaScript`…
- Chống prompt injection: nội dung CV được coi là dữ liệu, không phải chỉ dẫn cho AI.

---

## 2. Kiến trúc (hướng OOP)

```
src/main/java/cvrecruiter/
├── Main.java          # Composition root: tạo mọi đối tượng và tiêm phụ thuộc qua constructor
├── config/            # Record bất biến: AppConfig, AiSettings, ScoringWeights, StorageSettings
│                      #   + ConfigLoader (đọc config.properties, biến môi trường)
├── common/            # NotFoundException (→ HTTP 404), ValidationException (→ HTTP 400)
├── model/             # Thực thể có hành vi: JobPosting, Application, ContactInfo, SkillMatch,
│                      #   CriterionResult, MatchReport (Builder)...
├── repository/        # Interface Repository<T> + JsonFileRepository<T> + JobRepository, ApplicationRepository
├── storage/           # Interface CvStorage + LocalCvStorage (lưu file PDF)
├── parsing/           # CvTextExtractor (PDF → chữ), CvRenderer (PDF → ảnh trang)
├── matching/          # KeywordMatcher + RuleBasedKeywordMatcher + SynonymDictionary
├── criteria/          # CriterionRule (Strategy): ExperienceRule, EducationRule, LanguageRule, KeywordRule
│                      #   + CriteriaEvaluator (chọn quy tắc theo loại) + CriteriaMerger (ghép với AI)
├── scoring/           # ScoringService: ghép điểm kỹ năng, tiêu chí và AI
├── ai/                # AiAnalyzer (interface) ← NoAiAnalyzer, DeepSeekAiAnalyzer; AiAnalyzerFactory
├── service/           # JobService, ApplicationService
└── web/               # WebServer, Router, Controller (interface) ← AuthController, JobController,
                       #   ApplicationController, CvFileController, AiStatusController; dto/ (record)
```

Các nguyên lý OOP được áp dụng:

| Nguyên lý | Biểu hiện trong code |
|---|---|
| Đóng gói | Model không có setter. Muốn đổi trạng thái phải gọi phương thức nghiệp vụ: `JobPosting.edit/close`, `Application.decide`, `SkillMatch.applyAiVerdict`. Getter trả bản sao danh sách |
| Trừu tượng | Interface `Repository<T>`, `CvStorage`, `CvTextExtractor`, `CvRenderer`, `KeywordMatcher`, `AiAnalyzer`, `CriterionRule`, `Controller` |
| Kế thừa | Cây ngoại lệ: `CvExtractionException` → `ValidationException` → `IllegalArgumentException`; `NotFoundException`, `AuthException` kế thừa `RuntimeException` |
| Đa hình | `CriteriaEvaluator` gọi `rule.evaluate(...)` mà không cần biết loại tiêu chí; `ScoringService` gọi `aiAnalyzer.analyze(...)` dù đó là DeepSeek hay `NoAiAnalyzer` |
| Strategy | Mỗi loại tiêu chí là một `CriterionRule`; có AI hay không là hai `AiAnalyzer` khác nhau |
| Null Object | `NoAiAnalyzer` khi chưa cấu hình AI, không cần kiểm tra null |
| Factory | `AiAnalyzerFactory.create(settings)` |
| Builder | `MatchReport.builder()...build()` |
| Repository | `JsonFileRepository<T>` tổng quát, ghi file nguyên tử (tmp + atomic move) |
| Dependency injection | `Main` tạo đối tượng và truyền vào constructor; lớp nghiệp vụ chỉ phụ thuộc interface |

---

## 3. Chấm điểm như thế nào?

```
Điểm kỹ năng   = Σ trọng số kỹ năng tìm thấy / Σ trọng số tất cả kỹ năng × 100
Điểm tiêu chí  = Σ (trọng số tiêu chí × mức đạt) / Σ trọng số tiêu chí × 100
                 mức đạt: Đạt = 1.0 · Một phần = 0.5 · Chưa đạt / Chưa rõ = 0
Điểm quy tắc   = Điểm kỹ năng                                   (vị trí không có tiêu chí)
               = (1 − CRITERIA_WEIGHT) × Điểm kỹ năng
                 + CRITERIA_WEIGHT × Điểm tiêu chí              (vị trí có tiêu chí)
Điểm tổng      = (1 − AI_WEIGHT) × Điểm quy tắc + AI_WEIGHT × Điểm AI
```

- Mặc định `AI_WEIGHT = 0.4` (AI chiếm 40%), `CRITERIA_WEIGHT = 0.35`
  (trong phần quy tắc: 65% kỹ năng + 35% tiêu chí bổ sung).
- **Năm loại tiêu chí** (`CriterionType`):

  | Loại | Cách đối chiếu | Ví dụ yêu cầu |
  |---|---|---|
  | `EXPERIENCE` | Đọc số năm kinh nghiệm trong CV (`2 năm`, `3+ years`, `1,5 năm`) và so với mức tối thiểu | `2` |
  | `EDUCATION` | Xếp hạng trình độ theo thang Trung cấp/THPT → Cao đẳng → Đại học → Thạc sĩ → Tiến sĩ | `Đại học` |
  | `LANGUAGE` | Đọc điểm chứng chỉ ngoại ngữ (TOEIC, IELTS, TOEFL, HSK, JLPT, TOPIK) | `TOEIC 600`, `IELTS 6.5` |
  | `CERTIFICATION` | Khớp từ khoá + từ điển đồng nghĩa, hỗ trợ nhiều phương án | `AWS Certified, Oracle OCP` |
  | `OTHER` | Khớp từ khoá tự do | `Kinh nghiệm làm việc nhóm` |

- Trạng thái tiêu chí: `MET` (đạt), `PARTIAL` (một phần, ví dụ thiếu ≤ 0.5 năm kinh nghiệm),
  `NOT_MET` (chưa đạt), `UNKNOWN` (CV không nêu thông tin → không đoán).
- Việc chấm tiêu chí chạy **rule-based** trước (không cần API key), sau đó `CriteriaMerger`
  ghép với nhận định của AI:
  - AI trả "chưa rõ" hoặc không nêu căn cứ: giữ kết quả quy tắc.
  - AI đồng ý với quy tắc: giữ nguyên, ghi nguồn là cả hai.
  - Kinh nghiệm, trình độ, chứng chỉ, tiêu chí khác: AI có căn cứ thì kết luận của AI được dùng,
    kể cả khi thấp hơn quy tắc (ví dụ "3 năm" nhưng thực ra tính từ năm 2 đại học). Muốn hạ
    kết quả, AI phải nêu được giá trị thực tế hoặc câu trích. Ghi chú nói rõ quy tắc đã đọc được gì.
  - Ngoại ngữ: quy tắc đã đọc được điểm chứng chỉ (TOEIC 650...) thì giữ phép so sánh số đó.
    Quy tắc không thấy chứng chỉ thì AI được nâng nếu có căn cứ, ví dụ làm việc 4 năm tại Mỹ,
    CV viết bằng tiếng Anh → "Tiếng Anh" **Đạt**, gắn nhãn **"AI suy ra"**.
- **AI đánh giá đa chiều**, không chỉ đếm từ khoá trùng JD. Prompt yêu cầu chấm 6 nhóm:
  kỹ năng chuyên môn, kinh nghiệm & thâm niên, trình độ đào tạo, ngoại ngữ, chứng chỉ &
  bổ sung, yếu tố khác/rủi ro. Điểm từng nhóm được hiển thị trong hồ sơ.
  Thang điểm gợi ý: kỹ năng 40% · kinh nghiệm 25% · trình độ 10% · ngoại ngữ 10% · bổ sung 15%,
  tự phân bổ lại nếu vị trí không yêu cầu một nhóm nào.
- AI trả về JSON có schema cố định: điểm tổng quan, điểm từng nhóm, điểm mạnh, điểm cần làm rõ,
  trạng thái từng kỹ năng (`MATCHED` / `UNCERTAIN` / `MISSING`) và từng tiêu chí bổ sung
  (`criteria`) kèm trích dẫn + cờ `inferred`.
- Bằng chứng hiển thị cho HR ưu tiên câu trích **nguyên văn** từ CV; nếu AI không tìm thấy
  bằng chứng thì để trống thay vì bịa.
- Nếu chưa cấu hình AI hoặc API lỗi, hệ thống **vẫn hoạt động**: điểm tổng = điểm quy tắc,
  trạng thái AI hiển thị rõ là `Chưa cấu hình` / `Lỗi AI`.

---

## 4. Chạy ứng dụng

**Yêu cầu:** JDK 21 trở lên (đã kiểm tra với JDK 25). Maven không cần cài sẵn — dùng `mvnw`.

```powershell
# 1. Tạo cấu hình (bỏ qua nếu đã có config.properties)
Copy-Item config.properties.example config.properties
#    rồi mở file và điền khoá AI (AI_API_KEY)

# 2. Build + chạy test
.\mvnw.cmd package

# 3. Khởi động
java -jar target\cv-recruiter.jar
```

Hoặc chạy nhanh bằng script:

```powershell
.\run.ps1
```

Mở: http://localhost:8080 — tự chuyển hướng về trang quản trị `/admin`.
Mật khẩu mặc định `admin123` (đổi bằng `ADMIN_PASSWORD` trong `config.properties` hoặc biến môi trường).

### Cấu hình

Mọi biến đều có thể đặt trong `config.properties` hoặc biến môi trường (env được ưu tiên):

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `PORT` | `8080` | Cổng HTTP |
| `AI_API_KEY` | *(trống)* | Khoá API DeepSeek. Để trống thì tắt AI, chỉ chấm bằng quy tắc |
| `AI_MODEL` | `deepseek-chat` | Model DeepSeek dùng để phân tích |
| `AI_TIMEOUT_SECONDS` | `60` | Thời gian chờ tối đa cho một lần gọi AI |
| `ADMIN_PASSWORD` | `admin123` | Mật khẩu trang quản trị |
| `AI_WEIGHT` | `0.4` | Trọng số điểm AI trong điểm tổng (0–1) |
| `CRITERIA_WEIGHT` | `0.35` | Tỉ lệ điểm tiêu chí bổ sung trong phần điểm quy tắc (chỉ áp dụng khi vị trí có tiêu chí) |
| `DATA_DIR` | `data` | Nơi lưu `jobs.json`, `applications.json` |
| `UPLOADS_DIR` | `uploads` | Nơi lưu CV PDF |
| `MAX_UPLOAD_MB` | `15` | Giới hạn dung lượng CV |

`config.properties` chứa khoá bí mật và đã nằm trong `.gitignore` — **không commit file này**.

---

## 5. Thử luồng nghiệp vụ với CV mẫu

Thư mục `sample-cvs/` có ba CV PDF:

| File | Mô tả |
|---|---|
| `cv_strong_backend.pdf` | Ứng viên backend mạnh (Java, SQL, Spring Boot, Docker) |
| `cv_mid_support.pdf` | Ứng viên mức trung bình, thiên về hỗ trợ kỹ thuật |
| `cv_weak_marketing.pdf` | CV marketing, lệch JD backend |

Các bước demo:

1. Vào `/admin` → đăng nhập → mở vị trí **Lập trình viên Java Backend**.
2. Bấm **Tải CV lên** và chọn ba CV PDF ở trên. Hệ thống tự đọc tên, email, số điện thoại
   và chấm điểm từng CV.
3. Quan sát bảng xếp hạng: điểm tổng, điểm từ khoá, điểm AI, số kỹ năng khớp, số tiêu chí đạt.
4. Bấm vào một dòng để xem kỹ năng/tiêu chí khớp, câu trích dẫn bằng chứng và nhận xét AI.
5. Sửa JD, kỹ năng hoặc **tiêu chí bổ sung** → bấm **Chấm lại tất cả** để cập nhật xếp hạng.
6. Đặt quyết định **Mời phỏng vấn / Từ chối** kèm ghi chú — đây là quyết định của HR.

---

## 6. API chính

Toàn bộ API đều là nội bộ, yêu cầu header `X-Auth-Token` lấy từ `/api/auth/login`
(trừ chính endpoint đăng nhập).

| Method | Đường dẫn | Mô tả |
|---|---|---|
| `POST` | `/api/auth/login` | Đăng nhập HR, trả về token |
| `POST` | `/api/auth/logout` | Đăng xuất (huỷ token) |
| `GET` | `/api/admin/jobs` | Danh sách tất cả vị trí + số hồ sơ |
| `GET` | `/api/admin/jobs/{id}` | Chi tiết một vị trí |
| `POST` | `/api/admin/jobs` | Tạo vị trí |
| `PUT` | `/api/admin/jobs/{id}` | Sửa vị trí / đổi trạng thái |
| `DELETE` | `/api/admin/jobs/{id}` | Xoá vị trí, hồ sơ và file CV liên quan |
| `POST` | `/api/admin/jobs/{id}/cv` | HR tải CV lên (multipart: `file`; `name`, `email`, `phone` không bắt buộc) |
| `GET` | `/api/admin/jobs/{id}/applications` | Hồ sơ của vị trí, đã xếp theo điểm |
| `POST` | `/api/admin/jobs/{id}/rescore-all` | Chấm lại toàn bộ hồ sơ của vị trí |
| `GET` | `/api/admin/applications/{id}` | Chi tiết báo cáo chấm điểm |
| `POST` | `/api/admin/applications/{id}/rescore` | Chấm lại một hồ sơ |
| `POST` | `/api/admin/applications/{id}/decision` | Lưu quyết định và ghi chú của HR |
| `PUT` | `/api/admin/applications/{id}` | Sửa họ tên, email, số điện thoại |
| `DELETE` | `/api/admin/applications/{id}` | Xoá hồ sơ và file CV |
| `GET` | `/api/admin/applications/{id}/cv` | Tải CV PDF gốc |
| `GET` | `/api/admin/applications/{id}/cv/info` | Số trang, tên file, dung lượng CV |
| `GET` | `/api/admin/applications/{id}/cv/pages/{page}` | Ảnh PNG của một trang CV (để xem trong ứng dụng) |
| `GET` | `/api/admin/ai-status` | Trạng thái cấu hình AI và trọng số |

---

## 7. Kiểm thử

```powershell
.\mvnw.cmd test
```

18 unit test bao phủ: chuẩn hoá tiếng Việt, từ điển đồng nghĩa, khớp từ khoá có bằng chứng,
ranh giới từ (`JS` không khớp trong `JSON`), công thức điểm, ghép điểm AI, kiểm tra file PDF giả
và hồi quy lỗi đọc JSON (đảm bảo repository trả về đúng kiểu `JobPosting`/`Application` sau khi
khởi động lại, không phải `LinkedTreeMap`).

---

## 8. Ghi chú đạo đức & dữ liệu

- Chỉ HR có quyền truy cập hệ thống; ứng viên không tự nộp CV.
- Hệ thống **không tự động loại** ứng viên. Mọi quyết định đều do HR bấm chọn.
- Điểm AI có thể sai lệch; giao diện luôn hiển thị cảnh báo và bằng chứng để HR kiểm chứng.
- CV là dữ liệu cá nhân: chỉ lưu cục bộ trong `uploads/`, không gửi đi đâu ngoài nội dung văn bản
  gửi tới DeepSeek khi bật AI.
- Khi triển khai thật: đổi mật khẩu admin, dùng HTTPS, thêm phân quyền nhiều tài khoản HR,
  và có chính sách lưu trữ/xoá dữ liệu ứng viên.
