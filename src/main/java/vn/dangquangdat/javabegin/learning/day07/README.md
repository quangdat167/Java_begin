# Ngày 7 — Concurrency, thread safety và virtual thread

Ngày 7 giúp bạn hiểu điều gì xảy ra khi nhiều tác vụ cùng tiến triển. Đây là nền tảng của web backend: nhiều request, truy vấn database và lời gọi model có thể đang chờ I/O cùng lúc.

Code trong cùng thư mục:

- [ConcurrencyDemo.java](./ConcurrencyDemo.java): chạy ba lời gọi model giả lập bằng virtual thread, `Callable`, `Future` và `ExecutorService`.

## Mục tiêu

Sau ngày 7, bạn cần:

1. Phân biệt concurrency và parallelism.
2. Hiểu process, platform thread và virtual thread.
3. Giải thích race condition bằng `count++`.
4. Phân biệt atomicity, visibility và ordering.
5. Biết khi nào dùng immutable data, confinement, `synchronized`, atomics.
6. Hiểu `volatile` làm được gì và không làm được gì.
7. Quản lý task bằng `ExecutorService`.
8. Dùng `Runnable`, `Callable`, `Future`.
9. Xử lý interruption, exception, timeout và shutdown.
10. Giải thích virtual thread phù hợp blocking I/O, không làm CPU work nhanh hơn.
11. Nhận diện deadlock và vấn đề khi giữ lock trong I/O.
12. Liên hệ Java lock với database/distributed coordination.

---

## 1. Concurrency khác parallelism

**Concurrency** nghĩa là nhiều task cùng tiến triển trong một khoảng thời gian. Chúng có thể xen kẽ trên một CPU core.

**Parallelism** nghĩa là nhiều task thực sự chạy đồng thời, thường trên nhiều core.

```text
Concurrency trên một core:
task A: run ---- wait -------- run
task B: ---- run ---- wait run ----

Parallelism trên hai core:
core 1: task A ====================
core 2: task B ====================
```

Web server cần concurrency vì request thường dành nhiều thời gian chờ:

- Database trả kết quả.
- HTTP API hoặc AI model phản hồi.
- File/object storage hoàn tất I/O.

Trong lúc task A chờ, runtime có thể cho task B tiến triển.

## 2. Process và thread

Process là chương trình đang chạy, có vùng nhớ và tài nguyên riêng. Thread là đơn vị thực thi bên trong process.

Các thread cùng process chia sẻ:

- Heap và object.
- Static field.
- File/socket được chia sẻ.

Mỗi thread có stack/call stack riêng. Việc chia sẻ heap giúp giao tiếp nhanh nhưng cũng tạo race condition nếu nhiều thread sửa cùng state.

Không nên hiểu “mỗi request luôn có đúng một OS thread” như chân lý. Mô hình phụ thuộc server, executor và việc dùng platform hay virtual thread.

---

## 3. Race condition

Race condition xảy ra khi kết quả phụ thuộc thứ tự/timing của các thread.

```java
class Counter {
    private int value;

    void increment() {
        value++;
    }
}
```

`value++` không phải một thao tác nguyên tử. Có thể hình dung thành:

```text
1. đọc value
2. cộng 1
3. ghi value mới
```

Hai thread có thể cùng đọc `0`, cùng tính `1`, rồi cùng ghi `1`. Hai lần increment nhưng kết quả chỉ là `1`: đây là **lost update**.

Race condition có thể hiếm trên máy dev và xuất hiện khi production có tải. Chạy đúng một lần không chứng minh code thread-safe.

## 4. Atomicity, visibility và ordering

Ba khái niệm thường bị trộn lẫn:

### Atomicity

Một thao tác được quan sát như một khối không bị xen giữa.

```java
counter.incrementAndGet();
```

### Visibility

Khi thread A ghi dữ liệu, thread B có nhìn thấy giá trị mới đúng lúc không.

### Ordering

