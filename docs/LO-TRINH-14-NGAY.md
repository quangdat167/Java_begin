# Lộ trình Java 14 ngày cá nhân hóa cho Đặng Quang Đạt

## Cách học để 14 ngày thực sự có tác dụng

Bạn không bắt đầu từ con số 0: TypeScript, React, Node.js, REST, MongoDB, TanStack
Query, Docker và các domain auth/chat/payment/agent đã tạo sẵn “móc” để gắn kiến
thức Java. Khác biệt lớn nhất cần vượt qua là Java kiểm tra kiểu chặt hơn, mô hình
object/class rõ hơn, hệ sinh thái backend dựa nhiều vào IoC/DI, transaction và thread.

Mỗi ngày dành 4-6 giờ theo nhịp sau:

1. 60-90 phút đọc lý thuyết bên dưới và tự nói lại không nhìn tài liệu.
2. 90 phút gõ lại code mẫu, không copy-paste; đặt breakpoint và quan sát biến.
3. 90 phút làm bài tập; chỉ xem lời gợi ý sau 30 phút tự giải.
4. 30-45 phút trả lời câu hỏi phỏng vấn thành tiếng.
5. 15 phút ghi “hôm nay mình sai gì” vào nhật ký học tập.

Đừng cố thuộc annotation. Hãy luôn trả lời ba câu: dữ liệu đi từ đâu, object nào chịu
trách nhiệm, lỗi/transaction/thread được xử lý ở đâu.

---

## Ngày 1 - Java chạy như thế nào, kiểu dữ liệu và bộ nhớ

### Lý thuyết cốt lõi

- **JDK** là bộ công cụ phát triển: compiler `javac`, runtime, debugger và standard
  library. **JVM** là máy ảo thực thi bytecode. **JRE** là khái niệm runtime gồm JVM
  và thư viện cần để chạy. Luồng cơ bản: `.java -> javac -> .class bytecode -> JVM`.
- Java là statically typed. TypeScript kiểm tra kiểu chủ yếu lúc build rồi chạy thành
  JavaScript; kiểu Java vẫn là một phần của bytecode/runtime, hỗ trợ overload,
  reflection và JVM verification.
- Tám primitive (`byte`, `short`, `int`, `long`, `float`, `double`, `char`, `boolean`)
  giữ giá trị đơn giản. Class, array, enum, record là reference type. Biến reference
  giữ tham chiếu đến object, không chứa toàn bộ object.
- Java **luôn pass-by-value**. Khi truyền object, giá trị được copy là reference. Method
  có thể sửa trạng thái object mà hai reference cùng trỏ tới, nhưng không thể đổi biến
  của caller sang object khác.
- `String` immutable: mọi phép “sửa” tạo String mới. So sánh nội dung bằng `equals`,
  không dùng `==` vì `==` so sánh identity của reference.
- Không dùng `double` cho tiền. `0.1 + 0.2` không biểu diễn chính xác theo binary
  floating point; `BigDecimal("19.99")` bảo toàn decimal.
- Stack thường chứa frame của method/local variable; heap chứa object. Garbage
  collector thu hồi object không còn reachable. Đây là mô hình tư duy, JVM có thể tối
  ưu thực tế khác đi.

### Code mẫu

Mở [BasicsDemo.java](../src/main/java/vn/dangquangdat/javabegin/learning/day01/BasicsDemo.java).
Đoạn quan trọng:

```java
int requestCount = 1_250;
BigDecimal price = new BigDecimal("19.99");
UserSummary user = new UserSummary(1L, "dat@example.com", true);
```

`record` phù hợp cho DTO/value object: compiler sinh constructor, accessor,
`equals/hashCode/toString`. Compact constructor trong mẫu giữ invariant `id > 0` và
email không rỗng.

### Bài tập và tiêu chuẩn hoàn thành

- Viết `PlanPrice` có `name`, `monthlyPrice`, `currency`; chặn giá âm.
- In giá 12 tháng với giảm 10%; kết quả phải làm tròn 2 chữ số.
- Dùng debugger xem stack khi constructor ném `IllegalArgumentException`.
- Tự trả lời: `final` trên biến khác gì immutable object? `Integer` khác `int` ở đâu?

