# Ngày 14 — Async agent, scheduler, Docker, production và capstone

Ngày 14 ghép toàn bộ lộ trình thành một backend có thể demo. Trọng tâm là thiết kế công việc lâu chạy ngoài request thread, theo dõi trạng thái, vận hành scheduler an toàn và phân biệt “chạy được local” với “sẵn sàng production”.

Code chính:

- [JavaBeginApplication.java](../../JavaBeginApplication.java): bật async và scheduling.
- [AgentController.java](../../agent/AgentController.java): tạo agent, nhận execution và polling.
- [AgentService.java](../../agent/AgentService.java): tạo, liệt kê và kiểm tra ownership của agent.
- [AgentExecutionStore.java](../../agent/AgentExecutionStore.java): state execution trong memory.
- [AgentTaskRunner.java](../../agent/AgentTaskRunner.java): worker <code>@Async</code>.
- [AgentMaintenanceJob.java](../../agent/AgentMaintenanceJob.java): scheduler heartbeat.
- [application.yml](../../../../../../resources/application.yml): virtual thread và config.
- [Dockerfile](../../../../../../../../Dockerfile) và [compose.yml](../../../../../../../../compose.yml): đóng gói/chạy container.

---

## Mục tiêu

Sau ngày 14, bạn cần:

- Biết khi nào request nên đồng bộ và khi nào nên trả 202.
- Mô tả state machine PENDING → RUNNING → COMPLETED/FAILED.
- Hiểu <code>@Async</code>, executor, proxy và lỗi self-invocation.
- Phân biệt concurrency với durability.
- Thiết kế timeout, retry, backoff, jitter và dead-letter.
- Giải thích idempotency và at-least-once delivery.
- Hiểu polling, SSE và WebSocket.
- Phân biệt fixed delay, fixed rate và cron.
- Nhận diện scheduler trùng khi có nhiều replicas.
- Hiểu backpressure và bounded concurrency.
- Biết ba trụ quan sát: logs, metrics, traces.
- Đọc Dockerfile multi-stage và đánh giá production gaps.
- Demo toàn bộ login → chat → agent → RBAC.
- Trình bày trade-off trung thực trong phỏng vấn.

---

## 1. Vì sao không giữ HTTP request cho job lâu?

Một model call hoặc workflow agent có thể mất vài giây đến vài phút. Nếu giữ request:

- Client/proxy/load balancer có thể timeout.
- Server giữ connection và memory.
- Retry client có thể tạo job trùng.
- Khó theo dõi tiến độ.
- Deploy/restart làm mất kết nối.
- User phải chờ một response dài.

Thiết kế async:

~~~text
Client POST execute
  ↓
Server validate + tạo execution PENDING
  ↓
Server enqueue/start worker
  ↓
HTTP 202 + executionId
  ↓
Client poll/SSE
  ↓
Worker RUNNING → COMPLETED/FAILED
~~~

<code>202 Accepted</code> nói server đã nhận yêu cầu, không nói job chắc chắn đã hoàn thành.

---

## 2. Flow execution trong repo

Endpoint:

~~~http
POST /api/v1/agent/{id}/executions
Authorization: Bearer access-token
~~~

[AgentController.java](../../agent/AgentController.java):

~~~java
AgentDefinition agent =
        service.requireOwned(id, authentication.getName());
UUID executionId =
        executionStore.create(agent.getId());
taskRunner.run(executionId, agent.getTaskPrompt());
return ApiResponse.success(
        "Agent execution accepted",
        executionStore.get(executionId)
);
~~~

Các bước:

1. Kiểm agent tồn tại và thuộc user.
2. Tạo execution với UUID và PENDING.
3. Gọi worker async.
4. Trả snapshot cùng status 202.
5. Client dùng execution ID để poll.

Vì worker có thể chạy rất nhanh, response có thể đã mang bất kỳ trạng thái hợp lệ nào
tại thời điểm đọc: PENDING, RUNNING, COMPLETED hoặc FAILED. Client không nên giả định
status ban đầu luôn cố định.

---

## 3. State machine