Compiler, CPU và JVM có thể reorder một số thao tác nếu vẫn giữ semantics của một thread. Đồng bộ tạo quan hệ happens-before để các thread có cách quan sát nhất quán.

Một giải pháp chỉ bảo đảm visibility chưa chắc bảo đảm atomicity cho chuỗi read-modify-write.

## 5. Các chiến lược thread safety

Ưu tiên giảm chia sẻ mutable state trước khi thêm lock.

### Immutable data

```java
record ModelAnswer(
        String model,
        String answer,
        long latencyMs
) {}
```

Record trên chứa component immutable nên có thể chia sẻ an toàn sau khi tạo. Record chỉ shallowly immutable nếu component là list/object mutable.

### Thread confinement

State chỉ thuộc một task/thread:

```java
Callable<ModelAnswer> task = () -> {
    String local = "temporary";
    return callModel(local, 100);
};
```

Không thread nào khác truy cập `local`.

### `synchronized`

```java
class Counter {
    private int value;

    synchronized void increment() {
        value++;
    }

    synchronized int get() {
        return value;
    }
}
```

Một thread giữ monitor thì thread khác phải chờ cùng monitor.

- Instance synchronized method khóa `this`.
- Static synchronized method khóa object `Counter.class`.
- Hai instance khác nhau có hai lock khác nhau.

```java
synchronized (lockObject) {
    // critical section
}
```

Lock phải bảo vệ **tất cả** truy cập liên quan đến invariant, không chỉ một setter.

### Atomic class

```java
AtomicInteger counter = new AtomicInteger();
counter.incrementAndGet();
```

Phù hợp với counter và update đơn giản. Nhiều atomic variable riêng không tự tạo một transaction nguyên tử cho invariant giữa chúng.

### Explicit lock

`ReentrantLock` hỗ trợ `tryLock`, timeout, interruptible lock và nhiều condition, nhưng phải `unlock` trong `finally`:

```java
lock.lock();
try {
    updateState();
} finally {
    lock.unlock();
}
```

Chỉ dùng độ phức tạp này khi tính năng bổ sung thực sự cần thiết.

---

## 6. `volatile`

```java
private volatile boolean stopped;
```

`volatile` giúp:

- Write của một thread visible với thread đọc sau.
- Tạo ordering/happens-before quanh biến đó.

`volatile` không làm chuỗi thao tác thành atomic:

```java
private volatile int count;

void increment() {
    count++; // vẫn không atomic
}
```

Use case phù hợp là cờ trạng thái độc lập:

```java
while (!stopped) {
    doOneUnit();
}
```

Nếu việc chuyển state gồm nhiều field hoặc read-modify-write, cần thiết kế đồng bộ khác.

## 7. `Runnable`, `Callable` và task

`Runnable` không trả kết quả và không khai báo checked exception:

```java
Runnable task = () -> System.out.println("running");
```

`Callable<T>` trả kết quả và có thể ném exception:

```java
Callable<ModelAnswer> task =
        () -> callModel("model-a", 120);
```

Trong `ConcurrencyDemo`, mỗi lambda được target thành `Callable<ModelAnswer>`:

```java
List<Callable<ModelAnswer>> calls = List.of(
        () -> callModel("model-a", 120),
        () -> callModel("model-b", 80),
        () -> callModel("model-c", 100)
);
```

Task là mô tả công việc; executor quyết định thread nào thực thi.

## 8. Vì sao dùng `ExecutorService`

Tự tạo thread:

```java
new Thread(task).start();
```

khó kiểm soát lifecycle và số lượng thread khi request tăng.

`ExecutorService` tách việc submit task khỏi quản lý worker:

```java
try (ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor()) {
    Future<ModelAnswer> future = executor.submit(task);
}
```

Executor cung cấp:

- Submit task.
- Quản lý worker/lifecycle.
- Chờ kết quả.
- Hủy task.
- Shutdown có chủ đích.

Với fixed platform-thread pool:

```java
Executors.newFixedThreadPool(8);
```

task dư thường chờ trong queue. Queue không giới hạn có thể làm tăng memory và latency; production cần capacity/backpressure phù hợp.

---

## 9. `Future`

`Future<T>` đại diện kết quả có thể xuất hiện sau:

```java
Future<ModelAnswer> future = executor.submit(task);
ModelAnswer answer = future.get();
```

`get()` block thread hiện tại cho đến khi:

- Task hoàn thành.
- Task ném exception.
- Thread chờ bị interrupt.

Timeout:

```java
future.get(500, TimeUnit.MILLISECONDS);
```

Có thể gặp:

- `TimeoutException`: chưa xong trong thời gian chờ.
- `InterruptedException`: thread chờ bị interrupt.
- `ExecutionException`: task thất bại; nguyên nhân thật ở `getCause()`.
- `CancellationException`: task đã bị cancel.

Hủy:

```java
future.cancel(true);
```

`true` yêu cầu interrupt task đang chạy, nhưng code task phải hợp tác với interruption; đây không phải “kill thread” chắc chắn.

## 10. Interruption

Interruption là tín hiệu hợp tác yêu cầu thread dừng hoặc đổi hành vi.

Nếu bắt `InterruptedException` mà không thể ném tiếp, thường cần restore flag:

```java
try {
    Thread.sleep(Duration.ofMillis(100));
} catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    return;
}
```

Không được nuốt interruption rồi tiếp tục vô hạn:

```java
// catch (InterruptedException ignored) {}
```

Một task dài nên kiểm tra:

```java
if (Thread.currentThread().isInterrupted()) {
    return;
}
```

## 11. Virtual thread

Virtual thread là thread nhẹ do JVM quản lý. Có thể tạo rất nhiều virtual thread hơn platform thread trong workload phù hợp.

```java
try (ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor()) {
    // thường một virtual thread cho mỗi task
}
```

Phù hợp với blocking I/O:

- HTTP call.
- JDBC.
- Chờ socket.
- Chờ file.

Khi virtual thread block ở điểm runtime hỗ trợ, platform thread có thể phục vụ virtual thread khác.

Virtual thread **không**:

- Làm thuật toán CPU-bound nhanh hơn.
- Xóa nhu cầu timeout/cancellation.
- Làm shared mutable state thread-safe.
- Xóa giới hạn connection pool/database/API.

Một triệu virtual thread không tạo được một triệu database connection. Tài nguyên downstream vẫn cần semaphore, pool, rate limit và backpressure.

Tránh giữ lock lâu khi gọi I/O:

```java
synchronized (lock) {
    callRemoteModel(); // giữ lock trong lúc chờ: rất tệ
}
```

Hãy thu critical section, tách state transition khỏi network call.

---

## 12. Phân tích `ConcurrencyDemo`

Ba task giả lập model:

```java
() -> callModel("model-a", 120)
() -> callModel("model-b", 80)
() -> callModel("model-c", 100)
```

`callModel` dùng:

```java
Thread.sleep(Duration.ofMillis(latencyMs));
```

Đây là I/O giả lập, không phải CPU work. Sleep không tiêu thụ CPU liên tục.

Chạy task:

```java
List<Future<ModelAnswer>> futures =
        executor.invokeAll(calls);
```

`invokeAll`:

- Submit toàn bộ task.
- Chờ tất cả hoàn tất, trừ khi bị interrupt.
- Trả danh sách future theo thứ tự task đầu vào.

Vì vậy vòng lặp in kết quả theo `model-a`, `model-b`, `model-c`, dù `model-b` hoàn thành sớm nhất:

```java
for (Future<ModelAnswer> future : futures) {
    System.out.println(future.get());
}
```

Concurrency làm tổng thời gian gần task chậm nhất cộng overhead, thay vì tổng `120 + 80 + 100`, nhưng không nên assert timing quá chặt vì scheduler và máy chạy thay đổi.