---

## Ngày 2 - Control flow, method, scope và array

### Lý thuyết cốt lõi

- `if/else` biểu diễn nhánh điều kiện; `switch` expression phù hợp khi một giá trị rơi
  vào các trường hợp rời rạc và buộc trả kết quả cho mọi nhánh.
- Method nên làm một việc, tên diễn đạt intent, tham số ít và không có side effect ẩn.
  Java cho phép **overload** (cùng tên, khác danh sách tham số); không được overload chỉ
  bằng kiểu trả về. **Override** là subclass triển khai lại method của parent/interface.
- Biến chỉ sống trong lexical scope `{}`. Local variable phải được gán chắc chắn trước
  khi đọc; field có default value. Đây là nguồn của nhiều câu hỏi phỏng vấn.
- Array có kích thước cố định và biết kiểu phần tử. `List` linh hoạt hơn cho nghiệp vụ;
  array vẫn quan trọng khi học thuật toán và API mức thấp.
- Guard clause (`if invalid -> throw/return`) làm giảm nesting. Với backend, validate
  gần boundary rồi để domain method giả định dữ liệu hợp lệ.

### Code mẫu

[ControlFlowDemo.java](../src/main/java/vn/dangquangdat/javabegin/learning/day02/ControlFlowDemo.java)
chuyển plan thành quota bằng switch expression:

```java
long quota = switch (plan) {
    case "FREE" -> 100;
    case "PRO" -> 5_000;
    case "ENTERPRISE" -> 100_000;
    default -> throw new IllegalArgumentException("Unknown plan: " + plan);
};
```

Khác JavaScript, compiler buộc mọi nhánh tạo giá trị. Điều này giảm trạng thái
`undefined` lọt qua nhiều lớp.

### Bài tập và tiêu chuẩn hoàn thành

- Viết method tính phí over-quota bằng `BigDecimal`.
- Viết FizzBuzz và binary search trên `int[]`; ghi Big-O của từng thuật toán.
- Refactor một method dài thành parse -> validate -> calculate -> format.
- Trả lời: vì sao `String... tags` thực chất là array? Method signature gồm những gì?

---

## Ngày 3 - OOP đúng nghĩa: encapsulation, abstraction, polymorphism

### Lý thuyết cốt lõi

- **Class** là khuôn mô tả state và behavior; object là instance. Encapsulation không
  chỉ là `private field + getter/setter`: object phải tự bảo vệ invariant. Ví dụ
  `subscription.cancel()` tốt hơn `subscription.setStatus("CANCELLED")` vì method có
  thể kiểm tra trạng thái hợp lệ.
- **Interface** mô tả capability/contract, không quan tâm implementation. Code phụ
  thuộc interface có thể thay Stripe bằng mock hoặc bank gateway.
- **Polymorphism** gọi cùng `charge()` nhưng runtime chọn implementation đúng. Đây là
  nền của Strategy, dependency injection và test double.
- Ưu tiên **composition over inheritance**. Inheritance tạo quan hệ “is-a” và coupling
  mạnh; composition ghép capability nhỏ, thay đổi dễ hơn.
- `record` dành cho dữ liệu/value; `enum` là tập giá trị hữu hạn có type safety. Đừng
  dùng magic string như `"admin"` khắp code.
- `equals/hashCode` định nghĩa equality logic. Nếu override `equals` phải nhất quán với
  `hashCode`, nếu không `HashMap/HashSet` hành xử sai.

### Code mẫu

[OopDemo.java](../src/main/java/vn/dangquangdat/javabegin/learning/day03/OopDemo.java)
dùng `PaymentGateway` tương tự việc SoftAIBox tích hợp Stripe nhưng không khóa domain
service vào Stripe:

```java
interface PaymentGateway {
    PaymentResult charge(Subscription subscription);
}
```

Controller/service chỉ biết contract. Production adapter mới biết Stripe SDK, webhook
signature hay idempotency key.

### Bài tập và tiêu chuẩn hoàn thành

