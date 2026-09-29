# Ngày 6 — Lambda, functional interface và Stream API

Ngày 6 chuyển từ cách “ra lệnh từng bước” bằng vòng lặp sang cách mô tả pipeline xử lý dữ liệu. Mục tiêu không phải thay mọi vòng `for` bằng stream, mà là biết chọn cách rõ ràng, đúng và dễ bảo trì nhất.

Code trong cùng thư mục:

- [StreamsDemo.java](./StreamsDemo.java): lọc, sắp xếp, biến đổi, loại trùng và nhóm lịch sử chạy agent.

## Mục tiêu

Sau ngày 6, bạn cần:

1. Giải thích functional interface và lambda.
2. Dùng `Predicate`, `Function`, `Consumer`, `Supplier`.
3. Hiểu lambda chỉ capture biến local `final` hoặc effectively final.
4. Phân biệt collection và stream.
5. Phân biệt intermediate và terminal operation.
6. Giải thích lazy evaluation và stream chỉ dùng một lần.
7. Dùng `filter`, `map`, `flatMap`, `sorted`, `distinct`.
8. Dùng `reduce`, `collect`, `groupingBy` và primitive stream.
9. Xử lý `Optional` an toàn.
10. Nhận ra side effect và giới hạn của `parallelStream()`.

---

## 1. Functional interface và lambda

Functional interface là interface có đúng **một abstract method**:

```java
@FunctionalInterface
interface ExecutionRule {
    boolean test(AgentExecution execution);
}
```

Có thể tạo implementation bằng anonymous class:

```java
ExecutionRule successful = new ExecutionRule() {
    @Override
    public boolean test(AgentExecution execution) {
        return execution.status() == Status.SUCCESS;
    }
};
```

Lambda là cách viết gọn cho cùng contract:

```java
ExecutionRule successful =
        execution -> execution.status() == Status.SUCCESS;
```

Lambda Java không phải “function tự do”. Nó luôn cần một **target type** là functional interface. Vì vậy đoạn sau thiếu thông tin kiểu:

```java
// var rule = execution -> execution.isSuccessful(); // compile error
```

Annotation `@FunctionalInterface` không bắt buộc, nhưng giúp compiler bảo vệ ý định thiết kế. Interface vẫn có thể chứa nhiều `default`, `static` hoặc private method; chỉ số abstract method phải là một.

## 2. Bốn interface cần nhớ

| Interface | Method | Ý nghĩa |
|---|---|---|
| `Predicate<T>` | `boolean test(T)` | Kiểm tra điều kiện |
| `Function<T,R>` | `R apply(T)` | Biến đổi `T` thành `R` |
| `Consumer<T>` | `void accept(T)` | Thực hiện hành động |
| `Supplier<T>` | `T get()` | Cung cấp giá trị |

```java
Predicate<AgentExecution> isSuccess =
        item -> item.status() == Status.SUCCESS;

Function<AgentExecution, String> getName =
        item -> item.agentName();

Consumer<String> print = value -> System.out.println(value);

Supplier<Instant> now = () -> Instant.now();
```

Một số biến thể:

- `UnaryOperator<T>`: nhận `T`, trả `T`.
- `BinaryOperator<T>`: nhận hai `T`, trả `T`.
- `BiFunction<T,U,R>`: nhận hai input, trả `R`.
- `IntPredicate`, `ToLongFunction<T>`: tránh boxing cho primitive.

## 3. Cú pháp lambda và method reference

Lambda có dạng:

```text
(parameters) -> expression hoặc block
```

Một parameter có thể bỏ ngoặc:

```java
item -> item.durationMs() >= 100
```

Nhiều parameter phải có ngoặc:

```java
(left, right) ->
        Long.compare(left.durationMs(), right.durationMs())
```

Block nhiều statement cần `return` nếu contract yêu cầu kết quả:

```java
item -> {
    long seconds = item.durationMs() / 1_000;
    return seconds >= 1;
}
```

Khi lambda chỉ gọi method có sẵn, có thể dùng method reference:

```java
item -> item.agentName()
AgentExecution::agentName

value -> Long.parseLong(value)
Long::parseLong

value -> System.out.println(value)
System.out::println
```

Method reference không mặc nhiên nhanh hơn lambda; hãy chọn dạng dễ đọc.

## 4. Capture và effectively final

Lambda có thể dùng local variable bên ngoài nếu biến đó là `final` hoặc **effectively final**:

```java
long threshold = 100;
Predicate<AgentExecution> slow =
        item -> item.durationMs() >= threshold;
```

`threshold` effectively final vì chỉ được gán một lần. Nếu gán lại, code không compile:

```java
long threshold = 100;
threshold = 200;
// item -> item.durationMs() >= threshold; // compile error
```

`final` reference không làm object immutable:

```java
final List<String> names = new ArrayList<>();
Consumer<String> addName = names::add; // vẫn sửa list
```

Side effect này hợp lệ về cú pháp nhưng cần thận trọng.

---

## 5. Collection khác Stream

| Collection | Stream |
|---|---|
| Lưu phần tử | Mô tả quá trình xử lý |
| Có thể duyệt nhiều lần | Chỉ tiêu thụ một lần |
| Có thể thêm/xóa tùy loại | Không phải nơi lưu dữ liệu |
| Thao tác trực tiếp thường eager | Intermediate operation thường lazy |

```java
long count = executions.stream()
        .filter(item -> item.status() == Status.SUCCESS)
        .count();
```

Pipeline có ba phần:

```text
source -> intermediate operations -> terminal operation
```

- Source: `executions.stream()`.
- Intermediate: `filter`, `map`, `sorted`.
- Terminal: `count`, `toList`, `collect`.

## 6. Lazy evaluation và stream dùng một lần

Intermediate operation thường chưa chạy ngay:

```java
Stream<AgentExecution> pipeline = executions.stream()
        .filter(item -> item.status() == Status.SUCCESS);
```

Chỉ khi có terminal operation, pipeline mới được tiêu thụ:

```java
List<AgentExecution> result = pipeline.toList();
```

Không thể dùng lại:

```java
Stream<AgentExecution> stream = executions.stream();
stream.count();
// stream.toList(); // IllegalStateException
```

Nếu cần hai kết quả, tạo hai stream mới từ collection.

Các operation như `findFirst`, `anyMatch`, `allMatch` có thể short-circuit:

```java
boolean hasFailure = executions.stream()
        .anyMatch(item -> item.status() == Status.FAILED);
```

Nó dừng khi gặp failure đầu tiên. Tuy nhiên `sorted` thường phải đọc toàn bộ input trước khi trả kết quả.

## 7. Các operation quan trọng

### `filter`

Giữ phần tử thỏa `Predicate`:

```java
List<AgentExecution> successes = executions.stream()
        .filter(item -> item.status() == Status.SUCCESS)
        .toList();
```

### `map`

Biến đổi một phần tử thành một kết quả:

```java
List<String> names = executions.stream()
        .map(AgentExecution::agentName)
        .toList();
```

### `flatMap`

Biến mỗi phần tử thành nhiều phần tử rồi làm phẳng:

```java
record Conversation(List<ChatTurn> turns) {}
record ChatTurn(String prompt) {}

List<String> prompts = conversations.stream()
        .flatMap(conversation -> conversation.turns().stream())
        .map(ChatTurn::prompt)
        .toList();
```

`map` ở đây sẽ tạo `Stream<List<ChatTurn>>`; `flatMap` tạo một `Stream<ChatTurn>`.

### `sorted`

```java
List<AgentExecution> slowestFirst = executions.stream()
        .sorted(Comparator
                .comparingLong(AgentExecution::durationMs)
                .reversed())
        .toList();
```

Sắp xếp nhiều tiêu chí:

```java
Comparator<AgentExecution> comparator =
        Comparator.comparing(AgentExecution::agentName)
                .thenComparingLong(AgentExecution::durationMs);
```

### `distinct`

`distinct` dựa trên `equals()` và `hashCode()`:

```java
List<String> distinctNames = executions.stream()
        .map(AgentExecution::agentName)
        .distinct()
        .toList();
```

