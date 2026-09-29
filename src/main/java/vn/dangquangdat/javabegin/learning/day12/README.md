# Ngày 12 — REST, DTO, validation, pagination và error contract

Ngày 12 tập trung vào ranh giới giữa frontend và backend. Một API tốt không chỉ “trả được JSON”; nó phải có resource rõ ràng, status đúng nghĩa, DTO ổn định, validation nhiều lớp, pagination có giới hạn và error contract để mọi client xử lý nhất quán.

Code chính:

- [ConversationController.java](../../conversation/ConversationController.java): CRUD, request DTO và status.
- [ChatController.java](../../chat/ChatController.java): JSON naming, chat/history/status.
- [AgentController.java](../../agent/AgentController.java): pagination và <code>202 Accepted</code>.
- [ApiResponse.java](../../common/ApiResponse.java): response envelope.
- [ApiMeta.java](../../common/ApiMeta.java): metadata phân trang.
- [GlobalExceptionHandler.java](../../common/GlobalExceptionHandler.java): chuyển exception thành HTTP error.
- [NotFoundException.java](../../common/NotFoundException.java): domain/application error có ý nghĩa.

---

## Mục tiêu

Sau ngày 12, bạn cần giải thích và thực hành được:

1. Resource-oriented URL và ý nghĩa của các HTTP method.
2. Safe khác idempotent.
3. Khi nào dùng 200, 201, 202, 204, 400, 401, 403, 404, 409 và 500.
4. Vì sao không trả JPA entity trực tiếp.
5. Request DTO, command, entity và response DTO khác nhau ra sao.
6. Jackson bind JSON vào record như thế nào.
7. Bean Validation kiểm điều gì và không kiểm điều gì.
8. Validation frontend khác validation backend và database constraint.
9. Offset pagination khác cursor/keyset pagination.
10. Response/error contract thống nhất đem lại lợi ích gì.
11. Luồng validation error trong repo.
12. Cách test API bằng HTTP client/Postman.

---

## 1. REST là cách mô hình hóa resource

REST không đồng nghĩa “JSON qua HTTP”. Một REST API thường biểu diễn domain dưới dạng resource, dùng URL để định danh và HTTP method để diễn đạt thao tác.

Ví dụ hiện tại:

| Method | URL | Ý nghĩa |
|---|---|---|
| GET | <code>/api/v1/chat_conversations</code> | Danh sách conversation |
| GET | <code>/api/v1/chat_conversations/{id}</code> | Chi tiết một conversation |
| POST | <code>/api/v1/chat_conversations</code> | Tạo conversation |
| PUT | <code>/api/v1/chat_conversations/{id}</code> | Đổi tên conversation |
| DELETE | <code>/api/v1/chat_conversations/{id}</code> | Xóa conversation |
| GET | <code>/api/v1/chat_conversations/{id}/turns</code> | Lịch sử turn |
| POST | <code>/api/v1/chat</code> | Gửi message |
| POST | <code>/api/v1/agent/{id}/executions</code> | Tạo execution |

URL nên dùng danh từ. Tránh URL kiểu <code>/getConversations</code> hoặc <code>/deleteConversation</code> khi HTTP method đã mang ý nghĩa hành động.

Không cần cực đoan: endpoint <code>/chat</code> là command-oriented và vẫn chấp nhận được nếu contract frontend đã mô tả rõ. Quan trọng là semantics nhất quán.

---

## 2. Safe và idempotent

### Safe

Một method safe không nhằm thay đổi state server. GET và HEAD được kỳ vọng safe. Logging/metrics có thể thay đổi nội bộ, nhưng không được tạo business side effect như trừ tiền.

### Idempotent

Gửi cùng request một lần hay nhiều lần dẫn đến cùng trạng thái cuối.

| Method | Safe | Thường idempotent |
|---|---:|---:|
| GET | Có | Có |
| PUT | Không | Có |
| DELETE | Không | Có về trạng thái cuối |
| POST | Không | Không mặc định |
| PATCH | Không | Tùy thiết kế |