- Thêm `MockPaymentGateway` và test không gọi mạng.
- Thiết kế `NotificationSender` với email/dashboard; không viết `if (type == ...)` trong
  business service.
- Vẽ object diagram của User -> Subscription -> Package -> Plan.
- Trả lời: abstract class khác interface? Khi nào record không phù hợp với JPA entity?

---

## Ngày 4 - Collections, generics và độ phức tạp

### Lý thuyết cốt lõi

- `List`: có thứ tự, cho phép trùng; `ArrayList` đọc theo index O(1), chèn giữa O(n).
- `Set`: phần tử duy nhất; `HashSet` trung bình O(1), `TreeSet` O(log n) và có sort.
- `Map<K,V>` tra theo key; `HashMap` cần `equals/hashCode` đúng. `LinkedHashMap` giữ thứ
  tự chèn, `TreeMap` sắp theo key.
- Generic đưa type safety vào compile time: `Store<Long, Conversation>` ngăn lưu nhầm
  object. Do **type erasure**, phần lớn type parameter không còn đầy đủ ở runtime;
  không thể `new T()` hoặc `instanceof List<String>` trực tiếp.
- PECS: producer `extends`, consumer `super`. Nếu method chỉ đọc `List<Dog>` như
  `Animal`, nhận `List<? extends Animal>`; nếu thêm Dog, dùng `List<? super Dog>`.
- Tránh trả collection mutable nội bộ. `List.copyOf`/`Set.copyOf` tạo snapshot không
  sửa được và bảo vệ encapsulation.

### Code mẫu

[CollectionsDemo.java](../src/main/java/vn/dangquangdat/javabegin/learning/day04/CollectionsDemo.java)
có generic store và hợp nhất tag bằng `LinkedHashSet`:

```java
Set<String> uniqueTags = new LinkedHashSet<>();
sorted.forEach(conversation -> uniqueTags.addAll(conversation.tags()));
```

### Bài tập và tiêu chuẩn hoàn thành

- Đếm tần suất từ trong prompt bằng `Map<String,Integer>` và `merge`.
- Viết LRU cache nhỏ bằng `LinkedHashMap` (được xem tài liệu).
- Benchmark tư duy: tìm 1 email trong List 1 triệu user và HashMap khác nhau ra sao?
- Trả lời: collision của HashMap là gì? Vì sao key mutable nguy hiểm?

---

## Ngày 5 - Exception, Optional, ngày giờ và file I/O

### Lý thuyết cốt lõi

- Checked exception (`IOException`) buộc caller catch/declare; unchecked exception
  (`IllegalArgumentException`) không bắt buộc. Checked hữu ích khi caller có chiến lược
  phục hồi; đừng catch `Exception` rồi bỏ qua.
- Preserve cause khi chuyển exception: `new DomainException(message, cause)`. Log một
  lần tại boundary; vừa log vừa rethrow ở nhiều lớp tạo log trùng.
- `try-with-resources` tự đóng object implements `AutoCloseable`, kể cả khi có lỗi.
- `Optional<T>` diễn đạt “có thể không có kết quả”, phù hợp return type. Tránh dùng làm
  entity field, request DTO hoặc parameter. Đừng gọi `get()` mà không kiểm tra.
- Dùng `Instant` cho timestamp tuyệt đối; `LocalDate` cho ngày không timezone;
  `ZonedDateTime` khi business cần múi giờ. Lưu DB theo UTC, convert ở UI.
- File upload production phải giới hạn size/type, đổi tên server-side, tránh path
  traversal, scan malware và không tin `Content-Type` từ client.

### Code mẫu

[ExceptionsAndIoDemo.java](../src/main/java/vn/dangquangdat/javabegin/learning/day05/ExceptionsAndIoDemo.java)
dùng try-with-resources và wrap `IOException`:

```java
try (BufferedReader reader = Files.newBufferedReader(path)) {
    // read lines
} catch (IOException exception) {
    throw new DataSourceImportException("Cannot import " + path, exception);
}
```

### Bài tập và tiêu chuẩn hoàn thành