`try-with-resources` đóng executor. Với `ExecutorService` hiện đại, `close()` thực hiện shutdown và chờ termination theo contract.

## 13. `CompletableFuture`

`CompletableFuture` hỗ trợ pipeline:

```java
CompletableFuture<ModelAnswer> future =
        CompletableFuture.supplyAsync(
                () -> callModelUnchecked("model-a", 120),
                executor
        );

CompletableFuture<String> text =
        future.thenApply(ModelAnswer::answer);
```

Kết hợp:

```java
first.thenCombine(second, this::chooseBetter);
```

Xử lý lỗi:

```java
future.exceptionally(error -> fallbackAnswer());
```

Nó mạnh nhưng pipeline async dễ khó đọc nếu timeout, executor và error path không rõ. Đừng dùng chỉ để “trông bất đồng bộ”.

## 14. Deadlock

Deadlock có thể xảy ra:

```text
thread A giữ lock 1, chờ lock 2
thread B giữ lock 2, chờ lock 1
```

Bốn điều kiện kinh điển:

1. Mutual exclusion.
2. Hold and wait.
3. No preemption.
4. Circular wait.

Giảm rủi ro bằng:

- Một thứ tự lấy lock thống nhất.
- Critical section nhỏ.
- Không gọi network/DB khi giữ lock.
- `tryLock` với timeout khi phù hợp.
- Tránh thiết kế nhiều lock phụ thuộc.

Ngoài deadlock còn có:

- Starvation: task lâu không được tài nguyên.
- Livelock: các thread hoạt động nhưng không tiến triển.
- Thread leak: tạo task/thread mà không quản lifecycle.

## 15. Java lock, transaction và nhiều instance

`synchronized` chỉ phối hợp thread trong **một JVM** và trên cùng lock object.

Khi có nhiều app replica:

```text
instance A: lock riêng
instance B: lock riêng
```

Hai lock không biết nhau. Rule như email unique cần database constraint. Lost update database có thể cần optimistic/pessimistic locking hoặc atomic SQL.

Database transaction không thay Java lock:

- Transaction bảo vệ consistency của dữ liệu database theo isolation.
- Java synchronization bảo vệ shared in-memory state.

Distributed job có thể cần queue, unique key, leader election hoặc distributed lock. Chọn theo invariant, không theo từ khóa quen thuộc.

---

## 16. Liên hệ SoftAIBox

### Gọi nhiều model

Virtual thread hợp với nhiều lời gọi model blocking độc lập, nhưng vẫn phải có:

- Connect/read timeout.
- Giới hạn concurrent request theo provider.
- Retry có backoff cho lỗi transient.
- Cancellation khi user hủy.
- Metric latency và error.

### Agent execution

Không giữ HTTP request cho job dài. API có thể trả `202 Accepted + executionId`, worker xử lý, UI poll/SSE nhận trạng thái.

### State demo và production

`ConcurrentHashMap` có thể thread-safe cho operation đơn lẻ nhưng:

- Không tồn tại sau restart.
- Không chia sẻ giữa replica.
- Chuỗi nhiều operation chưa tự atomic.

Production cần persistent job store/queue và state transition có điều kiện.

### Idempotency

Thread-safe không đồng nghĩa idempotent. Hai webhook Stripe có thể chạy tuần tự hoàn hảo nhưng vẫn tạo hai lần side effect nếu không deduplicate event ID.

## 17. Lỗi thường gặp

- Nghĩ `count++` atomic.
- Dùng `volatile` để sửa lost update.
- Lock một instance nhưng state là static hoặc nằm ở instance khác.
- Giữ lock khi gọi HTTP/database.
- Quên shutdown executor.
- Gọi `Future.get()` không timeout ở boundary quan trọng.
- Nuốt `InterruptedException`.
- Tạo executor mới cho từng thao tác nhỏ mà không quản lifecycle.
- Cho rằng collection concurrent làm toàn bộ use case atomic.
- Dùng virtual thread cho CPU-bound rồi kỳ vọng nhanh hơn.
- Quên giới hạn downstream dù virtual thread rất nhẹ.
- Dùng sleep-based test với timing quá chặt.