[AgentExecutionStore.java](../../agent/AgentExecutionStore.java) định nghĩa:

~~~java
enum Status {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
~~~

State transition mong muốn:

~~~text
PENDING ──start──> RUNNING ──success──> COMPLETED
                       └────failure──> FAILED
~~~

Một state machine production cần rule:

- PENDING có được cancel không?
- RUNNING timeout thành trạng thái nào?
- FAILED có retry không?
- COMPLETED có immutable không?
- Có lưu startedAt, finishedAt, attempt, errorCode không?
- Ai được phép chuyển state?

Store hiện không ngăn transition ngược hoặc completed hai lần. API method cụ thể đang dùng đúng flow, nhưng invariant chưa được encode đầy đủ.

---

## 4. <code>@Async</code> hoạt động qua proxy

[AgentTaskRunner.java](../../agent/AgentTaskRunner.java):

~~~java
@Async
public void run(UUID executionId, String prompt) {
    ...
}
~~~

Spring bọc bean bằng proxy:

~~~text
Controller thread
  ↓ gọi taskRunner proxy
Proxy submit task vào executor
  ↓ return ngay
Worker/virtual thread
  ↓ chạy method target
~~~

Nếu method được gọi bằng <code>this.run()</code> trong cùng bean, call có thể bypass proxy và chạy đồng bộ. Repo tách <code>AgentTaskRunner</code> thành bean riêng để call từ controller đi qua proxy.

<code>@EnableAsync</code> ở [JavaBeginApplication.java](../../JavaBeginApplication.java) bật infrastructure async.

---

## 5. Virtual thread

[application.yml](../../../../../../resources/application.yml):

~~~yaml
spring:
  threads:
    virtual:
      enabled: true
~~~

Virtual thread nhẹ hơn platform thread, phù hợp lượng lớn task blocking I/O như chờ HTTP/model/database.

Nó không làm CPU work nhanh hơn. Nếu chạy inference/encode CPU-heavy, số core vẫn là giới hạn.

Nguyên tắc:

- I/O-bound: virtual thread thường phù hợp.
- CPU-bound: giới hạn concurrency theo core.
- Không giữ lock lâu khi blocking.
- Vẫn cần timeout và giới hạn upstream.
- Nhiều virtual thread không biến downstream thành vô hạn.

Nếu model provider chỉ cho 50 request đồng thời, app tạo 100.000 virtual thread vẫn cần semaphore/rate limiter/bulkhead.

---

## 6. Demo worker hiện tại

Worker:

~~~java
store.running(executionId);
try {
    Thread.sleep(Duration.ofMillis(300));
    store.completed(
        executionId,
        "Completed prompt: " + prompt
    );
} catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    throw new IllegalStateException(...);
}
~~~

<code>Thread.sleep</code> chỉ giả lập network latency. Việc restore interrupt flag là đúng.

Khoảng trống quan trọng:

- Exception không chuyển store sang FAILED.
- <code>void @Async</code> không trả Future cho caller theo dõi.
- Chưa có timeout ngoài sleep.
- Chưa có retry.
- Chưa có bounded concurrency.
- Prompt/output có thể nhạy cảm.
- State không bền vững.

Đây là các điểm tốt để chủ động nêu khi demo.

---

## 7. Async exception

Với method <code>@Async void</code>, exception không thể quay lại HTTP request đã kết thúc. Nếu không cấu hình handler/log/state update, lỗi có thể chỉ xuất hiện trong log.

Thiết kế worker nên:

~~~text
mark RUNNING
try
  call provider với timeout
  persist COMPLETED + output
catch known error
  persist FAILED + safe error code
  quyết định retry
finally
  release permit/resource
~~~

Không trả stack trace/internal provider response cho frontend. Lưu error code an toàn và correlation ID.

Nếu dùng <code>CompletableFuture</code>, vẫn phải xử lý exception path; đổi return type không tự giải quyết durability.

---

## 8. In-memory store và ConcurrentHashMap

Store dùng:

~~~java
private final Map<UUID, ExecutionView> executions =
        new ConcurrentHashMap<>();
~~~