- Import CSV `email,role`; trả danh sách lỗi có số dòng thay vì dừng ở lỗi đầu.
- Parse thời điểm user nhập ở `Asia/Ho_Chi_Minh` thành `Instant`.
- Phân biệt lỗi 400, 401, 403, 404, 409, 500 bằng ví dụ SoftAIBox.
- Trả lời: `finally` có luôn chạy không? Khi nào tạo custom exception?

---

## Ngày 6 - Lambda, functional interface và Stream API

### Lý thuyết cốt lõi

- Functional interface có đúng một abstract method: `Predicate<T>`, `Function<T,R>`,
  `Consumer<T>`, `Supplier<T>` là bốn loại cần nhớ. Lambda là implementation gọn của
  contract đó, không phải “function tự do” như JavaScript.
- Stream là pipeline xử lý, không phải collection. Intermediate operation như
  `filter/map/sorted` lazy; terminal operation như `toList/collect/count` kích hoạt chạy.
- Một stream chỉ dùng một lần. Operation nên stateless, không sửa external collection;
  side effect trong `map` khiến code khó dự đoán và khó parallelize.
- `map` biến 1-1; `flatMap` biến mỗi phần tử thành nhiều phần tử rồi làm phẳng;
  `reduce` gộp thành một giá trị; `collect` gộp mutable/result phức tạp.
- Stream không luôn tốt hơn loop. Loop rõ hơn khi cần break sớm, nhiều state thay đổi
  hoặc debug từng bước. `parallelStream()` không phải nút “tăng tốc”; common pool và
  overhead có thể làm chậm hoặc gây tranh chấp.

### Code mẫu

[StreamsDemo.java](../src/main/java/vn/dangquangdat/javabegin/learning/day06/StreamsDemo.java)
phân tích lịch sử agent giống dashboard SoftAIBox:

```java
Map<String, Double> averages = executions.stream()
    .collect(groupingBy(AgentExecution::agentName,
                        averagingLong(AgentExecution::durationMs)));
```

### Bài tập và tiêu chuẩn hoàn thành

- Tính success rate theo agent, bỏ agent chưa có execution.
- Từ conversation -> turns, dùng `flatMap` lấy top 10 prompt dài nhất.
- Viết một phiên bản loop và stream; giải thích bản nào đọc dễ hơn.
- Trả lời: intermediate operation có chạy ngay không? `map` khác `peek`?

---

## Ngày 7 - Concurrency, thread safety và virtual thread

### Lý thuyết cốt lõi

- Concurrency là nhiều task tiến triển xen kẽ; parallelism là thực sự chạy đồng thời.
  Web server cần concurrency vì nhiều request chờ I/O.
- Race condition xảy ra khi kết quả phụ thuộc thứ tự thread. `count++` không atomic:
  đọc, cộng, ghi là ba bước. Có thể dùng confinement/immutable data, `synchronized`,
  lock hoặc `AtomicInteger` tùy bài toán.
- `volatile` đảm bảo visibility/order cho một biến, không biến chuỗi thao tác thành
  atomic transaction.
- `ExecutorService` quản lý lifecycle và queue tốt hơn tự tạo thread. `Future` đại diện
  kết quả tương lai; `CompletableFuture` tạo pipeline async nhưng dễ phức tạp nếu error
  handling/timeout không rõ.
- Virtual thread nhẹ và phù hợp nhiều tác vụ blocking I/O. Nó không làm CPU work nhanh
  hơn; CPU-bound vẫn bị giới hạn bởi số core. Không giữ lock lâu khi thực hiện blocking
  I/O.
- Database transaction không thay thế Java lock và ngược lại. Nhiều app instance cần
  constraint/locking ở DB hoặc distributed coordination.

### Code mẫu

