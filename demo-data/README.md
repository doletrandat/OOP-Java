# Dữ liệu demo

5 vị trí tuyển dụng ở các ngành khác nhau và 17 CV (tiếng Việt và tiếng Anh) để trình diễn CV Screener. Mỗi vị trí có ít nhất một ứng viên rất tốt, một ứng viên tầm trung và một CV làm qua loa. Vài vị trí có thêm ứng viên trái ngành.

Mọi email đều dùng tên miền `example.com`; tên người, công ty, bệnh viện và số điện thoại đều là hư cấu.

## Nạp vào hệ thống

Chạy server trước, rồi:

```powershell
pwsh demo-data/seed.ps1
```

Script đọc mật khẩu admin từ `ADMIN_PASSWORD` (biến môi trường hoặc `config.properties`). Muốn chỉ định khác thì thêm `-BaseUrl http://localhost:8080 -Password ...`. Vị trí nào đã có (trùng tên) sẽ được bỏ qua, nên chạy lại không bị nhân đôi.

Script không gửi họ tên ứng viên: server tự đọc tên, email và số điện thoại từ CV. Đây cũng là một phần để demo.

Khi bật AI, mỗi CV mất vài giây để chấm, nên cả bộ mất khoảng 1 đến 3 phút.

## Cấu trúc

```
demo-data/
  jobs.json                 5 vị trí (trường "folder" trỏ tới thư mục CV tương ứng)
  seed.ps1                  nạp vị trí và CV qua API
  resumes/<folder>/*.pdf    CV đã kết xuất, dùng để tải lên
```

## Sửa hoặc thêm CV

Các PDF được tạo bằng một công cụ riêng, không nằm trong repo này (thư mục `cv-demo-generator` cạnh dự án). Sửa nội dung CV ở đó rồi chạy `run.ps1` để tạo lại và chép PDF vào đây.

## Ứng viên

Điểm trong bảng là kết quả khi **bật AI**, chấm ngày 24/11/2026. Cột "Kỹ năng" là điểm kỹ năng sau khi ghép từ khoá với nhận định của AI: kỹ năng có bằng chứng sử dụng được đủ điểm, kỹ năng chỉ được liệt kê được nửa điểm. Điểm AI có thể lệch vài điểm giữa các lần chấm.

### Kế toán tổng hợp (tiếng Việt)

| CV | Mức | Điểm nổi bật | Kỹ năng | AI | Tổng |
| --- | --- | --- | --- | --- | --- |
| Phạm Thu Hà | Rất tốt | 6 năm, BCTC hợp nhất, thuế, MISA và SAP, chứng chỉ Kế toán trưởng, đang học ACCA | 100 | 89 | 96 |
| Nguyễn Minh Khoa | Tầm trung | Kế toán kho và thanh toán, chưa làm kế toán tổng hợp; Cao đẳng, đang học liên thông Đại học | 55 | 35 | 44 |
| Lê Thị Ngọc Anh | Trái ngành | Làm nhân sự (C&B), muốn chuyển sang kế toán | 18 | 13 | 12 |
| cv-hung | Qua loa | Không dấu, 6 tháng kinh nghiệm, Trung cấp, không có email | 18 | 12 | 12 |

Nguyễn Minh Khoa có câu "Hỗ trợ kế toán trưởng chuẩn bị số liệu": quy tắc từ khoá đọc ra "kế toán trưởng" và tưởng là chứng chỉ, AI sửa lại thành chưa có chứng chỉ và ghi rõ quy tắc đã đọc được gì.

### Digital Marketing Executive (tiếng Anh)

