# Bản đồ từ SoftAIBox UI sang Java/Spring Boot

Tài liệu này nối trực tiếp kinh nghiệm trong CV và code frontend hiện có với kiến thức
backend Java. Mục tiêu là khi phỏng vấn, bạn không nói “em chỉ mới học Java” mà nói
“em đã hiểu cùng domain từ hai phía và có thể chỉ ra contract, transaction, security và
concurrency nằm ở đâu”.

## 1. API client và controller

Frontend có generic `apiGet/apiPost/apiPut/apiDelete` trong
`SoftAIBox_UI/src/services/api.ts`. Backend tương ứng là `@RestController` + DTO:

```text
React hook -> function API -> Axios -> SecurityFilterChain
           -> Controller -> Service (@Transactional) -> Repository -> H2/PostgreSQL
```

`ApiResponse<T>` trong Java giữ cùng envelope `code`, `message`, `data`, `meta` với
`BaseResponseType` của frontend. Đây là contract; đổi tên field tùy ý ở entity không
nên làm frontend vỡ.

Kỹ thuật cần học:

- Jackson serialize record/DTO thành JSON.
- Bean Validation tại boundary; domain validation trong service/entity.
- HTTP status đúng và error envelope thống nhất.
- OpenAPI/contract test nếu hai team phát triển độc lập.

## 2. Zod/React Hook Form và Bean Validation

Frontend login/register dùng Zod để báo lỗi sớm. Java dùng `@Email`, `@NotBlank`,
`@Size` trên request record. Hai lớp validation không thay nhau:

- Client validation: UX, phản hồi tức thời, có thể bị bypass.
- Server validation: security/data integrity boundary, luôn bắt buộc.
- Database constraint: lớp cuối chống race condition giữa nhiều request.

Ví dụ “email duy nhất” phải có unique index ở DB; chỉ query `existsByEmail` rồi insert
vẫn có race condition.

## 3. Axios interceptor và Spring Security

`src/services/http.ts` gắn access token, nhận 401, chỉ cho một refresh chạy rồi queue
các request thất bại. Backend mini hỗ trợ đúng luồng:

- `POST /api/v1/auth/email/login`
- `POST /api/v1/auth/refresh` với refresh token trong Bearer header
- `GET /api/v1/auth/me`
- Access token chứa subject, role, expiry; SecurityFilterChain kiểm signature.

Điểm phỏng vấn quan trọng:

- Queue refresh ở frontend ngăn “refresh storm”, nhưng backend vẫn phải rotate/revoke
  refresh token và xử lý reuse.
- `PrivateRoute`/`AdminRoute` chỉ ẩn UI. `@PreAuthorize` hoặc URL rule ở backend mới là
  authorization thực.
- LocalStorage token chịu rủi ro XSS. HttpOnly cookie giảm token theft nhưng cần CSRF,
  SameSite và CORS thiết kế đúng.
- Demo đang dùng symmetric HS256 cho dễ học. Hệ thống nhiều service thường cân nhắc
  asymmetric key để resource server chỉ giữ public key.

## 4. TanStack Query và semantics của backend

Frontend dùng query key, invalidation, infinite query, `setQueryData` và polling. Backend
phải tạo semantics ổn định để client cache đúng:

| Frontend | Backend Java cần bảo đảm |
|---|---|
| `invalidateQueries(["agents"])` | Mutation commit xong mới trả success |
| `useInfiniteQuery` | Sort ổn định, cursor hoặc page metadata đúng |
| `setQueryData` optimistic | Version/ETag hoặc conflict handling khi nhiều client sửa |
| `refetchInterval: 2000` | Status endpoint rẻ, index tốt, rate limit hợp lý |
| retry của query | GET idempotent; POST có idempotency key nếu retry được |

Mini backend mô phỏng polling agent bằng `executionId`. State dùng `ConcurrentHashMap`
chỉ phù hợp demo; production phải lưu bền hoặc dùng queue/job store để không mất khi
restart và chia sẻ giữa replicas.

## 5. Chat conversation và transaction

Frontend có `chat_conversations`, create/rename/delete, send chat và status polling.
Backend mini tách:

- `ConversationController`: HTTP/DTO.
- `ConversationService`: ownership, transaction và use case.
- `ConversationRepository`: query/pagination.
- `ChatService`: tạo turn và tăng `turnCount`.

Điểm cần suy nghĩ:

- Tạo turn và tăng count nên atomic. Nếu call model chậm, không giữ DB transaction mở
  trong suốt cuộc gọi mạng; tạo job/pending state, commit, gọi model ngoài transaction,
  rồi transaction ngắn để hoàn tất.
- UI hiện poll trạng thái mỗi 2 giây. Với tải lớn, SSE phù hợp stream token một chiều;
  WebSocket chỉ cần khi có tương tác hai chiều liên tục.