[ConcurrencyDemo.java](../src/main/java/vn/dangquangdat/javabegin/learning/day07/ConcurrencyDemo.java)
gọi nhiều model giả lập bằng virtual thread:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<ModelAnswer>> futures = executor.invokeAll(calls);
}
```

### Bài tập và tiêu chuẩn hoàn thành

- Tạo race condition với 10.000 lần `counter++`, sau đó sửa bằng `AtomicInteger`.
- Thêm timeout cho ba model, chọn câu trả lời hoàn thành đầu tiên.
- Giải thích vì sao request Stripe webhook phải idempotent dù code thread-safe.
- Trả lời: deadlock cần điều kiện gì? `synchronized` khóa object/class khác nhau ra sao?

---

## Ngày 8 - SOLID, design pattern và kiến trúc lớp

### Lý thuyết cốt lõi

- **S**: một class có một lý do thay đổi. Controller không chứa SQL, repository không
  gửi email.
- **O**: mở cho extension, đóng cho modification. Thêm kênh notification bằng Strategy
  thay vì sửa chuỗi `if/else` trung tâm.
- **L**: subtype thay được parent mà không phá contract. Implementation không được ném
  lỗi bất ngờ cho input parent chấp nhận.
- **I**: interface nhỏ theo client. `PaymentReader` và `PaymentWriter` đôi khi tốt hơn
  interface khổng lồ.
- **D**: policy cấp cao phụ thuộc abstraction, không phụ thuộc Stripe SDK/JPA trực tiếp.
- Pattern là vocabulary, không phải mục tiêu. Strategy chọn thuật toán; Factory tạo
  đúng strategy; Adapter bọc third-party; Observer phát event; Builder tạo object nhiều
  tùy chọn. Repository và dependency injection cũng là pattern.
- Kiến trúc controller -> service -> repository hợp lý cho CRUD vừa phải. Domain phức
  tạp có thể tách use case/domain/adapter, nhưng đừng “clean architecture” hình thức.

### Code mẫu

[DesignPatternsDemo.java](../src/main/java/vn/dangquangdat/javabegin/learning/day08/DesignPatternsDemo.java)
dùng Strategy + Factory cho email/dashboard notification, đúng domain agent trong CV.

### Bài tập và tiêu chuẩn hoàn thành

- Thêm Slack strategy nhưng không sửa các strategy có sẵn.
- Vẽ dependency direction cho AuthController -> TokenService -> JwtEncoder.
- Tìm một class trong frontend vừa fetch vừa format vừa toast; đề xuất tách trách nhiệm.
- Trả lời: dependency inversion khác dependency injection như thế nào?

---

## Ngày 9 - Maven, JUnit và tư duy kiểm thử

### Lý thuyết cốt lõi

- Maven đọc `pom.xml`, giải dependency transitive và chạy lifecycle. Các phase thường
  dùng: `compile`, `test`, `package`, `verify`, `install`. `clean` là lifecycle khác.
- Scope `test` không đi vào artifact production. Luôn commit wrapper để máy CI/dev dùng
  cùng Maven version.
- Unit test kiểm một đơn vị logic, nhanh và không cần Spring/DB. Integration test kiểm
  nhiều lớp thật. End-to-end test kiểm hệ thống từ ngoài; số lượng ít hơn vì chậm/dễ
  flaky.
- Cấu trúc Arrange-Act-Assert, tên test mô tả behavior. Test boundary và nhánh lỗi,
  không chạy theo phần trăm coverage mù quáng.
- Mock contract bên ngoài hoặc collaborator khó kiểm soát; đừng mock value object và
  đừng mock mọi thứ đến mức test chỉ xác nhận implementation.
- Với tiền, assert `BigDecimal` cẩn thận: `equals` xét cả scale (`10.0` khác `10.00`),
  `compareTo` xét giá trị số.

### Code mẫu

[PromotionService.java](../src/main/java/vn/dangquangdat/javabegin/learning/day09/PromotionService.java)
và [PromotionServiceTest.java](../src/test/java/vn/dangquangdat/javabegin/learning/day09/PromotionServiceTest.java)
kiểm cả happy path và invalid boundary.

```java
assertThrows(IllegalArgumentException.class,
    () -> service.applyPercent(new BigDecimal("19.99"), 101));
