# Bộ ôn phỏng vấn Java Junior

Đừng học thuộc nguyên văn. Với mỗi câu, hãy trả lời 60-90 giây theo cấu trúc: định
nghĩa -> ví dụ -> trade-off/lỗi thường gặp. Nếu không tự nói được, quay lại ngày tương
ứng trong lộ trình.

## Java core

### 1. JDK, JRE và JVM khác nhau thế nào?

JDK là bộ phát triển gồm compiler/tool/runtime. JVM thực thi bytecode và quản lý memory,
GC, JIT. JRE là runtime gồm JVM và thư viện để chạy ứng dụng. Source được `javac` biên
dịch thành bytecode portable; JVM của từng nền tảng thực thi bytecode đó.

### 2. Java pass-by-value hay pass-by-reference?

Luôn pass-by-value. Với primitive, copy giá trị primitive. Với object, copy giá trị của
reference; hai reference có thể trỏ cùng object nên mutation nhìn thấy ở caller, nhưng
gán parameter sang object mới không đổi biến caller.

### 3. `==` và `equals()`?

Primitive `==` so giá trị; object `==` so identity/reference. `equals` so equality logic
do class định nghĩa. Nếu override `equals` phải override `hashCode` nhất quán để dùng
đúng trong HashMap/HashSet.

### 4. String immutable có lợi gì?

An toàn để share/cache/intern, thread-safe tự nhiên, hashCode ổn định làm map key, giảm
rủi ro security khi truyền path/URL/class name. Nối String nhiều lần trong loop tạo nhiều
object; dùng `StringBuilder`.

### 5. `final`, `finally`, `finalize`?

`final` giới hạn gán lại/override/inheritance; final reference vẫn có thể trỏ object
mutable. `finally` là block cleanup sau try/catch (nhưng try-with-resources tốt hơn cho
resource). `finalize` là cơ chế cũ không đáng tin và đã bị loại bỏ/deprecate; không dùng
để quản lý resource.

### 6. Overload và override?

Overload chọn ở compile time dựa trên danh sách tham số. Override là subtype cung cấp
implementation cùng contract, dispatch ở runtime. Return type override có thể covariant;
access không được thu hẹp và checked exception không được rộng hơn parent contract.

### 7. Interface và abstract class?

Interface mô tả capability, hỗ trợ nhiều interface và là dependency contract tốt.
Abstract class chia sẻ state/constructor/implementation giữa các subtype có quan hệ gần.
Ưu tiên interface + composition nếu không thực sự là “is-a”.

### 8. ArrayList và LinkedList?

ArrayList dùng array động, random access O(1), locality tốt, append amortized O(1), chèn
giữa O(n). LinkedList node kép, access O(n); chèn O(1) chỉ khi đã có iterator/node. Trong
thực tế ArrayList thường là mặc định vì cache locality và ít allocation.

### 9. HashMap hoạt động thế nào?

Tính hash, phân bucket, dùng equals tìm key trong bucket; trung bình get/put O(1), xấu
nhất phụ thuộc collision và treeification. Key cần equals/hashCode ổn định; key mutable
có thể “mất” sau khi hash-changing field đổi.

### 10. Checked và unchecked exception?

Checked buộc catch/declare, phù hợp lỗi caller có thể phục hồi có ý nghĩa. Unchecked
thường là programmer error/domain rejection và đi qua nhiều layer dễ hơn. Không catch
rộng rồi nuốt lỗi; wrap cần giữ cause.

### 11. Optional dùng khi nào?

Tốt cho return “có thể không có”. Không nên dùng tùy tiện làm field JPA, request DTO,
parameter hay collection element. Dùng `map/flatMap/orElseGet/orElseThrow`; chú ý
`orElse` tính eager còn `orElseGet` lazy.

### 12. Stream khác collection?