- Chat response có thể chứa Markdown/code. Backend không nên tin HTML do model trả về;
  frontend đã dùng sanitizer nhưng vẫn cần content-security policy và output handling.

## 6. Agent orchestration và scheduling

CV nêu dashboard agent, automated scheduling và notification. Backend mapping:

| Domain | Spring/Java |
|---|---|
| Create/update agent | REST DTO + validation + JPA |
| Execute now | `202 Accepted`, async worker, execution state |
| Schedule daily/hourly | `@Scheduled` cho demo; Quartz/external scheduler cho production |
| Many blocking model calls | virtual threads hoặc bounded executor |
| Notification | Strategy/Factory + event/outbox |
| History infinite scroll | keyset pagination theo `(createdAt,id)` |

`@Scheduled` trong ba replicas sẽ chạy ba lần. Cần distributed lock, leader election hoặc
queue/scheduler bên ngoài. Retry phải phân loại transient/permanent và dùng backoff;
notification/payment cần idempotency.

## 7. RBAC và ownership

CV nhấn mạnh RBAC ở SoftAIBox và dự án fintech. Cần phân biệt:

- Role coarse-grained: USER, ADMIN.
- Permission fine-grained: AGENT_READ, AGENT_EXECUTE, PAYMENT_MANAGE.
- Ownership/data scope: USER chỉ đọc conversation của chính mình.

Chỉ `hasRole('USER')` chưa đủ: user A vẫn có thể đoán ID của user B. Repository trong
backend mini dùng `findByIdAndOwnerEmail`, tránh kiểm quyền sau khi đã load dữ liệu.
Production có thể dùng policy service cho rule phức tạp và audit log cho hành động admin.

## 8. Stripe/payment

Frontend quản lý plan/package/promotion, checkout và cancel subscription. Java backend
cần đặc biệt chú ý:

- Tiền dùng `BigDecimal` + currency; không dùng `double`.
- Không tin price/discount từ client; server lookup package/promotion.
- Checkout dùng idempotency key để double click/retry không tạo nhiều phiên.
- Webhook phải verify signature, ghi event ID unique, xử lý idempotent và trả nhanh.
- Subscription state thay đổi qua webhook; không đánh dấu paid chỉ vì browser quay về
  success URL.
- Transaction DB không bao trùm Stripe. Dùng state machine/outbox/saga tùy độ phức tạp.

`PromotionService` ngày 9 là điểm khởi đầu để test rounding và boundary 0-100%.

## 9. Data source và upload file

Frontend gửi `fetch_function`, execute data source và upload PDF/image/document. Rủi ro
backend đáng nói trong phỏng vấn:

- Không chạy code người dùng trực tiếp trong application process. Cần sandbox/container,
  allowlist network, timeout, CPU/memory quota và audit.
- Upload cần size/type/magic-byte validation, random storage key, malware scan, signed
  URL và ownership check.
- Không ghép filename người dùng vào filesystem path.
- Job parse/embed document nên async; trả upload ID/job ID và status.

## 10. MongoDB kinh nghiệm cũ sang JPA/SQL

Kinh nghiệm Node.js/MongoDB vẫn hữu ích nhưng cần đổi mental model:

- Mongo document thường aggregate dữ liệu; SQL normalize và join.
- Mongo schema flexible không có nghĩa không cần schema; SQL constraint mạnh hơn.
- JPA persistence context/dirty checking khác Mongoose document save.
- Transaction, index và query plan đều phải học ở cả hai hệ.
- Chọn database theo access pattern/consistency, không theo độ quen thuộc.

## 11. Docker/Nginx và production readiness

Frontend có Docker/Nginx. Java thêm các điểm:

- Multi-stage build tạo executable JAR, runtime image không cần Maven/source.
- JVM container memory cần quan sát heap/native memory; đặt resource limits và test OOM.
- Health/readiness không đồng nghĩa: process sống nhưng DB down thì readiness nên fail.
- Config/secret qua environment hoặc secret manager; không commit JWT/DB/Stripe secret.
- Graceful shutdown để request/job đang chạy có cơ hội hoàn tất.

## Cách kể dự án trong phỏng vấn

Dùng cấu trúc Problem - Decision - Trade-off - Result:

> “Ở SoftAIBox, frontend có interceptor queue các request khi access token hết hạn. Khi
> học Java, em xây backend tương ứng bằng Spring Security resource server và refresh
> token rotation. Em hiểu UI route guard không đủ cho RBAC, nên endpoint admin dùng
> method security và query conversation luôn kèm owner. Demo lưu refresh token/job state
> in-memory để tập trung vào flow; production em sẽ lưu token hash và execution trong
> database/queue để hỗ trợ revoke, restart và nhiều replica.”

Câu trả lời này tận dụng kinh nghiệm thật, phân biệt rõ demo/production và cho thấy bạn
hiểu cả hệ thống chứ không chỉ thuộc annotation.