<code>ConcurrentHashMap</code> làm các operation map riêng lẻ thread-safe. Store thay cả immutable record khi đổi status, tránh sửa object shared.

Nhưng thread-safe không đồng nghĩa:

- Dữ liệu bền vững.
- Transition hợp lệ.
- Atomic nhiều operation.
- Scale nhiều replicas.
- Có lịch sử/audit.

Nếu hai thread đồng thời cập nhật cùng execution, pattern get-then-put vẫn có thể lost update. Production dùng database optimistic update, compare-and-set/compute hoặc state transition có điều kiện.

---

## 9. Vì sao cần database hoặc queue?

In-memory state mất khi:

- App restart.
- Container bị reschedule.
- Deploy version mới.
- Request poll tới replica khác.

Production thường dùng:

~~~text
API
  ↓ transaction
execution row + outbox message
  ↓
message broker/queue
  ↓
worker
  ↓
database status/output
~~~

Queue đem lại buffering và delivery semantics. Database đem lại durability/query/audit. Outbox pattern giảm khoảng trống “DB commit nhưng publish fail” hoặc ngược lại.

Không nhất thiết mọi app nhỏ cần Kafka. Có thể bắt đầu bằng database-backed job queue, nhưng phải hiểu locking, polling và recovery.

---

## 10. Delivery semantics

Hệ thống phân tán thường đạt at-least-once dễ hơn exactly-once:

- Message có thể được giao lại.
- Worker có thể hoàn tất rồi crash trước ACK.
- Retry tạo lần xử lý thứ hai.

Vì vậy handler phải idempotent hoặc có deduplication.

Exactly-once end-to-end thường là tập hợp transaction/idempotency/deduplication trong một phạm vi được định nghĩa, không phải checkbox miễn phí của broker.

---

## 11. Idempotency

Giả sử execution gửi email hoặc charge payment. Retry mù có thể tạo side effect hai lần.

Thiết kế:

- Client gửi idempotency key cho create execution.
- Server có unique constraint theo owner + key.
- Lần đầu tạo execution.
- Lần sau trả kết quả/execution cũ.
- Worker dùng execution ID làm dedupe key với downstream nếu hỗ trợ.

Idempotency phải bao phủ side effect thực, không chỉ endpoint. Nếu DB đánh COMPLETED nhưng payment đã charge hai lần, API dedupe bên ngoài là chưa đủ.

---

## 12. Timeout

Mọi network call cần timeout có chủ đích:

- Connect timeout.
- Read/response timeout.
- Tổng deadline của job.
- Per-attempt timeout.

Không có timeout đồng nghĩa một task có thể chiếm resource vô thời hạn.

Deadline nên được truyền qua các layer khi có nhiều bước:

~~~text
job deadline 30s
  ├─ model attempt tối đa 20s
  ├─ persistence 2s
  └─ thời gian dự phòng
~~~

Khi timeout, cần biết request downstream có thể vẫn hoàn thành hay không. Idempotency tiếp tục quan trọng.

---

## 13. Retry, backoff và jitter

Chỉ retry lỗi tạm thời:

- Network timeout.
- 429 rate limit.
- Một số 5xx.

Không retry mù:

- Validation error.
- Unauthorized do credential sai.
- Prompt vượt giới hạn cố định.
- Business rule không thể thay đổi.

Exponential backoff:

~~~text
attempt 1: chờ khoảng 1s
attempt 2: khoảng 2s
attempt 3: khoảng 4s
~~~

Thêm jitter để hàng nghìn worker không retry cùng lúc. Tôn trọng <code>Retry-After</code> khi provider trả.

Retry cần:

- Số lần tối đa.
- Tổng deadline.
- Error classification.
- Idempotency.
- Metrics.
- Dead-letter/final failed state.

---

## 14. Dead-letter và replay

Sau số lần retry tối đa, job chuyển FAILED/dead-letter để:

- Không block queue chính.
- Có thể điều tra.
- Có thể replay có kiểm soát.
- Giữ error metadata.

Replay phải kiểm quyền, audit và idempotency. Không cho operator nhấn replay vô hạn một payment/email mà không thấy side effect trước đó.