| CV | Mức | Điểm nổi bật | Kỹ năng | AI | Tổng |
| --- | --- | --- | --- | --- | --- |
| Sarah Nguyen | Rất tốt | 4 năm, quản lý ngân sách quảng cáo lớn, SEO tăng 180% traffic, IELTS 7.5 | 100 | 92 | 97 |
| Võ Thanh Tâm (CV tiếng Việt) | Tầm trung | Giỏi content và TikTok, chưa chạy Google Ads, TOEIC 560 | 62 | 38 | 52 |
| resume-tom | Qua loa | 6 dòng, sai chính tả, không có kinh nghiệm cụ thể | 19 | 10 | 12 |
| Đặng Quốc Việt | Trái ngành | Bếp phó khách sạn, CV trình bày tốt nhưng không liên quan | 0 | 8 | 7 |

### Điều dưỡng viên Khoa Nội (tiếng Việt)

| CV | Mức | Điểm nổi bật | Kỹ năng | AI | Tổng |
| --- | --- | --- | --- | --- | --- |
| Hoàng Thị Thu Trang | Rất tốt | Cử nhân ĐH Y Hà Nội, 5 năm Hồi sức, có chứng chỉ hành nghề, BLS và ACLS | 100 | 93 | 97 |
| Bùi Văn Nam | Mới tốt nghiệp | Chỉ có thực tập, **chưa có** chứng chỉ hành nghề nhưng CV có nhắc cụm từ này | 80 | 45 | 55 |
| cv-dieu-duong | Qua loa | 6 dòng, chỉ có tên "Mai" và số điện thoại | 0 | 12 | 8 |

Bùi Văn Nam có đủ kỹ năng cơ bản nhưng CV ghi "đang chờ … được cấp chứng chỉ hành nghề". Hệ thống đánh dấu tiêu chí này là "Chưa đạt (chưa có)", và AI giữ điểm dưới 60 vì thiếu một yêu cầu bắt buộc theo luật. Kinh nghiệm cũng được tính đúng: 6 tháng thực tập, không phải 1 năm.

### Frontend Developer (React) (tiếng Anh)

| CV | Mức | Điểm nổi bật | Kỹ năng | AI | Tổng |
| --- | --- | --- | --- | --- | --- |
| Alex Tran | Rất tốt | 6 năm, Next.js, design system, testing, accessibility | 100 | 94 | 98 |
| Đỗ Hoàng Long (CV tiếng Việt) | Tầm trung | 1 năm ReactJS, chưa dùng TypeScript | 79 | 58 | 62 |
| my-cv | Qua loa | Chỉ có tên "John", làm todo app theo YouTube | 29 | 12 | 16 |

### Nhân viên kinh doanh B2B (Logistics) (tiếng Việt)

| CV | Mức | Điểm nổi bật | Kỹ năng | AI | Tổng |
| --- | --- | --- | --- | --- | --- |
| Trương Gia Bảo | Rất tốt | 4 năm sales forwarding, doanh số 2,3 tỷ/tháng, Salesforce, TOEIC 785 | 100 | 90 | 96 |
| Kevin Le (CV tiếng Anh) | Tầm trung | Chăm sóc khách hàng ở hãng tàu, muốn chuyển sang sales, TOEIC 830 | 55 | 55 | 60 |
| cv-xin-viec-sales | Qua loa | Viết như đơn xin việc, không dấu, chưa làm logistics | 27 | 12 | 15 |

### Kỹ sư Java Backend (Middle/Senior) (tiếng Việt)

| CV | Mức | Điểm nổi bật | Kỹ năng | AI | Tổng |
| --- | --- | --- | --- | --- | --- |
| Lê Hoàng Minh | Rất tốt | 7 năm, lõi thanh toán, Kafka, microservices, Kubernetes, TOEIC 820 | 100 | 95 | 98 |
| Nguyễn Thị Phương Thảo | Tốt | 4 năm, Spring Boot, Kafka ở sàn thương mại điện tử | 88 | 85 | 89 |
| Trịnh Quang Huy | Trái stack | Senior .NET làm thanh toán, chưa viết Java | 72 | 52 | 65 |
| David Pham (CV tiếng Anh) | Tầm trung | 3 năm bảo trì monolith Spring MVC, chưa làm microservices | 53 | 55 | 60 |
| Hồ Văn Tài | Nhồi từ khoá | Liệt kê mọi công nghệ, thực tế chỉ thực tập 4 tháng và làm todo app | 75 | 25 | 46 |
| cv-java | Qua loa | 7 dòng không tên, đòi lương 40 triệu, liên hệ qua Zalo | 19 | 17 | 14 |