Đặt `distinct` trước `map` sẽ loại execution trùng toàn bộ, không phải tên trùng.

### `limit` và `skip`

```java
List<AgentExecution> topThree = executions.stream()
        .sorted(Comparator.comparingLong(
                AgentExecution::durationMs).reversed())
        .limit(3)
        .toList();
```

Không tải toàn bộ database rồi phân trang bằng stream; hãy để database `ORDER BY` và giới hạn kết quả.

---

## 8. `reduce`, primitive stream và `Optional`

`reduce` gộp nhiều phần tử thành một value:

```java
long total = executions.stream()
        .map(AgentExecution::durationMs)
        .reduce(0L, Long::sum);
```

Không có identity thì kết quả có thể rỗng:

```java
Optional<Long> maximum = executions.stream()
        .map(AgentExecution::durationMs)
        .reduce(Long::max);
```

Với số, primitive stream thường rõ hơn và tránh boxing:

```java
long total = executions.stream()
        .mapToLong(AgentExecution::durationMs)
        .sum();

LongSummaryStatistics stats = executions.stream()
        .mapToLong(AgentExecution::durationMs)
        .summaryStatistics();
```

`min`, `max`, `findFirst` trả `Optional` vì stream có thể rỗng:

```java
AgentExecution slowest = executions.stream()
        .max(Comparator.comparingLong(
                AgentExecution::durationMs))
        .orElseThrow(() ->
                new IllegalStateException("No execution"));
```

Không gọi `get()` vô điều kiện.

Phân biệt fallback:

```java
optional.orElse(expensiveFallback());          // luôn tính argument
optional.orElseGet(() -> expensiveFallback()); // chỉ tính khi rỗng
```

## 9. `collect` và grouping

`collect` tạo result container hoặc kết quả phức tạp:

```java
Set<String> names = executions.stream()
        .map(AgentExecution::agentName)
        .collect(Collectors.toSet());
```

Nhóm execution theo agent:

```java
Map<String, List<AgentExecution>> byAgent =
        executions.stream()
                .collect(Collectors.groupingBy(
                        AgentExecution::agentName));
```

Downstream collector thay list bằng thống kê:

```java
Map<String, Long> countByAgent = executions.stream()
        .collect(Collectors.groupingBy(
                AgentExecution::agentName,
                Collectors.counting()));
```

`toMap` cần merge rule nếu key có thể trùng:

```java
Map<String, Long> maxDurationByAgent = executions.stream()
        .collect(Collectors.toMap(
                AgentExecution::agentName,
                AgentExecution::durationMs,
                Long::max));
```

Nếu bỏ `Long::max`, dữ liệu có nhiều execution cùng agent sẽ ném `IllegalStateException`.

---

## 10. Phân tích `StreamsDemo`

Dữ liệu:

```text
summarizer  SUCCESS 120
summarizer  FAILED   80
code-review SUCCESS 350
summarizer  SUCCESS 100
```

Pipeline đầu:

```java
List<String> slowSuccessfulAgents = executions.stream()
        .filter(item -> item.status() == Status.SUCCESS)
        .filter(item -> item.durationMs() >= 100)
        .sorted(Comparator.comparingLong(
                AgentExecution::durationMs).reversed())
        .map(AgentExecution::agentName)
        .distinct()
        .toList();
```

Luồng dữ liệu:

```text
lọc SUCCESS
-> giữ duration >= 100
-> sort 350, 120, 100
-> map tên: code-review, summarizer, summarizer
-> distinct
-> [code-review, summarizer]
```

`distinct` đặt sau `map` vì yêu cầu là tên agent duy nhất.

Pipeline thứ hai:

```java
Map<String, Double> averageDurationByAgent = executions.stream()
        .collect(Collectors.groupingBy(
                AgentExecution::agentName,
                Collectors.averagingLong(
                        AgentExecution::durationMs)));
```

Tính tay:

```text
summarizer: (120 + 80 + 100) / 3 = 100.0
code-review: 350 / 1 = 350.0
```

