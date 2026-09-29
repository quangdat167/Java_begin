# Trung tâm học Java trong 14 ngày

Thư mục này chứa tài liệu học theo từng ngày. Mỗi README được thiết kế để đọc theo
thứ tự: mục tiêu → lý thuyết → code trong project → lỗi thường gặp → bài tập → câu
hỏi phỏng vấn → Definition of Done.

| Ngày | Chủ đề | Tài liệu | Code bắt đầu |
|---:|---|---|---|
| 1 | JVM, kiểu dữ liệu, bộ nhớ | [README](./day01/README.md) | [BasicsDemo](./day01/BasicsDemo.java) |
| 2 | Control flow, method, scope, array | [README](./day02/README.md) | [ControlFlowDemo](./day02/ControlFlowDemo.java) |
| 3 | OOP, interface, record, enum | [README](./day03/README.md) | [OopDemo](./day03/OopDemo.java) |
| 4 | Collections, generics, Big-O | [README](./day04/README.md) | [CollectionsDemo](./day04/CollectionsDemo.java) |
| 5 | Exception, Optional, time, file I/O | [README](./day05/README.md) | [ExceptionsAndIoDemo](./day05/ExceptionsAndIoDemo.java) |
| 6 | Lambda và Stream API | [README](./day06/README.md) | [StreamsDemo](./day06/StreamsDemo.java) |
| 7 | Concurrency và virtual thread | [README](./day07/README.md) | [ConcurrencyDemo](./day07/ConcurrencyDemo.java) |
| 8 | SOLID và design patterns | [README](./day08/README.md) | [DesignPatternsDemo](./day08/DesignPatternsDemo.java) |
| 9 | Maven, JUnit và test design | [README](./day09/README.md) | [PromotionService](./day09/PromotionService.java) |
| 10 | SQL, transaction, JPA/Hibernate | [README](./day10/README.md) | [Conversation entity](../conversation/Conversation.java) |
| 11 | Spring IoC/DI và request lifecycle | [README](./day11/README.md) | [Conversation service](../conversation/ConversationService.java) |
| 12 | REST, DTO, validation và errors | [README](./day12/README.md) | [GlobalExceptionHandler](../common/GlobalExceptionHandler.java) |
| 13 | Spring Security, JWT, RBAC và CORS | [README](./day13/README.md) | [SecurityConfig](../security/SecurityConfig.java) |
| 14 | Async agent, scheduler và production | [README](./day14/README.md) | [AgentController](../agent/AgentController.java) |

## Cách học đề xuất

1. Đọc README của ngày hiện tại và tự nói lại từng khái niệm.
2. Chạy code mẫu, đoán kết quả trước khi nhìn terminal.
3. Gõ lại ví dụ bằng tay và thay đổi input để tạo cả happy path lẫn lỗi.
4. Làm bài tập mà chưa nhìn lời giải trong ít nhất 30 phút.
5. Trả lời câu hỏi phỏng vấn thành tiếng.
6. Chỉ chuyển ngày khi đạt Definition of Done của ngày hiện tại.

Không cần thuộc mọi API. Mục tiêu là hiểu dữ liệu đi đâu, object nào chịu trách nhiệm,
lỗi được xử lý ở đâu và quyết định thiết kế có trade-off gì.