Collection chứa dữ liệu; Stream mô tả pipeline một lần, lazy cho intermediate operation.
Stream không tự nhanh hơn loop và parallel stream có overhead/common-pool risk. Tránh
side effect trong pipeline.

### 13. Generic và type erasure?

Generic đưa type safety vào compile time và giảm cast. JVM phần lớn erase type parameter,
nên không `new T()`, không `instanceof List<String>` và overload chỉ khác generic type có
thể clash. Wildcard dùng PECS: producer extends, consumer super.

### 14. Immutable object thiết kế ra sao?

Class/record không cho mutation, field final, validate ở constructor, không expose mutable
collection, defensive copy input/output. Nếu field trỏ mutable object thì chỉ `final`
reference chưa đủ.

## Memory và concurrency

### 15. Stack, heap và GC?

Mỗi thread có stack frame cho call/local; object thường ở heap dùng chung. Object
unreachable là ứng viên GC, nhưng thời điểm thu hồi không bảo đảm. Memory leak Java vẫn
xảy ra khi reference bị giữ ngoài ý muốn (static map, listener, cache không eviction).

### 16. Race condition và `volatile`?

Race condition là kết quả phụ thuộc interleaving không kiểm soát. `volatile` bảo đảm
visibility/order của đọc-ghi biến, không làm compound action như `count++` atomic. Dùng
immutable/confinement/atomic/lock tùy invariant.

### 17. `synchronized` và deadlock?

`synchronized` mutual exclusion + happens-before quanh cùng monitor. Deadlock khi các
thread giữ resource và chờ vòng tròn. Phòng bằng lock ordering nhất quán, giảm phạm vi
lock, timeout/tryLock, không gọi I/O bên ngoài khi đang giữ lock.

### 18. Virtual thread dùng khi nào?

Rất nhiều task blocking I/O độc lập như HTTP/DB. Nó tăng scalability/concurrency, không
làm CPU task nhanh hơn. Vẫn cần giới hạn downstream connection/rate, timeout và tránh
pinning/lock dài.

### 19. `@Async` có bẫy gì?

Chạy qua Spring proxy nên self-invocation có thể không async. Exception của `void` không
trả cho caller; lifecycle/context/security cần cân nhắc. Phải cấu hình executor, queue,
timeout và observability thay vì mặc định mù.

## SQL, JPA và transaction

### 20. ACID?

Atomicity: toàn bộ hoặc rollback. Consistency: invariant/constraint được giữ. Isolation:
transaction concurrent được tách ở mức cam kết. Durability: commit tồn tại sau crash.
Nêu ví dụ payment + subscription thay đổi cùng transaction DB.

### 21. Index là gì, trade-off?

Cấu trúc dữ liệu giúp tìm/sort/join nhanh, đổi lại storage và write/update chậm. Chọn từ
query pattern/selectivity; composite index có leftmost-prefix/order. Dùng explain plan,
không thêm index theo cảm giác.

### 22. JPA entity lifecycle?

Transient chưa được persistence context quản; managed được dirty checking; detached đã
rời context; removed chờ delete. Trong transaction, sửa managed entity có thể tự flush
khi commit. `save` của Spring Data không phải lúc nào cũng INSERT ngay.

### 23. Lazy/eager và N+1?

Lazy trì hoãn relation; truy cập ngoài persistence context có thể lỗi. N+1: một query
parent rồi N query relation. Dùng fetch join, entity graph, projection hoặc batch đúng
use case; không đổi tất cả thành EAGER vì dễ tạo query/cartesian khổng lồ.

### 24. Optimistic và pessimistic locking?

Optimistic dùng version, cho concurrency cao và phát hiện conflict khi update; phù hợp
xung đột hiếm. Pessimistic khóa DB sớm, phù hợp critical section/xung đột cao nhưng giảm
throughput, có deadlock/timeout. `@Version` trong Conversation là optimistic lock.

### 25. `@Transactional` đặt ở đâu?