---

## 15. Backpressure và bounded concurrency

Nếu API nhận 10.000 job/phút nhưng worker xử lý 1.000 job/phút, backlog tăng không giới hạn.

Backpressure gồm:

- Queue có giới hạn.
- Rate limiting.
- Reject/429/503 khi quá tải.
- Concurrency cap theo provider/tenant.
- Priority/fairness.
- Autoscaling dựa trên queue depth.
- TTL/cancel job lỗi thời.

Metrics quan trọng: queue depth, oldest job age, active workers, throughput, retry rate và saturation.

Virtual thread giảm cost thread, không loại bỏ backpressure.

---

## 16. Polling

Repo cung cấp:

~~~http
GET /api/v1/agent/{agentId}/executions/{executionId}
Authorization: Bearer access-token
~~~

Controller:

1. Xác nhận agent thuộc user.
2. Lấy execution.
3. Xác nhận execution thuộc agent.
4. Trả status/output.

Polling dễ triển khai và hợp TanStack Query. Client nên:

- Poll khi PENDING/RUNNING.
- Dừng khi COMPLETED/FAILED.
- Backoff interval.
- Dừng khi tab ẩn nếu phù hợp.
- Có timeout tổng.
- Xử lý 401 refresh.

Polling quá nhanh tạo tải đọc lớn; có thể thêm <code>Retry-After</code>, ETag hoặc interval hợp lý.

---

## 17. SSE và WebSocket

| Cơ chế | Phù hợp |
|---|---|
| Polling | Đơn giản, trạng thái thay đổi không quá dày |
| SSE | Server → client một chiều, progress/token stream |
| WebSocket | Hai chiều realtime, session tương tác |

SSE chạy trên HTTP, tự reconnect và đơn giản hơn WebSocket cho stream một chiều. WebSocket cần quản lý connection, heartbeat, scale/pub-sub và authorization khi kết nối lẫn message.

Không chọn WebSocket chỉ vì “realtime”. Với job 30 giây cập nhật ba lần, polling có thể đủ và vận hành dễ hơn.

---

## 18. Scheduler trong repo

[AgentMaintenanceJob.java](../../agent/AgentMaintenanceJob.java):

~~~java
@Scheduled(fixedDelayString = "PT1M")
void heartbeat() {
    log.debug("Agent scheduler heartbeat");
}
~~~

<code>@EnableScheduling</code> bật scheduler. Job chỉ ghi heartbeat, không tự execute agent để tránh side effect lúc học.

### fixedDelay

Chờ một khoảng sau khi lần chạy trước hoàn thành:

~~~text
run ──finish── wait 1m ── run
~~~

### fixedRate

Cố khởi chạy theo khoảng từ thời điểm bắt đầu; nếu task lâu, behavior overlap phụ thuộc scheduler/executor.

### cron

Chạy theo lịch biểu. Phải chỉ rõ timezone cho business schedule, đặc biệt khi có daylight saving.

---

## 19. Scheduler nhiều replicas

Nếu deploy ba replicas, mỗi instance có scheduler riêng:

~~~text
Replica A → heartbeat/job
Replica B → heartbeat/job
Replica C → heartbeat/job
~~~

Một job có thể chạy ba lần.

Giải pháp tùy yêu cầu:

- Distributed lock.
- Database lease.
- Leader election.
- External scheduler.
- Queue với consumer group.
- Job idempotent.

Distributed lock cũng phải xử lý TTL, crash, clock và fencing token. Không chỉ tạo một boolean “locked”.

---

## 20. Scheduling agent theo user timezone

Nếu user chọn “9:00 mỗi ngày”:

- Lưu timezone IANA như <code>Asia/Ho_Chi_Minh</code>, không chỉ UTC+7.
- Xác định behavior DST cho vùng có DST.
- Tính nextRunAt rõ ràng.
- Claim job atomically.
- Ghi lastRun/nextRun.
- Tránh chạy trùng.
- Quyết định catch-up khi service downtime.

Field <code>scheduleType</code> String hiện tại chỉ là demo. Production nên có enum/config có schema và validation.

---

