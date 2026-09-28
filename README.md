# Java 14 ngày - từ Frontend sang Junior Java Backend

Đây là lộ trình thực hành được cá nhân hóa từ CV của Đặng Quang Đạt và codebase
`SoftAIBox_UI`. Repo không chỉ chứa snippet: từ ngày 10, các phần ghép lại thành một
mini SoftAIBox backend có JWT, RBAC, REST, JPA, validation, pagination, async agent và
scheduler.

> Mục tiêu thực tế: sau 14 ngày học tập trung 4-6 giờ/ngày, bạn có thể đọc/viết Java
> core, giải thích kiến trúc Spring Boot cơ bản, demo một backend và trả lời phần lớn
> câu hỏi Junior. “Thành thạo” cần thêm vài tháng làm dự án và sửa lỗi thực tế.

## Bắt đầu nhanh

Máy hiện có Java 25 LTS. Kiểm tra:

```powershell
java -version
./mvnw.cmd -version
```

Chạy test và ứng dụng:

```powershell
./mvnw.cmd clean test
./mvnw.cmd spring-boot:run
```

Ứng dụng chạy tại `http://localhost:8080`, H2 console tại
`http://localhost:8080/h2-console` với JDBC URL trong `application.yml`.

Tài khoản học tập:

| Vai trò | Email | Mật khẩu |
|---|---|---|
| USER | `dat@softaibox.local` | `java123` |
| ADMIN | `admin@softaibox.local` | `admin123` |

Các tài khoản và JWT secret chỉ phục vụ local learning, tuyệt đối không dùng cho
production.

## Lộ trình

| Ngày | Chủ đề | Code chính | Kết quả cần đạt |
|---:|---|---|---|
| 1 | JVM, kiểu dữ liệu, biến, String | `learning/day01` | Giải thích JDK/JRE/JVM, primitive/reference |
| 2 | Điều kiện, vòng lặp, method | `learning/day02` | Tách logic thành method nhỏ, đọc stack frame |
| 3 | OOP, interface, record, enum | `learning/day03` | Dùng polymorphism thay cho `if/else` theo loại |
| 4 | Collections, generics, Big-O | `learning/day04` | Chọn đúng List/Set/Map, hiểu equals/hashCode |
| 5 | Exception, Optional, time, I/O | `learning/day05` | Xử lý lỗi và resource đúng cách |
| 6 | Lambda và Stream API | `learning/day06` | Viết pipeline filter/map/group/reduce dễ đọc |
| 7 | Concurrency, Future, virtual thread | `learning/day07` | Phân biệt I/O-bound và CPU-bound, tránh race condition |
| 8 | SOLID, Strategy, Factory, layers | `learning/day08` | Thiết kế code dễ thay đổi và test |
| 9 | Maven, JUnit, test design | `learning/day09` | Viết unit test AAA và test boundary |
| 10 | SQL, transaction, JPA/Hibernate | `conversation`, `chat` entities | Hiểu entity state, lazy loading, N+1, index |
| 11 | Spring IoC/DI và kiến trúc lớp | service/repository/controller | Theo được request từ HTTP đến database |
| 12 | REST, DTO, validation, errors | `common`, controllers | API contract sạch, status code và pagination đúng |
| 13 | Spring Security, JWT, RBAC, CORS | `auth`, `security` | Phân biệt authentication/authorization, access/refresh token |
| 14 | Async agent, scheduler, Docker, phỏng vấn | `agent` | Demo capstone và trình bày trade-off |

Đọc bài chi tiết tại [docs/LO-TRINH-14-NGAY.md](docs/LO-TRINH-14-NGAY.md), sau đó
dùng [docs/PHONG-VAN-JAVA-JUNIOR.md](docs/PHONG-VAN-JAVA-JUNIOR.md) để tự mock
interview. Phần liên hệ code frontend nằm ở
[docs/SOFTAIBOX-MAPPING.md](docs/SOFTAIBOX-MAPPING.md).

## Luồng API nên demo khi phỏng vấn

1. `POST /api/v1/auth/email/login` lấy access/refresh token.
2. Gửi `Authorization: Bearer <access-token>`.
3. `POST /api/v1/chat_conversations` tạo hội thoại.
4. `POST /api/v1/chat` tạo turn; backend cập nhật `turnCount` trong transaction.
5. `POST /api/v1/agent` rồi `POST /api/v1/agent/{id}/executions`.
6. Poll `GET /api/v1/agent/{id}/executions/{executionId}` như TanStack Query ở UI.
7. Dùng ADMIN gọi `GET /api/v1/admin/demo`; USER phải nhận `403`.

File [requests.http](requests.http) chứa request mẫu cho IntelliJ HTTP Client. Có thể
dùng Postman hoặc curl tương đương.

### Postman

Import collection và environment trong thư mục [postman](postman):

- `SoftAIBox_Java_Lab.postman_collection.json`
- `SoftAIBox_Local.postman_environment.json`

Collection gồm 22 request, tự động lưu token và ID giữa các bước, đồng thời kiểm tra
status code, response contract, validation và RBAC. Xem [hướng dẫn Postman](postman/README.md).

## Cấu trúc quan trọng

```text
src/main/java/vn/dangquangdat/javabegin
├── learning/       # ví dụ Java core ngày 1-9
├── auth/           # login, profile, access/refresh token
├── security/       # SecurityFilterChain, JWT decoder, RBAC, CORS
├── conversation/   # CRUD + pagination + optimistic locking
├── chat/           # transaction tạo turn và cập nhật conversation
├── agent/          # async execution + polling status + scheduler
└── common/         # response envelope, metadata, exception handler
```

## Nguồn phiên bản

- Java 25 là LTS hiện tại; bài core tránh các preview feature để kiến thức chuyển
  được sang Java 17/21.
- Spring Boot 4.1.1 yêu cầu tối thiểu Java 17 và hỗ trợ Java 25.
- Maven 3.9.16 là bản Maven 3 ổn định hiện tại.

Tham khảo: [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html),
[Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html),
[Apache Maven downloads](https://maven.apache.org/download.cgi).