Thường ở service/use-case boundary nơi biết các thao tác phải atomic. Không giữ transaction
mở qua network call chậm. Annotation dựa proxy nên private/self-invoked method không tạo
boundary như mong đợi; rollback mặc định thường cho unchecked exception.

## Spring và REST

### 26. IoC và DI?

IoC là container kiểm soát creation/lifecycle; DI là cách cung cấp dependency thay vì
class tự `new`. Constructor injection làm dependency rõ/bắt buộc/testable, tránh hidden
state của field injection.

### 27. `@Component`, `@Service`, `@Repository`, `@RestController`?

Đều tạo bean stereotype nhưng diễn đạt vai trò. Repository có exception translation;
RestController kết hợp controller + response body. Vai trò rõ giúp architecture và AOP,
không phải chỉ để component scan.

### 28. Bean singleton có thread-safe không?

Không tự động. Nhiều request cùng dùng một instance; bean stateless thường an toàn, field
mutable per-request gây race/data leak. Thread-safe phụ thuộc state và synchronization,
không phụ thuộc annotation.

### 29. DTO vì sao không trả entity?

DTO giữ API contract ổn định, tránh lộ field, lazy-loading/vòng relation, over-posting và
coupling schema. Mapping có cost nhưng boundary rõ; projection giúp query đúng dữ liệu.

### 30. POST, PUT, PATCH và idempotency?

POST thường tạo/command và không mặc định idempotent. PUT thay thế trạng thái tại URI,
gọi lặp cùng payload cho cùng kết quả. PATCH cập nhật phần. Payment/webhook cần idempotency
key vì retry có thể xảy ra dù dùng POST.

### 31. Pagination offset và cursor?

Offset/page đơn giản và nhảy trang dễ, nhưng page sâu chậm và dữ liệu insert/delete gây
trùng/bỏ sót. Cursor/keyset dùng sort key ổn định, nhanh/ổn định cho infinite scroll nhưng
khó nhảy đến trang tùy ý.

### 32. 401 và 403?

401: thiếu/sai/hết hạn authentication credential, thường kèm challenge. 403: đã xác thực
nhưng không được phép. 404 đôi khi dùng để tránh lộ resource existence cho object không
thuộc user.

## Security

### 33. JWT gồm gì?

Header, payload claims, signature. Base64url không phải encryption; ai có token có thể
đọc payload. Server verify algorithm/signature/exp/iss/aud. Key rotation và short TTL
quan trọng.

### 34. Access và refresh token?

Access ngắn, dùng gọi API. Refresh dài hơn, chỉ dùng lấy access mới, cần rotate/revoke và
lưu an toàn. Nếu refresh bị đánh cắp, access TTL ngắn không đủ; reuse detection/device
session giúp giảm rủi ro.

### 35. CORS và CSRF?

CORS là browser policy cho cross-origin response, không chặn server-to-server/attacker
gọi API. CSRF lợi dụng browser tự gắn credential, thường liên quan cookie. Token trong
Authorization không tự được browser gắn nhưng XSS/storage vẫn là rủi ro khác.

### 36. RBAC có đủ chống IDOR?

Không. Hai user cùng role USER nhưng user A không được đọc `/conversation/{id}` của B.
Cần ownership/tenant predicate trong query hoặc policy check. Mini backend query theo cả
`id` và `ownerEmail`.

### 37. Lưu password thế nào?

BCrypt/Argon2 với salt và cost phù hợp, qua thư viện chuẩn. Không plaintext, không mã hóa
reversible, không hash nhanh kiểu SHA đơn. Có rate limit, MFA/reset token one-time, tránh
phân biệt lỗi “email không tồn tại”.

## Câu hỏi dự án SoftAIBox

### 38. Nếu chuyển SoftAIBox backend sang Java, em chia module ra sao?