## 18. Bài thực hành

1. Chạy 10.000 lần `counter++` từ nhiều task và quan sát lost update.
2. Sửa bằng `synchronized`, sau đó bằng `AtomicInteger`.
3. Viết ví dụ chứng minh `volatile int` vẫn mất update.
4. Thêm timeout cho ba model.
5. Chọn câu trả lời hoàn thành đầu tiên và cancel task còn lại.
6. Cho một model ném exception; đọc `ExecutionException.getCause()`.
7. Interrupt một task đang sleep và xử lý đúng interrupt flag.
8. Tạo deadlock có chủ đích trong demo riêng, lấy thread dump và giải thích.
9. So sánh thời gian sequential và concurrent, không assert mốc quá chặt.
10. Thiết kế idempotency cho webhook bằng event ID unique.

## 19. Câu hỏi phỏng vấn và đáp án ngắn

### Concurrency khác parallelism?

Concurrency là nhiều task cùng tiến triển; parallelism là nhiều task thực sự chạy đồng thời.

### Vì sao `count++` không thread-safe?

Nó là read-modify-write gồm nhiều bước; thread có thể xen kẽ và gây lost update.

### `volatile` bảo đảm gì?

Visibility và ordering quanh biến; không làm chuỗi `count++` thành atomic.

### `synchronized` instance và static khác gì?

Instance method khóa `this`; static method khóa object `Class`, nên phạm vi lock khác nhau.

### `Runnable` khác `Callable`?

`Runnable` không trả kết quả và không khai checked exception; `Callable<T>` trả `T` và có thể ném exception.

### `Future.get()` làm gì?

Chờ kết quả; có thể bị interrupt, timeout hoặc bọc lỗi task trong `ExecutionException`.

### Virtual thread phù hợp việc gì?

Nhiều task blocking I/O. Nó không tăng số core và không làm CPU-bound work tự nhanh hơn.

### Deadlock cần điều kiện gì?

Mutual exclusion, hold-and-wait, no preemption và circular wait.

### Java lock có bảo vệ nhiều app instance không?

Không. Lock trong JVM không phối hợp replica khác; cần database hoặc distributed coordination.

---

## 20. Cách chạy

Từ thư mục gốc:

```powershell
java .\src\main\java\vn\dangquangdat\javabegin\learning\day07\ConcurrencyDemo.java
```

Kết quả có ba `ModelAnswer`, được in theo thứ tự danh sách task:

```text
ModelAnswer[model=model-a, answer=answer from model-a, latencyMs=120]
ModelAnswer[model=model-b, answer=answer from model-b, latencyMs=80]
ModelAnswer[model=model-c, answer=answer from model-c, latencyMs=100]
```

Biên dịch/test dự án:

```powershell
.\mvnw.cmd compile
.\mvnw.cmd test
```

Đặt breakpoint trong `callModel`, xem tên/loại thread và Call Stack. Kết quả xen kẽ khi debug có thể khác chạy bình thường; debugger làm thay đổi timing.

## Definition of done

Bạn hoàn thành ngày 7 khi có thể:

- Giải thích concurrency/parallelism và ba thuộc tính atomicity/visibility/ordering.
- Tạo rồi sửa race condition.
- Dùng đúng `synchronized`, `volatile`, `AtomicInteger`.
- Dùng `ExecutorService`, `Callable`, `Future`.
- Xử lý timeout, exception, cancellation và interruption.
- Giải thích virtual thread cho I/O và giới hạn downstream.
- Nhận diện deadlock, thu nhỏ critical section.
- Phân biệt Java lock, transaction và distributed coordination.
- Tự viết lại `ConcurrencyDemo`.
- Áp dụng vào model call, agent execution và webhook của SoftAIBox.