Hồ Văn Tài là ví dụ rõ nhất về CV nhồi từ khoá. Từ khoá khớp đủ 10/10 kỹ năng, nhưng AI thấy 6 kỹ năng (Kafka, Kubernetes, Redis...) chỉ nằm trong danh sách nên chúng hiện là "Chưa chắc" và chỉ được nửa điểm. CV tự ghi "3 năm kinh nghiệm (tính từ năm 2 đại học)": quy tắc đọc ra 3 năm, AI sửa thành khoảng 3 tháng thực tập và ghi chú lại để HR thấy. CV cv-java không ghi tên nên hệ thống hiển thị "Ứng viên".

### Chuyên viên Phân tích Dữ liệu

Vị trí này có sẵn trong app (không nằm trong `jobs.json`), nên `seed.ps1` không tải các CV này; tải tay vào vị trí Phân tích Dữ liệu. Thư mục `resumes/phan-tich-du-lieu` chứa:

- `tran-quoc-bao`, `pham-ngoc-linh`, `nguyen-minh-anh`: dựng lại từ 3 CV mẫu (`cv_phan_tich_du_lieu_*.pdf`) với nội dung giữ nguyên, mỗi CV một mẫu trình bày (timeline, banner, sidebar) để không còn giống nhau.
- `tran-dai-phat-ceo`: CV troll, xem bên dưới.

| CV | Mức | Kỹ năng | AI | Tổng |
| --- | --- | --- | --- | --- |
| Nguyễn Minh Anh | Rất tốt | 100 | 84 | 94 |
| Phạm Ngọc Linh | Tốt | 100 | 83 | 93 |
| Trần Quốc Bảo | Tốt | 100 | 73 | 89 |
| Trần Đại Phát | Troll | 50 | 3 | 21 |

### CV troll (vị trí Chuyên viên Phân tích Dữ liệu)

`resumes/phan-tich-du-lieu/tran-dai-phat-ceo.pdf`: Trần Đại Phát ứng tuyển làm Chủ tịch kiêm CEO, "hoặc Chuyên viên Phân tích Dữ liệu nếu công ty chưa sẵn sàng". Kinh nghiệm là trưởng nhóm Zalo gia đình, kỹ năng có "SQL (Siêu Quản Lý)" và "Power BI: hiểu rất rõ phần Power", đòi lương 500 triệu và không làm thứ Hai.

Từ khoá vẫn thấy SQL, Excel, Python, Power BI trong CV, nhưng AI nhận ra chúng chỉ được nhắc tới để đùa nên các kỹ năng này hiện là "Chưa chắc", AI chấm 3 điểm và điểm tổng chỉ còn 21.

## Gợi ý kịch bản demo

1. Chạy `seed.ps1` và mở trang Tổng quan: mục "Cần xem tiếp" cho thấy CV chưa xử lý ở từng vị trí.
2. Vào "Điều dưỡng viên Khoa Nội", bấm "Duyệt CV chưa xử lý", dùng phím `S`, `R`, `J` để duyệt nhanh.
3. Ở "Kỹ sư Java Backend", mở CV của Hồ Văn Tài, tab "Kỹ năng": từ khoá khớp đủ 10/10 nhưng AI đánh dấu 6 kỹ năng là "Chưa chắc" vì chỉ được liệt kê; tab "Tiêu chí" cho thấy AI sửa "3 năm" tự khai thành khoảng 3 tháng thực tập.
4. Ở "Digital Marketing Executive", lọc ứng viên và chọn nhiều dòng để từ chối hàng loạt các CV trái ngành.