Ví dụ PUT đổi title thành “Java” gọi hai lần vẫn để title là “Java”. POST tạo conversation gọi hai lần thường tạo hai hàng.

Idempotent không có nghĩa response mọi lần giống hệt; lần DELETE thứ hai có thể trả 404, nhưng trạng thái “resource không còn” không đổi.

Với payment hoặc job creation cần retry, dùng idempotency key và lưu kết quả theo key thay vì hy vọng POST tự idempotent.

---

## 3. Chọn HTTP status

| Status | Dùng khi |
|---:|---|
| 200 OK | Đọc/cập nhật thành công và có body |
| 201 Created | Resource được tạo đồng bộ |
| 202 Accepted | Đã nhận job nhưng xử lý chưa hoàn thành |
| 204 No Content | Thành công và không có body |
| 400 Bad Request | Input sai cú pháp/boundary/business input |
| 401 Unauthorized | Chưa có identity hợp lệ |
| 403 Forbidden | Đã xác thực nhưng thiếu quyền |
| 404 Not Found | Resource không tồn tại hoặc không được phép lộ |
| 409 Conflict | Xung đột state/unique/optimistic lock |
| 500 Internal Server Error | Lỗi bất ngờ phía server |

Code create conversation:

~~~java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
ApiResponse<ConversationView> create(...) {
}
~~~

Code execute agent:

~~~java
@PostMapping("/{id}/executions")
@ResponseStatus(HttpStatus.ACCEPTED)
ApiResponse<ExecutionView> execute(...) {
}
~~~

Code delete:

~~~java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
void delete(...) {
}
~~~

Response 204 không được có response body. Vì vậy delete trả <code>void</code>, không bọc <code>ApiResponse</code>.

---

## 4. 201 và header Location

<code>201 Created</code> nói resource đã tồn tại. API production thường cân nhắc thêm:

~~~http
Location: /api/v1/chat_conversations/42
~~~

Repo hiện trả 201 và object mới nhưng chưa đặt Location header. Đây không làm demo sai, nhưng là một cải tiến REST hữu ích.

---

## 5. PUT khác PATCH

PUT thường biểu diễn thay thế đầy đủ representation tại URI. PATCH biểu diễn cập nhật một phần.

Repo dùng:

~~~http
PUT /api/v1/chat_conversations/1

{ "title": "Tên mới" }
~~~

Vì resource còn nhiều field nhưng request chỉ đổi title, PATCH có thể diễn đạt sát hơn. Tuy nhiên contract hiện tại có thể coi đây là “rename representation” đơn giản. Điều quan trọng là tài liệu hóa và giữ semantics nhất quán.

Với PATCH, cần phân biệt:

- Field không gửi.
- Field gửi <code>null</code>.
- Field gửi giá trị rỗng.

Record DTO đơn giản không tự giải quyết ba trạng thái đó.

---

## 6. Vì sao không trả JPA entity?

Trả entity trực tiếp gây rủi ro:

- Lộ field nội bộ như owner, version hoặc secret.
- Schema database trở thành public contract.
- Lazy relation phát query khi serialize.
- Quan hệ hai chiều tạo vòng lặp JSON.
- Client phụ thuộc field không được thiết kế cho nó.
- Thay đổi persistence làm vỡ API.
- Client có thể gửi lại field không nên sửa.

Repo map entity sang:

~~~java
public record ConversationView(
        long id,
        String title,
        int turnCount,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<Object> turns,
        LastTurn lastTurn,
        long chatConversationId
) {
}
~~~

DTO là contract có chủ đích. Một entity có thể có nhiều response DTO cho list, detail, admin và export.

---

## 7. Request DTO, command, entity và response DTO

~~~text
JSON request
  ↓
Controller request DTO
  ↓
Application command
  ↓
Entity/domain logic
  ↓
Response view DTO
  ↓
JSON response
~~~

Trong [AgentController.java](../../agent/AgentController.java):

~~~java
CreateAgentRequest request
    ↓