## 21. Observability: logs

Log tốt là event có cấu trúc:

~~~text
event=agent_execution_completed
executionId=...
agentId=...
durationMs=...
attempt=1
status=COMPLETED
~~~

Nên có:

- Timestamp.
- Level.
- Service/version/environment.
- Request/trace/correlation ID.
- Entity/execution ID an toàn.
- Duration và outcome.

Không log:

- Password.
- Access/refresh token.
- Authorization header.
- Secret/API key.
- Prompt/output nhạy cảm nguyên văn.

Log một exception ở boundary có đủ context; tránh mỗi layer log rồi rethrow tạo trùng.

---

## 22. Metrics

Bốn nhóm hữu ích:

- Rate: request/job mỗi giây.
- Errors: tỷ lệ lỗi theo code/provider.
- Duration: latency percentile p50/p95/p99.
- Saturation: queue, connection pool, thread/executor, memory.

Agent metrics:

- execution accepted/completed/failed.
- queue wait time.
- execution duration.
- retry count.
- provider 429/5xx.
- polling request rate.
- jobs stuck RUNNING.

Average latency có thể che tail latency; production quan tâm percentile.

---

## 23. Distributed tracing

Trace nối các span:

~~~text
HTTP POST execution
  → DB insert
  → queue publish
  → worker consume
  → model HTTP call
  → DB update
~~~

Async boundary cần truyền trace context qua message metadata. Trace giúp biết latency nằm ở queue, provider hay database.

Không đưa dữ liệu nhạy cảm vào span attributes.

---

## 24. Health checks

Phân biệt:

- Liveness: process có bị kẹt và cần restart không?
- Readiness: instance có sẵn sàng nhận traffic không?
- Startup: app khởi động chậm đã hoàn tất chưa?

Readiness có thể xét database/critical dependency, nhưng không nên gọi mọi third-party đắt tiền mỗi lần probe. Liveness không nên fail chỉ vì provider ngoài tạm down, nếu không orchestrator tạo restart storm.

Repo chưa thêm Actuator/health endpoint trong dependency hiện tại; đây là production gap cần nêu.

---

## 25. Graceful shutdown

Khi deploy:

1. Instance bị loại khỏi load balancer/readiness.
2. Dừng nhận request/job mới.
3. Chờ request/job đang chạy trong grace period.
4. ACK/rollback/requeue job phù hợp.
5. Đóng connection.
6. Process thoát.

In-memory execution đang chạy sẽ mất khi container dừng. Durable queue/state là điều kiện quan trọng để recovery.

---

## 26. Dockerfile hiện tại

[Dockerfile](../../../../../../../../Dockerfile) dùng multi-stage:

~~~dockerfile
FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/...jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
~~~

Multi-stage giữ Maven/source khỏi runtime image. Copy pom trước giúp cache dependency layer khi source đổi.

Lưu ý: image build đang skip test. CI phải chạy test ở bước riêng trước khi publish image.

---

## 27. Docker production gaps

Image hiện chạy được nhưng chưa đầy đủ production hardening:

- Chưa tạo non-root user.
- Chưa có health check.
- Base image chưa pin digest.
- Chưa scan CVE/SBOM/sign image.
- Chưa tối ưu layered jar/jlink.
- Chưa đặt JVM/container resource policy rõ.
- Chưa có read-only filesystem.
- Chưa cấu hình graceful shutdown.
- Chưa có labels/version metadata.

Không nhét secret vào Dockerfile/image layer. Secret đi qua runtime secret mechanism.

---

## 28. Compose hiện tại

[compose.yml](../../../../../../../../compose.yml):

~~~yaml
services:
  java-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      APP_JWT_SECRET: "compose-local-secret..."
~~~

Spring relaxed binding map <code>APP_JWT_SECRET</code> tới <code>app.jwt.secret</code>.

File phù hợp local demo, nhưng production không:

- Commit secret thật.
- Dùng H2 memory.
- Chỉ chạy một service không health/dependency.
- Không resource limit.
- Không persistent volume/database.

---

## 29. Chạy local bằng Maven