```

### Bài tập và tiêu chuẩn hoàn thành

- Viết parameterized test cho 0%, 25%, 100%, -1%, 101%.
- Test `ConversationService` bằng fake repository hoặc Mockito.
- Chạy `./mvnw.cmd clean test`; đọc report trong `target/surefire-reports`.
- Trả lời: test private method không? Stub, mock, spy khác nhau thế nào?

---

## Ngày 10 - SQL, transaction, JPA/Hibernate

### Lý thuyết cốt lõi

- SQL vẫn là nền tảng: `SELECT`, join, group, subquery, index, constraint. ORM không
  thay nhu cầu hiểu query plan.
- ACID: atomicity (all-or-nothing), consistency (constraint/invariant), isolation
  (transaction concurrent nhìn nhau thế nào), durability (commit tồn tại sau crash).
- Isolation giải quyết dirty/non-repeatable/phantom read theo mức khác nhau. Mức càng
  chặt thường giảm concurrency. Cần hiểu lost update và optimistic/pessimistic lock.
- JPA là specification; Hibernate là implementation phổ biến. Entity có lifecycle
  transient -> managed -> detached -> removed. Dirty checking update managed entity
  khi transaction commit, không cần gọi save sau mọi setter.
- Quan hệ mặc định/lazy phải hiểu. N+1 xảy ra khi load N parent rồi mỗi parent phát thêm
  query; sửa bằng fetch join/entity graph/projection/batch tùy use case.
- Index tăng tốc read nhưng tốn storage và làm write chậm. Index composite phụ thuộc
  thứ tự cột; index email không tự giúp mọi query `%keyword%`.

### Code mẫu

Đọc [Conversation.java](../src/main/java/vn/dangquangdat/javabegin/conversation/Conversation.java)
và [ConversationRepository.java](../src/main/java/vn/dangquangdat/javabegin/conversation/ConversationRepository.java).
Entity có `@Version` để phát hiện lost update:

```java
@Version
private long version;
```

`ConversationService.requireForNewTurn` chạy trong transaction, tăng turn count cùng
luồng lưu chat. Hãy bật SQL log trong lúc học nếu muốn quan sát query.

### Bài tập và tiêu chuẩn hoàn thành

- Viết SQL lấy 10 conversation cập nhật gần nhất của user có ít nhất 3 turns.
- Thêm unique constraint phù hợp cho user/agent name và giải thích trade-off.
- Tạo quan hệ `Conversation` - `ChatTurn`, cố tình gây N+1 rồi sửa bằng projection.
- Trả lời: `save()` khác `flush()`? `LAZY` lỗi ngoài transaction vì sao?

---

## Ngày 11 - Spring IoC/DI và request lifecycle

### Lý thuyết cốt lõi

- Spring container tạo, cấu hình và nối bean. Đây là inversion of control: application
  khai báo dependency, container điều khiển creation/lifecycle.
- Constructor injection làm dependency bắt buộc, field có thể `final`, test không cần
  reflection. Tránh field injection.
- Stereotype: `@RestController` nhận HTTP; `@Service` orchestration/use case;
  `@Repository` data access; `@Configuration/@Bean` cho object từ thư viện.
- Bean mặc định singleton trong một ApplicationContext. Vì nhiều request dùng cùng
  instance, service singleton không được giữ mutable request state trong field.
- Request đi qua servlet filters (security/CORS), DispatcherServlet, argument resolver,
  controller, service, repository, rồi response serialization. Exception advice là
  boundary chuyển exception thành HTTP response.
- Proxy tạo hành vi như `@Transactional`, `@Async`, `@PreAuthorize`. Self-invocation
  cùng object có thể bypass proxy; đó là lý do `AgentTaskRunner` tách bean riêng.

### Code mẫu

Theo luồng:

1. [ConversationController.java](../src/main/java/vn/dangquangdat/javabegin/conversation/ConversationController.java)
2. [ConversationService.java](../src/main/java/vn/dangquangdat/javabegin/conversation/ConversationService.java)
3. [ConversationRepository.java](../src/main/java/vn/dangquangdat/javabegin/conversation/ConversationRepository.java)

Constructor thể hiện dependency rõ ràng:

```java
public ConversationService(ConversationRepository repository) {
    this.repository = repository;
}
```

### Bài tập và tiêu chuẩn hoàn thành

- Vẽ sequence diagram cho POST conversation.
- Đặt breakpoint ở security filter, controller, service, repository.
- Cố tạo mutable field `currentUser` trong singleton service và giải thích bug concurrent.
- Trả lời: bean scope là gì? Circular dependency báo vấn đề thiết kế nào?

---

## Ngày 12 - REST, DTO, validation, pagination và error contract

### Lý thuyết cốt lõi

- Resource URL dùng danh từ; HTTP method diễn đạt hành động. GET safe/idempotent; PUT
  idempotent theo nghĩa gọi lặp cùng request cho cùng trạng thái; POST thường không.
- Status phổ biến: 200 đọc/sửa, 201 tạo, 202 nhận job async, 204 xóa không body, 400
  input sai, 401 chưa xác thực, 403 không có quyền, 404 không tìm thấy, 409 conflict.
- Không trả JPA entity trực tiếp: dễ lộ field, lazy serialization, vòng lặp relation và
  coupling database schema với public API. Request/response DTO là contract.
- Bean Validation kiểm syntax/boundary (`@NotBlank`, `@Email`, `@Size`); service/domain
  kiểm rule cần database hoặc state. Client validation bằng Zod cải thiện UX nhưng
  server validation mới là ranh giới tin cậy.
- Pagination phải giới hạn page size. Offset đơn giản nhưng chậm/lệch khi dataset lớn;
  cursor/keyset tốt hơn cho infinite scroll ổn định.
- Error response thống nhất giúp Axios/TanStack Query xử lý một chỗ. Không trả stack
  trace hoặc internal exception message ra production.

### Code mẫu

[GlobalExceptionHandler.java](../src/main/java/vn/dangquangdat/javabegin/common/GlobalExceptionHandler.java)
gom validation và domain errors. [ApiResponse.java](../src/main/java/vn/dangquangdat/javabegin/common/ApiResponse.java)
giữ shape `code/message/data/meta` tương thích type `BaseResponseType` ở UI.

```java
public record ConversationRequest(
    @NotBlank @Size(max = 120) String title
) {}
```

### Bài tập và tiêu chuẩn hoàn thành

- Thêm request ID vào response/log.
- Thiết kế cursor pagination cho agent execution `(createdAt,id)`.
- Thêm 409 khi title trùng theo user; bắt đúng exception DB vẫn cần constraint.
- Trả lời: PATCH khác PUT? Vì sao DELETE có thể trả 204? Idempotency key dùng khi nào?

---

## Ngày 13 - Spring Security, JWT, refresh token, RBAC và CORS

### Lý thuyết cốt lõi

- Authentication trả lời “bạn là ai”; authorization trả lời “bạn được làm gì”. UI
  `PrivateRoute/AdminRoute` chỉ là UX, không phải security boundary; backend phải chặn.
- Password lưu bằng slow adaptive hash như BCrypt/Argon2, không mã hóa reversible và
  không SHA-256 đơn thuần.
- JWT có header, claims, signature; thường **signed chứ không encrypted**. Không đặt
  secret/password/PII nhạy cảm trong payload. Backend kiểm signature, expiry, issuer,
  audience và quyền.
- Access token ngắn hạn; refresh token dài hơn, lưu/rotate/revoke server-side. Demo dùng
  memory nên mất khi restart và không scale nhiều instance. Production lưu hash của
  refresh token, device/session metadata và phát hiện reuse.
- 401 là token thiếu/sai/hết hạn; 403 là identity hợp lệ nhưng thiếu quyền.
- CORS là chính sách browser cho cross-origin, không phải authentication. CSRF quan
  trọng khi browser tự gửi credential (đặc biệt cookie). Token trong localStorage dễ
  bị lấy nếu XSS; secure HttpOnly SameSite cookie có trade-off CSRF. Phải phân tích
  threat model, không có một câu trả lời đúng cho mọi hệ thống.

### Code mẫu

Đọc theo thứ tự:

1. [SecurityConfig.java](../src/main/java/vn/dangquangdat/javabegin/security/SecurityConfig.java)
2. [TokenService.java](../src/main/java/vn/dangquangdat/javabegin/auth/TokenService.java)
3. [AuthController.java](../src/main/java/vn/dangquangdat/javabegin/auth/AuthController.java)
4. [AdminDemoController.java](../src/main/java/vn/dangquangdat/javabegin/security/AdminDemoController.java)

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/demo")
ApiResponse<?> adminOnly() { ... }
```