new AgentService.CreateAgent(...)
    ↓
new AgentDefinition(...)
    ↓
AgentView
~~~

Tách các model cho phép:

- HTTP annotation không tràn vào domain.
- Service API không phụ thuộc JSON naming.
- Response không lộ persistence.
- Mỗi boundary có validation phù hợp.

Không bắt buộc tạo bốn class cho use case cực nhỏ. Hãy tách khi trách nhiệm/contract thực sự khác, tránh mapping hình thức.

---

## 8. Record phù hợp làm DTO

Record:

~~~java
public record ConversationRequest(
        @NotBlank(message = "title is required")
        @Size(max = 120)
        String title
) {
}
~~~

phù hợp DTO vì:

- Ngắn gọn.
- Component final.
- Có accessor, constructor, equals/hashCode/toString.
- Thể hiện data carrier.

Record không tự deep immutable. Nếu component là mutable list, cần defensive copy khi contract yêu cầu.

Không log <code>toString()</code> của DTO chứa password/token.

---

## 9. Jackson và JSON naming

Jackson chuyển JSON thành Java object và ngược lại.

Frontend gửi snake_case:

~~~json
{
  "conversation_id": 1,
  "data_context": "docs"
}
~~~

Java dùng camelCase:

~~~java
@JsonProperty("conversation_id")
long conversationId
~~~

<code>@JsonProperty</code> giữ API contract cũ mà Java code vẫn theo naming convention.

Có thể cấu hình global snake_case, nhưng phải đánh giá ảnh hưởng toàn API. Annotation explicit phù hợp khi chỉ vài field hoặc cần tương thích contract hiện hữu.

Các lỗi binding thường gặp:

- JSON malformed.
- String gửi vào field số.
- Tên field sai.
- Enum value không hợp lệ.
- Date format sai.

Các lỗi này có thể xảy ra trước khi method controller chạy.

---

## 10. Bean Validation

Annotations repo đang dùng:

| Annotation | Ý nghĩa |
|---|---|
| <code>@NotBlank</code> | String không null, không rỗng và không chỉ whitespace |
| <code>@Size(max=N)</code> | Giới hạn độ dài/kích thước |
| <code>@Positive</code> | Số phải lớn hơn 0 |
| <code>@Email</code> | Hình dạng email hợp lệ ở mức validator |
| <code>@Valid</code> | Kích hoạt cascade validation cho argument/object lồng |

Ví dụ:

~~~java
public record ChatRequest(
        @NotBlank String message,
        @Positive long conversationId,
        String dataContext,
        List<String> url,
        String modelName
) {
}
~~~

Validation này kiểm boundary. Nó không biết conversation ID có tồn tại, thuộc user hay đang bị khóa; các rule cần database/state thuộc service/domain.

---

## 11. Ba lớp validation

### Client validation

Zod/form validation cho feedback nhanh và UX tốt. Client có thể bị bỏ qua nên không phải security boundary.

### Server validation

Bean Validation và business rule kiểm mọi request tới backend. Đây là trust boundary chính.

### Database constraint

NOT NULL, UNIQUE, FK, CHECK bảo vệ dữ liệu trước concurrency, bug và đường ghi khác.

~~~text
Client validation → trải nghiệm
Server validation → contract/use case
DB constraint → toàn vẹn cuối cùng
~~~

Ba lớp bổ sung cho nhau, không thay thế nhau.

---

## 12. Validation flow trong repo

Request title rỗng:

~~~text
HTTP JSON
  ↓
Jackson tạo ConversationRequest
  ↓
@Valid kích hoạt @NotBlank
  ↓ fail
MethodArgumentNotValidException
  ↓
GlobalExceptionHandler.handleValidation()
  ↓
HTTP 400
~~~

Handler gom lỗi:

~~~java
Map<String, String> errors = new LinkedHashMap<>();
fieldErrors.forEach(error ->
        errors.putIfAbsent(
                error.getField(),
                error.getDefaultMessage()
        )
);
~~~