~~~powershell
./mvnw.cmd clean test
./mvnw.cmd spring-boot:run
~~~

Chờ log:

~~~text
Started JavaBeginApplication
~~~

Sau đó dùng [requests.http](../../../../../../../../requests.http) hoặc [Postman](../../../../../../../../postman/README.md).

---

## 30. Chạy bằng Docker

Từ root:

~~~powershell
docker compose build
docker compose up
~~~

Kiểm:

~~~text
http://localhost:8080
~~~

Khi kết thúc:

~~~powershell
docker compose down
~~~

H2 in-memory mất dữ liệu khi container dừng; đó là behavior mong đợi của lab.

---

## 31. API flow agent

### Tạo agent

~~~http
POST /api/v1/agent
Authorization: Bearer access-token
Content-Type: application/json

{
  "name": "Daily Java Coach",
  "description": "Creates interview questions",
  "task_prompt": "Create one Java question",
  "schedule_type": "DAILY"
}
~~~

### Execute

~~~http
POST /api/v1/agent/1/executions
Authorization: Bearer access-token
~~~

Kỳ vọng 202 và lưu <code>executionId</code>.

### Poll

~~~http
GET /api/v1/agent/1/executions/{executionId}
Authorization: Bearer access-token
~~~

Poll tới khi COMPLETED. Controller so sánh <code>execution.agentId()</code> với agent
trên URL; service kiểm agent đó thuộc principal. Store chỉ tra execution theo UUID.

---

## 32. Capstone end-to-end

Demo theo một câu chuyện liền mạch:

1. Login USER, giải thích BCrypt/JWT/access-refresh.
2. Tạo conversation, giải thích DTO/validation/201.
3. Gửi chat, giải thích transaction tăng turn count và tạo turn.
4. List/history, giải thích ownership/pagination.
5. Tạo agent.
6. Execute agent, giải thích 202/async/proxy/virtual thread.
7. Poll execution tới COMPLETED.
8. USER gọi admin endpoint và nhận 403.
9. Login ADMIN rồi gọi lại nhận 200.
10. Chỉ ra H2/in-memory store là demo, không che giấu giới hạn.

Mục tiêu không phải click nhanh; mỗi bước phải nói được data flow và trade-off.

---

## 33. Architecture để vẽ khi phỏng vấn

~~~text
SoftAIBox React
  ↓ REST/JWT
Security Filter Chain
  ↓
Controllers
  ↓
Services / transaction
  ├─ JPA repositories → relational DB
  └─ execution submit → durable queue
                         ↓
                       workers
                         ↓ model/provider
                         ↓
                     execution DB

Observability: logs + metrics + traces
Config/secrets: environment + secret manager
~~~

Nói rõ repo lab chưa có queue/execution DB; sơ đồ bên phải là hướng production.

---

## 34. Production readiness checklist

### Data

- PostgreSQL/MySQL production thay H2.
- Flyway/Liquibase migration.
- Constraint/index.
- Backup/restore test.
- Connection pool sizing.
- Retention/cleanup.

### Async

- Durable execution table.
- Queue/outbox.
- Idempotency.
- Timeout/retry/backoff/jitter.
- Dead-letter/replay.
- Cancellation.
- Concurrency/rate limits.

### Security

- Secret manager/rotation.
- Issuer/audience validation.
- Durable hashed refresh sessions.
- Logout/revoke/reuse detection.
- Rate limiting/audit.
- H2 console tắt.
- Least privilege.

### API

- OpenAPI/contract tests.
- Stable error codes.
- Request ID.
- Pagination limits.
- 409 mappings.
- Payload/body size limits.

### Operations

- Structured logs.
- Metrics/alerts/traces.
- Health probes.
- Graceful shutdown.
- Non-root image.
- CVE scanning/SBOM.
- Resource limits/autoscaling.
- Runbook/incident response.

---

## 35. Failure-mode thinking

Hãy hỏi “nếu crash đúng giữa hai dòng thì sao?”:

| Điểm lỗi | Câu hỏi |
|---|---|
| Sau tạo execution, trước submit | Ai nhặt PENDING job? |
| Provider hoàn thành, trước DB update | Retry có tạo side effect trùng? |
| DB update xong, trước queue ACK | Message giao lại có idempotent? |
| Deploy khi RUNNING | Job được resume/requeue thế nào? |
| Poll vào replica khác | State có dùng chung không? |
| Scheduler chạy ba replicas | Làm sao chỉ claim một job? |
| Provider 429 | Backoff/rate limit ra sao? |

Đây là tư duy production quan trọng hơn thuộc annotation.

---

## 36. Những lỗi thường gặp

### Cho mọi việc vào <code>@Async</code>

Async không tự tạo durability, retry, tracing hay backpressure.

### Trả 200 “completed” trước khi hoàn thành

Sai contract. Trả 202 cùng execution ID/status URL.

### Dùng unbounded executor/queue

Load tăng có thể làm memory cạn và downstream sập.

### Retry mọi exception

Validation/auth lỗi không tự biến mất; retry làm tốn tài nguyên.

### Retry side effect không idempotent

Có thể charge/send nhiều lần.

### Để execution chỉ trong memory

Restart/multi-replica làm mất hoặc không tìm thấy state.

### Scheduler trong mọi replica

Job bị chạy trùng nếu không có coordination/idempotency.

### Log prompt/token

Rò PII, secret và dữ liệu doanh nghiệp.

### Docker chạy root

Tăng impact nếu process/container bị compromise.

### Health check phụ thuộc mọi provider

Một provider chập chờn có thể gây restart storm.

---

## 37. Bài thực hành

### Bài 1 — Failure state

Thiết kế method <code>failed(id, errorCode)</code> và bảo đảm worker chuyển FAILED khi exception. Không lưu raw stack trace trong API view.

### Bài 2 — Timeout/retry policy

Lập bảng error của model provider: 400, 401, 429, 500, connect timeout, read timeout. Đánh dấu retry/no retry, delay và số lần tối đa.

### Bài 3 — Idempotency

Thiết kế header <code>Idempotency-Key</code>, unique constraint và response khi client gửi lại cùng key.

### Bài 4 — Durable schema

Thiết kế <code>agent_executions</code> gồm status, attempt, timestamps, safe error, idempotency key, version. Viết index cho polling và worker claim.

### Bài 5 — Multi-replica scheduler

So sánh database lease, distributed lock và external scheduler. Chọn một cho quy mô nhỏ và giải thích recovery khi worker chết.

### Bài 6 — Polling

Viết pseudo-code TanStack Query: poll 1 giây khi PENDING/RUNNING, dừng terminal, backoff sau lỗi và refresh khi 401.

### Bài 7 — Docker review

Review Dockerfile theo non-root, test gate, digest, health, secret, layer cache và signal/graceful shutdown. Không sửa chỉ để “đẹp”; giải thích tác động.

### Bài 8 — Chaos tabletop

Mô phỏng provider timeout, DB down, app restart, duplicate message và expired JWT. Với mỗi case, ghi expected status, retry owner, dữ liệu cần bền và alert.

### Bài 9 — Video demo

Quay video 8 phút: 2 phút kiến trúc, 4 phút demo, 2 phút production gaps/trade-off. Xem lại và bỏ các câu chỉ đọc annotation.

---

## 38. Câu hỏi phỏng vấn và đáp án ngắn

### Khi nào trả 202?

Khi request được chấp nhận nhưng xử lý cuối chưa hoàn tất; trả ID/location để theo dõi.

### <code>@Async</code> hoạt động thế nào?

Spring proxy submit lời gọi bean vào executor; self-invocation có thể bypass proxy.

### Virtual thread có làm CPU task nhanh hơn?

Không. Nó giúp scale blocking I/O, còn CPU vẫn giới hạn bởi core.

### ConcurrentHashMap có làm job durable?

Không. Nó chỉ hỗ trợ concurrent access trong một process.

### Retry cần điều kiện gì?

Lỗi transient, giới hạn attempt/deadline, backoff+jitter, idempotency và observability.

### At-least-once nghĩa là gì?

Message có thể được giao lại; consumer phải chịu duplicate.