Bắt đầu modular monolith theo domain auth, conversation/chat, agent, datasource, payment,
notification; mỗi module có API/application/domain/infrastructure vừa đủ. Shared chỉ chứa
thứ thực sự chung. Chưa tách microservice khi chưa có nhu cầu scaling/team/deployment độc
lập vì distributed transaction/observability phức tạp hơn.

### 39. Chat streaming nên làm thế nào?

Client gửi command, backend xác thực/ghi pending turn rồi gọi model ngoài transaction.
Stream token một chiều bằng SSE; reconnect dùng event ID nếu cần. WebSocket khi có hai
chiều liên tục. Có timeout/cancel, backpressure, sanitize/render phía client, lưu final
answer và metric token/latency.

### 40. Agent scheduling chạy nhiều replica?

`@Scheduled` đơn thuần sẽ chạy ở từng replica. Chọn distributed lock/Quartz clustered,
external scheduler đẩy queue, hoặc leader election. Execution có unique/idempotency key,
lease/heartbeat, retry/backoff và dead-letter; state lưu bền để recover.

### 41. Stripe webhook xử lý ra sao?

Đọc raw body, verify signature/timestamp, insert event ID với unique constraint, trả nhanh,
xử lý idempotent async. Không tin success redirect của browser. Map subscription bằng
state machine và reconcile định kỳ. Log không chứa secret/full payment data.

### 42. Chạy `fetch_function` do user nhập có nguy hiểm không?

Rất nguy hiểm: RCE, đọc secret/file, SSRF, đào coin, DoS. Không eval trong app process.
Chạy sandbox/container/VM tách biệt với allowlist egress, read-only FS, timeout, CPU/RAM
quota, package allowlist, scan và audit; hoặc thay code tùy ý bằng DSL/connector cấu hình.

## Bài coding nên làm sau mỗi buổi

1. Two Sum bằng HashMap - giải thích O(n) time/O(n) space.
2. Valid Parentheses bằng stack.
3. First non-repeating character bằng frequency map.
4. Merge intervals - sort rồi scan.
5. Top K frequent words - map + heap/sort.
6. LRU cache - HashMap + doubly linked list hoặc LinkedHashMap.
7. Producer-consumer bounded queue.
8. SQL: user có nhiều conversation, lấy top 3 mới nhất mỗi user bằng window function.
9. REST design cho share datasource accept/reject.
10. Unit test promotion, token expiry và conversation ownership.

Khi code trên bảng, luôn nói trước input/output, edge case, approach, Big-O; viết xong chạy
tay ít nhất một case bình thường, rỗng, một phần tử và duplicate.

## Kịch bản mock interview 60 phút

- 5 phút: giới thiệu bản thân và lý do chuyển từ frontend/fullstack sang Java.
- 15 phút: 6 câu Java core trong danh sách trên.
- 10 phút: Spring request lifecycle + transaction/JPA.
- 10 phút: JWT/RBAC/ownership và một security threat.
- 15 phút: coding Two Sum hoặc grouping agent executions.
- 5 phút: bạn hỏi interviewer về architecture, testing, release và mentoring.

## Mẫu giới thiệu 90 giây

> Em có ba năm tập trung vào React/TypeScript và từng làm fullstack Node.js/MongoDB. Ở
> SoftAIBox em phụ trách frontend cho chat AI, TanStack Query server state, agent dashboard,
> RBAC, upload và Stripe flow. Khi học Java em không chỉ làm syntax exercise mà xây mini
> backend giữ đúng API contract của dự án: Spring Boot, JPA, validation, JWT refresh,
> ownership, async agent và polling. Điểm mạnh của em là hiểu luồng end-to-end và UX của
> API consumer; phần em đang tiếp tục đào sâu là SQL tuning, transaction dưới tải và vận
> hành production Java.

Hãy điều chỉnh cho đúng sự thật tại thời điểm phỏng vấn; không nhận phần backend production
chưa từng trực tiếp triển khai.