API refresh giữ đúng cách frontend đang gửi refresh token qua Bearer header và trả
`token`, `refreshToken`, `tokenExpires`, `user`.

### Bài tập và tiêu chuẩn hoàn thành

- Login USER gọi admin endpoint phải 403; login ADMIN phải 200.
- Giảm TTL còn 10 giây, quan sát 401 rồi refresh.
- Refactor refresh store thành JPA entity, lưu token hash và revokedAt.
- Trả lời: ký khác mã hóa? OAuth2 khác JWT? Method security và URL security khác nhau?

---

## Ngày 14 - Async agent, scheduling, production và mock interview

### Lý thuyết cốt lõi

- Job lâu không nên giữ HTTP request. API trả `202 Accepted + executionId`, worker xử
  lý sau, UI poll/SSE/WebSocket nhận trạng thái. Poll dễ triển khai; SSE hợp stream một
  chiều; WebSocket hợp hai chiều thời gian thực.
- `@Async` chạy qua proxy/executor. Phải có timeout, retry với backoff, idempotency,
  bounded concurrency và dead-letter strategy. Retry mù có thể nhân đôi payment/email.
- `@Scheduled` trong nhiều app replica sẽ chạy ở mọi replica. Production cần distributed
  lock hoặc external scheduler/queue tùy yêu cầu.