### Polling, SSE, WebSocket chọn thế nào?

Polling đơn giản; SSE cho server-to-client stream; WebSocket cho hai chiều realtime.

### fixedDelay khác fixedRate?

Fixed delay tính khoảng chờ sau khi lần trước hoàn thành; fixed rate theo mốc bắt đầu định kỳ.

### Vì sao scheduler chạy trùng?

Mỗi app replica có scheduler riêng; cần coordination hoặc job idempotent.

### Backpressure là gì?

Khả năng giới hạn/điều tiết intake khi downstream xử lý chậm hơn nguồn.

### Ba trụ observability?

Logs, metrics và distributed traces; bổ sung profiling khi cần.

### Multi-stage Docker build giúp gì?

Tách build tool/source khỏi runtime image, thường giảm kích thước và attack surface.

### Liveness khác readiness?

Liveness nói process có sống; readiness nói instance có sẵn sàng nhận traffic.

---

## 39. Cách trả lời phỏng vấn trung thực

Cấu trúc tốt:

1. Nêu quyết định.
2. Nêu lý do.
3. Nêu trade-off.
4. Nêu rủi ro production.
5. Nêu cách kiểm chứng/đo lường.

Ví dụ:

> Demo dùng ConcurrentHashMap để tập trung vào async flow. Nó thread-safe trong một process nhưng mất state khi restart và không scale nhiều replicas. Production em sẽ lưu execution trong database, publish qua outbox/queue, xử lý idempotent và đo queue age/failure rate.

Nếu chưa dùng công nghệ production, nói rõ. Hiểu boundary và cách kiểm chứng tốt hơn bịa kinh nghiệm.

---

## 40. Lịch học ngày cuối

### 60 phút

Đọc async flow, state machine, proxy và virtual thread. Vẽ sequence 202/polling.

### 60 phút

Học timeout, retry, idempotency, queue và backpressure. Làm failure table.

### 45 phút

Học scheduler/multi-replica/timezone.

### 60 phút

Đọc Dockerfile/compose và làm production checklist.

### 60–90 phút

Chạy Postman end-to-end, ghi lại lỗi và production gaps.

### 45 phút

Vẽ architecture trong 10 phút, quay demo và tự mock interview.

---

## 41. Definition of Done ngày 14

Bạn hoàn thành ngày 14 khi có thể:

- Giải thích vì sao job lâu trả 202.
- Vẽ state machine execution và terminal states.
- Theo code controller → store → async worker → polling.
- Giải thích proxy/self-invocation và virtual thread.
- Chỉ ra lỗi async hiện chưa ghi FAILED.
- Phân biệt thread safety với durability.
- Thiết kế timeout, retry, backoff, jitter và dead-letter.
- Giải thích idempotency/at-least-once.
- Chọn polling/SSE/WebSocket theo use case.
- Giải thích scheduler multi-replica.
- Nêu metric/log/trace cho agent.
- Đọc Dockerfile multi-stage và chỉ ra hardening gaps.
- Chạy capstone login → chat → agent → RBAC.
- Nêu ít nhất mười điểm chưa production-ready và cách cải thiện.
- Trình bày kiến trúc/trade-off trong 8 phút.

---

## Definition of Done toàn bộ 14 ngày

Sau sprint, bạn cần:

- Viết Java core: type, OOP, collection, exception, stream, concurrency.
- Dùng Maven/JUnit và đọc stack trace.
- Hiểu entity lifecycle, transaction, locking, index và N+1.
- Theo request qua Spring Security/MVC/service/JPA/database.
- Thiết kế REST DTO, validation, pagination và error contract.
- Giải thích JWT, refresh, RBAC, CORS và CSRF.
- Thiết kế async workflow có failure/retry/idempotency.
- Demo mini backend và nói rõ giới hạn.
- Giải 15–20 bài easy array/string/hash map và một số bài medium.
- Trả lời bằng reasoning/trade-off thay vì học thuộc annotation.

Nếu một mục chưa làm được, quay lại ngày tương ứng. Hoàn thành lịch không quan trọng bằng tự viết, debug và giải thích được.