<code>putIfAbsent</code> giữ message đầu tiên cho mỗi field. Nếu product cần tất cả lỗi, contract có thể dùng <code>Map&lt;String, List&lt;String&gt;&gt;</code>.

---

## 13. Business validation

Service kiểm ownership:

~~~java
repository.findByIdAndOwnerEmail(id, ownerEmail)
        .orElseThrow(() ->
            new NotFoundException(...)
        );
~~~

Đây không thể chỉ làm bằng annotation trên DTO vì cần truy vấn state.

Các rule khác thuộc service/domain:

- Không rename conversation đã archived.
- Không tạo turn khi quota hết.
- Không execute agent đang disabled.
- Title phải duy nhất theo user.

Không nhét repository call vào custom validator một cách tùy tiện; điều đó có thể làm validation khó dự đoán và phát query lặp.

---

## 14. Response envelope

[ApiResponse.java](../../common/ApiResponse.java):

~~~java
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        ApiMeta meta
) {
}
~~~

Lợi ích:

- Frontend đọc shape nhất quán.
- Có machine-readable code.
- Có message.
- Generic data giữ type.
- Metadata phân trang có vị trí cố định.

Ví dụ success:

~~~json
{
  "code": "SUCCESS",
  "message": "Request completed",
  "data": {
    "id": 1,
    "title": "Java"
  },
  "meta": {
    "totalItems": 0,
    "itemsPerPage": 0,
    "totalPages": 0,
    "currentPage": 1,
    "hasNextPage": false,
    "hasPreviousPage": false
  }
}
~~~

Envelope có trade-off: response đơn giản bị verbose và mọi client phải unwrap data. Vì frontend SoftAIBox đã dùng BaseResponse shape, consistency ở đây có giá trị.

---

## 15. Error contract

Not found:

~~~json
{
  "code": "NOT_FOUND",
  "message": "Conversation 99 was not found",
  "data": null,
  "meta": {
    "totalItems": 0,
    "itemsPerPage": 0,
    "totalPages": 0,
    "currentPage": 1,
    "hasNextPage": false,
    "hasPreviousPage": false
  }
}
~~~

Validation:

~~~json
{
  "code": "VALIDATION_ERROR",
  "message": "Input is invalid",
  "data": {
    "title": "title is required"
  },
  "meta": {
    "totalItems": 0,
    "itemsPerPage": 0,
    "totalPages": 0,
    "currentPage": 1,
    "hasNextPage": false,
    "hasPreviousPage": false
  }
}
~~~

Client nên dựa vào HTTP status và stable error code. Message có thể đổi/ngôn ngữ hóa, không nên là khóa logic duy nhất.

Production không trả:

- Stack trace.
- SQL.
- Secret/token.
- Internal host/path.
- Exception message chứa implementation detail.

Log nội bộ có correlation ID để tra cứu, response chỉ trả thông tin an toàn.

---

## 16. Exception mapping hiện tại

[GlobalExceptionHandler.java](../../common/GlobalExceptionHandler.java) map:

| Exception | Status | Code |
|---|---:|---|
| <code>NotFoundException</code> | 404 | NOT_FOUND |
| <code>MethodArgumentNotValidException</code> | 400 | VALIDATION_ERROR |
| <code>IllegalArgumentException</code> | 400 | BAD_REQUEST |
| <code>ConstraintViolationException</code> | 400 | BAD_REQUEST |

Khoảng trống cần biết:

- Chưa map duplicate/optimistic conflict thành 409.
- Chưa có handler tổng quát trả error ID an toàn cho 500.
- JSON parse/type mismatch cần contract rõ.
- Security 401/403 thường xảy ra trước controller advice.
- Login sai đang thành 400 do <code>IllegalArgumentException</code>; một hệ thống có thể chọn 401 theo contract.

Không bắt <code>Exception</code> rồi trả 200 với code lỗi. HTTP status phải phản ánh kết quả transport/application.

---

## 17. 401 và 403

<code>401 Unauthorized</code> thực chất nghĩa request chưa có authentication hợp lệ:

- Thiếu bearer token.
- Token sai signature.
- Token hết hạn.

<code>403 Forbidden</code>:

- Token hợp lệ.
- Identity đã biết.
- Nhưng không có quyền.

Ví dụ USER gọi <code>/api/v1/admin/demo</code> phải 403; ADMIN gọi được 200.

Không đổi mọi lỗi ownership thành 403 nếu việc đó làm lộ resource tồn tại. Repo query theo ID + owner và trả 404, giúp tránh enumeration.

---

## 18. Offset pagination

Request:

~~~http
GET /api/v1/chat_conversations?page=1&limit=10&keyword=java
~~~

Service:

~~~java
int safePage = Math.max(1, page) - 1;
int safeLimit = Math.clamp(limit, 1, 100);
PageRequest.of(safePage, safeLimit);
~~~

API page bắt đầu từ 1; Spring Data page bắt đầu từ 0. Limit bị clamp để bảo vệ server.

[ApiMeta.java](../../common/ApiMeta.java) chuyển Page thành:

- totalItems.
- itemsPerPage.
- totalPages.
- currentPage.
- hasNextPage.
- hasPreviousPage.

### Hạn chế của offset

Query kiểu <code>OFFSET 100000 LIMIT 20</code> có thể phải bỏ qua rất nhiều hàng. Khi dữ liệu mới được chèn giữa hai request, item có thể trùng hoặc bị bỏ qua.

Offset hợp:

- Admin table.
- Dataset vừa.
- Cần nhảy trang.

---

## 19. Cursor/keyset pagination

Với infinite scroll execution theo <code>(createdAt, id)</code>:

~~~sql
SELECT *
FROM agent_executions
WHERE owner_email = :owner
  AND (
    created_at < :cursorCreatedAt
    OR (
      created_at = :cursorCreatedAt
      AND id < :cursorId
    )
  )
ORDER BY created_at DESC, id DESC
LIMIT :limit;
~~~

Cursor cần cả <code>createdAt</code> và <code>id</code> vì timestamp có thể trùng. Sort phải ổn định và index phải theo access pattern.

Response có thể trả:

~~~json
{
  "data": [],
  "meta": {
    "nextCursor": "opaque-value",
    "hasNextPage": true
  }
}
~~~

Cursor nên opaque với client để backend có thể đổi encoding. Ký cursor nếu cần chống sửa.

---

## 20. Filter, sort và giới hạn

Mọi list endpoint nên quyết định rõ:

- Field filter được phép.
- Sort field được phép.
- Default sort.
- Maximum page size.
- Case sensitivity.
- Empty keyword.

Không đưa trực tiếp tên cột/order tùy ý từ client vào SQL. Dùng allowlist để tránh injection và query quá đắt.

Conversation query dùng sort cố định <code>updatedAt DESC</code>, an toàn và phù hợp màn hình lịch sử.

---

## 21. Versioning API

Repo dùng prefix:

~~~text
/api/v1
~~~

Versioning giúp quản lý breaking change, nhưng không phải lý do phá contract thường xuyên. Thay đổi additive như thêm optional response field có thể không cần v2; đổi nghĩa/xóa field có thể cần migration/version mới.

Cần quản lý:

- Deprecation policy.
- Thời gian hỗ trợ version cũ.
- OpenAPI/changelog.
- Contract tests với frontend.

---

## 22. Request/data flow: tạo conversation

~~~text
JSON body
  ↓ Jackson
ConversationRequest
  ↓ Bean Validation
ConversationController
  ↓ title + authenticated email
ConversationService
  ↓ entity/repository
Database
  ↓
ConversationView
  ↓
ApiResponse
  ↓ Jackson
HTTP 201 JSON
~~~

Owner không có trong request DTO. Đây là thiết kế quan trọng: identity đến từ token đã xác thực.

---

## 23. Request/data flow: lỗi title rỗng

~~~text
POST body { "title": "   " }
  ↓
@NotBlank fail
  ↓