Pipeline này tính cả execution failed. Nếu dashboard chỉ đo success latency, phải filter trước `collect`. Thứ tự key của map mặc định không nên được xem là contract.

`AgentExecution` là record nên có accessor, `equals`, `hashCode`, `toString` tự sinh. Record chỉ shallowly immutable nếu component chứa object mutable.

## 11. Side effect và `peek`

Không nên tích lũy vào collection ngoài:

```java
List<String> names = new ArrayList<>();
executions.stream()
        .filter(item -> item.status() == Status.SUCCESS)
        .forEach(item -> names.add(item.agentName()));
```

Nên mô tả kết quả:

```java
List<String> names = executions.stream()
        .filter(item -> item.status() == Status.SUCCESS)
        .map(AgentExecution::agentName)
        .toList();
```

Side effect làm code phụ thuộc thứ tự, khó test và dễ race condition khi parallel.

`peek` chủ yếu để quan sát:

```java
executions.stream()
        .peek(item -> System.out.println("before: " + item))
        .filter(item -> item.status() == Status.SUCCESS)
        .toList();
```

Không đặt lưu DB, gửi email hoặc business rule trong `peek`. Nếu không có terminal operation, `peek` có thể không chạy.

## 12. Stream hay vòng lặp?

Stream phù hợp khi:

- Có chuỗi lọc, biến đổi, gom kết quả.
- Operation stateless và tên rõ ràng.
- Không cần control flow phức tạp.

Loop phù hợp khi:

- Cần `break`, `continue` phức tạp.
- Một lượt duyệt cập nhật nhiều state liên quan.
- Cần xử lý checked exception.
- Debug từng statement quan trọng hơn.

Code ngắn hơn không tự động tốt hơn. Hãy viết cả hai và chọn bản người khác đọc nhanh hơn.

## 13. Vì sao không bật `parallelStream()` máy móc?

Parallel stream thường dùng common `ForkJoinPool`. Nó có thể hữu ích khi dữ liệu đủ lớn, công việc CPU-bound, operation độc lập và đã benchmark.

Nó có thể chậm hoặc nguy hiểm khi:

- Dataset nhỏ, overhead lớn hơn lợi ích.
- Công việc blocking I/O.
- Có shared mutable state.
- Common pool bị phần khác chiếm dụng.
- Phép reduce không associative.

Gọi model HTTP trong SoftAIBox phù hợp hơn với executor/virtual thread có timeout và lifecycle rõ ràng. Parallel stream không làm CPU có thêm core.

## 14. Độ phức tạp

| Operation | Chi phí điển hình |
|---|---|
| `filter`, `map`, `count` | `O(n)` |
| `distinct` | Trung bình `O(n)`, thêm bộ nhớ |
| `sorted` | `O(n log n)` |
| `groupingBy` | Trung bình `O(n)`, thêm map |

Nếu chỉ cần phần tử chậm nhất, dùng `max` là `O(n)` thay vì sort toàn bộ:

```java
executions.stream()
        .max(Comparator.comparingLong(
                AgentExecution::durationMs));
```

---

## 15. Liên hệ SoftAIBox

Stream hợp với dữ liệu vừa phải đã nằm trong memory:

- Map entity thành response DTO.
- Tính status count và latency dashboard.
- Gom execution theo agent.
- Dùng `flatMap` lấy turn từ conversation.

Stream không thay database query. Không load một triệu execution rồi filter/sort/paginate trong Java; hãy dùng `WHERE`, `ORDER BY`, `GROUP BY`, pagination và index tại database.

Đối với notification, pipeline có thể **chọn** notification cần gửi, còn service riêng chịu trách nhiệm gửi, retry và lưu trạng thái. Side effect mạng không nên ẩn trong `map`.

## 16. Lỗi thường gặp

- Dùng lại stream sau terminal operation.
- Quên terminal operation nên pipeline không chạy.
- Dùng `map` để trả `null` thay vì `filter`.
- Đặt business logic trong `peek`.
- Sửa collection ngoài bằng `forEach`.
- Quên merge function của `toMap`.
- Gọi `Optional.get()` không kiểm tra.
- Sort toàn bộ chỉ để lấy min/max.
- Nghĩ `Stream.toList()` trả list mutable.
- Dùng parallel stream cho blocking I/O.