- Logging có cấu trúc, correlation ID, metric latency/error/saturation và trace giúp
  chẩn đoán. Không log token/password/prompt nhạy cảm.
- Docker image nên nhỏ, non-root, có health check, config qua environment/secret,
  graceful shutdown. Database migration chạy có kiểm soát, không `ddl-auto=create-drop`.
- Khi phỏng vấn, trình bày quyết định và trade-off. Câu “em chưa dùng production nhưng
  em hiểu rủi ro và sẽ kiểm chứng như sau” tốt hơn bịa kinh nghiệm.

### Code mẫu

[AgentController.java](../src/main/java/vn/dangquangdat/javabegin/agent/AgentController.java)
trả 202; [AgentTaskRunner.java](../src/main/java/vn/dangquangdat/javabegin/agent/AgentTaskRunner.java)
chạy async trên virtual thread; [AgentExecutionStore.java](../src/main/java/vn/dangquangdat/javabegin/agent/AgentExecutionStore.java)
dùng `ConcurrentHashMap` cho state demo.

```java
UUID executionId = executionStore.create(agent.getId());
taskRunner.run(executionId, agent.getTaskPrompt());
return ApiResponse.success("Agent execution accepted", executionStore.get(executionId));
```

### Capstone cuối ngày

1. Chạy test và app từ sạch.
2. Demo login -> conversation -> chat -> agent execution -> polling -> admin RBAC.
3. Mở frontend SoftAIBox, chỉ ra endpoint nào backend mini đã mô phỏng.
4. Vẽ architecture và data model trên giấy trong 10 phút.
5. Tự quay video 8 phút: 2 phút giới thiệu, 4 phút demo, 2 phút trade-off.
6. Làm mock interview theo tài liệu `PHONG-VAN-JAVA-JUNIOR.md` mà không nhìn đáp án.

### Definition of done sau 14 ngày

Bạn đạt mục tiêu sprint khi có thể:

- Viết class/interface/record, collection, stream và exception không tra cú pháp cơ bản.
- Giải thích JVM, memory, equality, generics, transaction, DI, JPA, REST, JWT và thread.
- Lần theo một request từ React/TanStack Query đến controller/service/repository/DB.
- Viết unit test, chạy Maven, đọc stack trace và dùng debugger.
- Demo mini backend, nói được ít nhất ba điểm chưa production-ready và cách cải thiện.
- Giải 15-20 bài array/string/hash map mức easy và 3-5 bài medium sau sprint.

Nếu chưa đạt một mục, lặp lại ngày liên quan. Không “chạy hết lịch” quan trọng bằng việc
tự viết và giải thích được.