MethodArgumentNotValidException
  ↓
GlobalExceptionHandler
  ↓
HTTP 400
code = VALIDATION_ERROR
data.title = title is required
~~~

Service và repository không chạy. Đây là fail fast tại boundary.

---

## 24. Cách chạy API

Từ repository root:

~~~powershell
./mvnw.cmd clean test
./mvnw.cmd spring-boot:run
~~~

Login:

~~~http
POST http://localhost:8080/api/v1/auth/email/login
Content-Type: application/json

{
  "email": "dat@softaibox.local",
  "password": "java123"
}
~~~

Tạo conversation:

~~~http
POST http://localhost:8080/api/v1/chat_conversations
Authorization: Bearer access-token
Content-Type: application/json

{
  "title": "REST practice"
}
~~~

Test validation:

~~~http
POST http://localhost:8080/api/v1/chat_conversations
Authorization: Bearer access-token
Content-Type: application/json

{
  "title": "   "
}
~~~

Dùng [requests.http](../../../../../../../../requests.http) hoặc import collection theo [postman/README.md](../../../../../../../../postman/README.md). Collection tự lưu access token, refresh token, conversation ID, agent ID và execution ID.

---

## 25. Checklist test API

Với mỗi endpoint, test:

- Happy path.
- Thiếu token.
- Token sai/hết hạn.
- Sai role.
- JSON malformed.
- Field null/rỗng/quá dài.
- ID âm/0.
- Resource không tồn tại.
- Resource thuộc user khác.
- Duplicate/conflict.
- Page/limit boundary.
- Method sai.
- Content-Type sai.
- Response không lộ field nội bộ.

Automation nên assert cả status, code, data shape và side effect database.

---

## 26. Những lỗi thường gặp

### Trả 200 cho mọi kết quả

Client, proxy và monitoring mất semantics HTTP. Dùng status đúng và error body ổn định.

### Dùng entity làm request/response

Client có thể over-post field, lazy relation bị serialize và schema bị coupling.

### Chỉ validate ở frontend

Attacker/client khác bỏ qua frontend. Backend và database vẫn phải bảo vệ.

### Dùng <code>@NotNull</code> cho String cần nội dung

Chuỗi rỗng vẫn qua. Dùng <code>@NotBlank</code> khi whitespace không hợp lệ.

### Quên <code>@Valid</code>

Constraint trên request có thể không được kích hoạt tại boundary như mong đợi.

### Cho page size không giới hạn

Một request có thể chiếm memory/DB/serialization quá lớn.

### Trả raw exception message

Có thể lộ SQL, class, path hoặc thông tin nhạy cảm.

### Pagination thiếu sort ổn định

Nếu nhiều hàng có cùng timestamp và không có tie-breaker ID, trang có thể trùng/mất item.

### Dùng message làm error code

Message có thể đổi. Client cần code ổn định, con người cần message.

---

## 27. Bài thực hành

### Bài 1 — Ma trận endpoint

Lập bảng cho toàn bộ endpoint: method, path, request DTO, success status, error status, auth rule và response DTO.

### Bài 2 — Request ID

Thiết kế filter tạo/nhận <code>X-Request-Id</code>, đưa vào log và error response. Nêu cách tránh tin giá trị quá dài/độc hại từ client.

### Bài 3 — Conflict

Thiết kế title duy nhất theo owner:

1. Unique constraint.
2. Exception mapping.
3. HTTP 409.
4. Stable code <code>CONVERSATION_TITLE_CONFLICT</code>.

Giải thích vì sao check trước insert vẫn chưa đủ.

### Bài 4 — Cursor pagination

Thiết kế request/response cho agent execution dùng cursor <code>(createdAt,id)</code>, viết SQL và index tương ứng.

### Bài 5 — DTO review

So sánh entity và DTO hiện tại. Liệt kê field nào chỉ để tương thích frontend, field nào persistence-only và field nào chưa được implement thật như <code>deletedAt</code>.

### Bài 6 — Error cases