`Stream.toList()` trả list không cho phép thêm/xóa:

```java
List<String> names = executions.stream()
        .map(AgentExecution::agentName)
        .toList();
// names.add("new"); // UnsupportedOperationException
```

## 17. Bài thực hành

1. Tính success rate theo agent, chú ý phép chia số nguyên.
2. Tạo `Map<Status, Long>` bằng `groupingBy` và `counting`.
3. Lấy ba execution thành công chậm nhất.
4. Dùng `flatMap` lấy 10 prompt dài nhất từ conversation.
5. Viết một bài bằng loop và stream, giải thích bản dễ đọc hơn.
6. Tạo map `agentName -> maxDuration` với merge function.
7. Dùng `summaryStatistics` lấy count, sum, min, max, average.
8. Đặt `peek` quanh `filter` và `findFirst`, đoán output trước khi chạy.
9. So sánh `sorted().findFirst()` với `max()` về Big-O.
10. Test input rỗng, tất cả failed, dữ liệu trùng và boundary `100`.

## 18. Câu hỏi phỏng vấn và đáp án ngắn

### Functional interface là gì?

Interface có đúng một abstract method, có thể được cài bằng lambda hoặc method reference.

### Lambda có phải function độc lập không?

Không. Lambda được target-typed thành một functional interface cụ thể.

### Intermediate operation có chạy ngay không?

Thông thường không; nó lazy và chạy khi terminal operation tiêu thụ pipeline.

### `map` khác `flatMap`?

`map` biến một phần tử thành một kết quả; `flatMap` tạo nhiều phần tử rồi làm phẳng.

### `map` khác `peek`?

`map` biến đổi dữ liệu; `peek` giữ nguyên phần tử và chủ yếu phục vụ quan sát/debug.

### `reduce` khác `collect`?

`reduce` gộp thành một value; `collect` tích lũy vào container hoặc kết quả phức tạp.

### `distinct` dựa vào gì?

`equals()` và `hashCode()` của phần tử.

### Stream có luôn nhanh hơn loop?

Không. Lợi ích chính là diễn đạt pipeline; hiệu năng phải đo theo workload.

### Khi nào dùng parallel stream?

Khi workload đủ lớn, CPU-bound, stateless, phép gộp hợp lệ và benchmark chứng minh có lợi.

---

## 19. Cách chạy

Từ thư mục gốc:

```powershell
java .\src\main\java\vn\dangquangdat\javabegin\learning\day06\StreamsDemo.java
```

Kết quả list:

```text
[code-review, summarizer]
```

Map chứa `summarizer=100.0` và `code-review=350.0`; không phụ thuộc thứ tự in key.

Biên dịch và chạy toàn bộ test:

```powershell
.\mvnw.cmd compile
.\mvnw.cmd test
```

## 20. Cách học

1. Tự viết bốn functional interface chuẩn.
2. Viết yêu cầu bằng loop trước.
3. Chuyển thành stream từng operation.
4. Ghi kiểu dữ liệu sau mỗi bước.
5. Đoán output trước khi chạy.
6. Thử input rỗng và trùng.
7. Viết test boundary.
8. Cuối buổi giải thích lại không nhìn tài liệu.

## Definition of done

Bạn hoàn thành ngày 6 khi có thể:

- Viết lambda cho bốn functional interface chính.
- Giải thích effectively final.
- Phân biệt collection và stream.
- Nhận diện intermediate/terminal operation.
- Giải thích lazy evaluation và one-shot stream.
- Dùng đúng `filter`, `map`, `flatMap`, `sorted`, `distinct`.
- Dùng `reduce`, primitive stream và `groupingBy`.
- Xử lý `Optional` an toàn.
- Tránh side effect.
- Chọn hợp lý giữa loop và stream.
- Giải thích rủi ro `parallelStream()`.
- Tự viết lại `StreamsDemo` và liên hệ dashboard SoftAIBox.