Chạy folder Validation trong Postman. Với từng request, ghi lỗi bị chặn ở security filter, JSON/validation, service hay database.

### Bài 7 — PUT/PATCH

Thiết kế lại rename thành PATCH. Nêu semantics của missing/null/blank và cách giữ backward compatibility.

---

## 28. Câu hỏi phỏng vấn và đáp án ngắn

### REST là gì?

Là architectural style mô hình resource, representation và thao tác qua uniform interface như HTTP method.

### Safe khác idempotent?

Safe không nhằm thay đổi business state. Idempotent nghĩa gọi lặp dẫn tới cùng trạng thái cuối.

### POST khác PUT?

POST thường tạo subordinate resource/command và không idempotent mặc định. PUT đặt representation tại URI và thường idempotent.

### PATCH khác PUT?

PATCH cập nhật một phần; PUT thường thay thế toàn representation.

### Vì sao create trả 201?

Vì resource mới đã được tạo; có thể kèm Location.

### Vì sao async job trả 202?

Vì server mới chấp nhận xử lý, kết quả cuối chưa sẵn sàng.

### Vì sao DELETE trả 204?

Thao tác thành công nhưng không cần body.

### 401 khác 403?

401 là chưa có authentication hợp lệ; 403 là đã xác thực nhưng thiếu quyền.

### Vì sao không trả entity?

Để tránh lộ field, lazy serialization, vòng lặp relation và coupling API với database.

### Bean Validation khác business validation?

Bean Validation kiểm shape/boundary; business validation cần domain state hoặc database.

### Offset khác cursor pagination?

Offset dễ dùng và nhảy trang nhưng chậm/lệch ở dữ liệu lớn; cursor ổn định và hiệu quả hơn cho feed tuần tự.

### Idempotency key dùng khi nào?

Khi client có thể retry một operation không idempotent như payment hoặc tạo job mà không được tạo trùng.

### Vì sao cần stable error code?

Client xử lý logic theo code; message dành cho con người và có thể thay đổi.

---

## 29. Liên hệ SoftAIBox

Frontend SoftAIBox cần contract ổn định để Axios/TanStack Query:

- Unwrap <code>data</code>.
- Đọc pagination <code>meta</code>.
- Hiển thị field validation.
- Refresh khi nhận 401.
- Hiển thị forbidden khi 403.
- Poll execution sau 202.
- Invalidate conversation query sau mutation.

Luồng trách nhiệm:

~~~text
Zod → UX nhanh
HTTP DTO + Bean Validation → trust boundary
Service → business rule/ownership
Database → constraint
ApiResponse/Error code → frontend xử lý nhất quán
~~~

---

## 30. Kế hoạch học

1. 60 phút: HTTP method, safe/idempotent và status code.
2. 60 phút: đọc DTO/Jackson/validation trong ba controller.
3. 60 phút: đọc ApiResponse, ApiMeta và GlobalExceptionHandler.
4. 60 phút: offset và cursor pagination.
5. 60–90 phút: chạy Postman happy/error cases, hoàn thành ma trận endpoint.
6. 30 phút: trả lời phỏng vấn thành tiếng.

---

## Definition of Done

Bạn hoàn thành ngày 12 khi có thể:

- Thiết kế URL theo resource và chọn HTTP method phù hợp.
- Phân biệt safe/idempotent.
- Chọn đúng status phổ biến và giải thích.
- Tách request DTO, command, entity và response DTO.
- Dùng record, Jackson annotation và Bean Validation đúng chỗ.
- Giải thích ba lớp validation.
- Lần validation error tới HTTP 400.
- Giải thích response envelope và error code.
- Thiết kế 409 cho conflict.
- Giới hạn offset pagination và thiết kế cursor ổn định.
- Chạy toàn bộ request conversation/chat/error bằng Postman.
- Chỉ ra các khoảng trống của error handler hiện tại.
- Liên hệ API contract với Axios/TanStack Query.
- Trả lời câu hỏi phỏng vấn không nhìn tài liệu.
