# Ngày 11 — Spring IoC/DI, kiến trúc lớp và vòng đời request

Ngày 11 trả lời câu hỏi: Spring đã nối những class riêng lẻ thành một ứng dụng web như thế nào? Sau ngày này, bạn phải lần được một request từ HTTP qua security, controller, service, repository, transaction, database rồi trở lại JSON.

Code đọc theo thứ tự:

- [JavaBeginApplication.java](../../JavaBeginApplication.java): điểm khởi động và component scan.
- [ConversationController.java](../../conversation/ConversationController.java): HTTP boundary.
- [ConversationService.java](../../conversation/ConversationService.java): use case và transaction.
- [ConversationRepository.java](../../conversation/ConversationRepository.java): data access.
- [SecurityConfig.java](../../security/SecurityConfig.java): filter chain và các bean cấu hình.
- [GlobalExceptionHandler.java](../../common/GlobalExceptionHandler.java): exception boundary.
- [AgentTaskRunner.java](../../agent/AgentTaskRunner.java): ví dụ tách bean để <code>@Async</code> đi qua proxy.

---

## Mục tiêu của ngày 11

Sau khi học xong, bạn cần:

1. Phân biệt IoC và Dependency Injection.
2. Giải thích Spring container, bean và ApplicationContext.
3. Biết component scan tìm bean từ đâu.
4. Phân biệt <code>@Component</code>, <code>@Service</code>, <code>@Repository</code>, <code>@RestController</code>.
5. Dùng constructor injection và giải thích vì sao tránh field injection.
6. Hiểu singleton scope và yêu cầu thread safety.
7. Theo được toàn bộ Spring MVC request lifecycle.
8. Hiểu JSON binding, validation và argument resolution.
9. Biết Spring Data tạo repository implementation runtime.
10. Hiểu proxy/AOP đứng sau <code>@Transactional</code>, <code>@Async</code> và <code>@PreAuthorize</code>.
11. Giải thích self-invocation bypass proxy.
12. Biết exception được đổi thành HTTP response ở đâu.
13. Đặt breakpoint đúng chỗ để quan sát flow.

---

## 1. Từ object tự tạo đến IoC container

Không dùng Spring, ta có thể tự nối object:

~~~java
ConversationRepository repository = ...;
ConversationService service = new ConversationService(repository);
ConversationController controller = new ConversationController(service);
~~~

Code gọi <code>new</code> chịu trách nhiệm:

- Chọn implementation.
- Tạo object theo đúng thứ tự.
- Chia sẻ hoặc tạo mới object.
- Quản lý lifecycle.
- Cấu hình dependency.

Với Spring, application khai báo các thành phần và dependency. Container chịu trách nhiệm tạo và nối chúng:

~~~text
Application khai báo:
ConversationController cần ConversationService
ConversationService cần ConversationRepository

Spring container:
tìm bean → tạo repository proxy → tạo service → tạo controller
~~~

Đây là Inversion of Control: quyền điều khiển việc tạo và nối object được đảo từ application code sang framework/container.

---

## 2. Dependency Injection là gì?

Dependency Injection là một cách thực hiện IoC. Dependency được cung cấp từ bên ngoài thay vì class tự tạo.

Code thật:

~~~java
public ConversationService(ConversationRepository repository) {
    this.repository = repository;
}
~~~

<code>ConversationService</code> không gọi:

~~~java
this.repository = new ConversationRepository(...);
~~~

Nó chỉ công bố rằng mình cần một <code>ConversationRepository</code>. Spring tìm bean phù hợp và truyền vào constructor.

### IoC khác DI

- IoC là nguyên tắc rộng: framework/container điều khiển lifecycle và flow.
- DI là kỹ thuật cung cấp dependency cho object.
- Spring còn thực hiện IoC qua callback, event, filter chain và request dispatching.

Câu trả lời phỏng vấn ngắn:

> IoC là việc chuyển quyền tạo và điều phối object cho container. DI là cơ chế container truyền dependency vào object, thường qua constructor.

---

## 3. ApplicationContext và bean

Bean là object được Spring container tạo, cấu hình và quản lý. ApplicationContext là container chứa bean definitions, bean instances và các dịch vụ framework.

Khi app start:

~~~text
main()
  ↓
SpringApplication.run(...)
  ↓
tạo ApplicationContext
  ↓
đọc auto-configuration + component scan + @Bean
  ↓
tạo và nối các singleton bean
  ↓
khởi động embedded web server
  ↓
nhận request
~~~

[JavaBeginApplication.java](../../JavaBeginApplication.java) chứa:

~~~java
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class JavaBeginApplication {
}
~~~

<code>@SpringBootApplication</code> kết hợp các ý chính:

- Configuration.
- Auto-configuration.
- Component scan.

Vì class nằm ở package <code>vn.dangquangdat.javabegin</code>, Spring scan package đó và các package con: <code>auth</code>, <code>security</code>, <code>conversation</code>, <code>chat</code>, <code>agent</code>, <code>common</code>.

Nếu đặt component ngoài cây package này, Spring có thể không tìm thấy nếu không cấu hình scan rõ ràng.

---

## 4. Các stereotype annotation

### <code>@Component</code>

Annotation tổng quát cho một Spring-managed component:

~~~java
@Component
public class AgentTaskRunner {
}
~~~

### <code>@Service</code>

Diễn đạt class chứa use case/business orchestration:

~~~java
@Service
public class ChatService {
}
~~~

Nó giúp người đọc nhận ra trách nhiệm; bản thân annotation cũng là một specialization của <code>@Component</code>.

### <code>@Repository</code>

Diễn đạt data access component và có thể tham gia exception translation. Với Spring Data, repository interface được framework tạo proxy nên repo này không cần tự gắn annotation:

~~~java
public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {
}
~~~

### <code>@RestController</code>

Đánh dấu HTTP controller, đồng thời để return value được serialize thành response body:

~~~java
@RestController
@RequestMapping("/api/v1/chat_conversations")
public class ConversationController {
}
~~~

### <code>@Configuration</code> và <code>@Bean</code>

Dùng khi object không thể hoặc không nên gắn component annotation, đặc biệt object của thư viện:

~~~java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
~~~

Method <code>@Bean</code> mô tả cách tạo object; Spring quản lý kết quả trả về.

---

## 5. Vì sao ưu tiên constructor injection?

Repo dùng constructor injection nhất quán:

~~~java
public ChatService(
        ConversationService conversations,
        ChatTurnRepository turns
) {
    this.conversations = conversations;
    this.turns = turns;
}
~~~

Ưu điểm:

- Dependency bắt buộc được nhìn thấy trong API của class.
- Field có thể là <code>final</code>.
- Object không tồn tại ở trạng thái thiếu dependency.
- Unit test có thể gọi constructor trực tiếp.
- Dễ thấy class có quá nhiều dependency.
- Không cần reflection để inject trong test.

Field injection:

~~~java
@Autowired
private ConversationService service;
~~~

gây bất lợi:

- Dependency ẩn.
- Field khó final.
- Tạo object ngoài Spring dễ nhận null.
- Test thuần Java khó hơn.
- Class có thể vi phạm Single Responsibility mà không dễ thấy.

Trong Spring hiện đại, constructor duy nhất không cần viết <code>@Autowired</code>.

---

## 6. Chọn một bean khi có nhiều implementation

Nếu có hai bean cùng implement một interface, Spring không biết chọn bean nào:

~~~java
interface ModelClient {
    String complete(String prompt);
}
~~~

Các cách giải quyết:

- <code>@Qualifier</code> để chọn theo tên/nhãn.
- <code>@Primary</code> để đặt implementation mặc định.
- Inject <code>List&lt;ModelClient&gt;</code> hoặc <code>Map&lt;String, ModelClient&gt;</code> khi cần toàn bộ strategy.
- Tách interface nhỏ hơn nếu hai implementation thực ra không cùng contract.

Đừng giải quyết ambiguity bằng cách service tự gọi <code>new</code>; điều đó bỏ DI và làm test khó hơn.

---

## 7. Bean scope

Scope xác định một bean instance sống và được chia sẻ như thế nào.

| Scope | Ý nghĩa |
|---|---|
| singleton | Một instance trong một ApplicationContext |
| prototype | Mỗi lần resolve tạo instance mới |
| request | Một instance cho mỗi HTTP request |
| session | Một instance cho mỗi HTTP session |

Bean Spring mặc định là singleton. <code>ConversationService</code>, <code>ChatService</code> và controller trong repo đều được nhiều request dùng chung.

### Singleton không đồng nghĩa với Java Singleton pattern

Spring singleton nghĩa là một instance theo container/bean definition, không nhất thiết một instance duy nhất trong toàn JVM. Hai ApplicationContext có thể có hai instance.

### Singleton service phải stateless

Đúng:

~~~java
public ConversationView findOne(long id, String ownerEmail) {
    return ConversationView.from(requireOwned(id, ownerEmail));
}
~~~

State của request nằm ở parameter/local variable, mỗi thread có call stack riêng.

Sai:

~~~java
private String currentUser;

public ConversationView findOne(long id, String ownerEmail) {
    currentUser = ownerEmail;
    return ...;
}
~~~

Hai request concurrent có thể ghi đè <code>currentUser</code>, gây rò dữ liệu giữa user. Không dùng request scope để che thiết kế sai nếu local variable đã đủ.

---

## 8. Request lifecycle tổng quát

Một request có thể đi theo luồng:

~~~text
Client
  ↓ HTTP
Servlet container
  ↓
Security/CORS filters
  ↓
DispatcherServlet
  ↓
HandlerMapping chọn controller method
  ↓
Argument resolvers + Jackson + Bean Validation
  ↓
Controller
  ↓
Service proxy / transaction
  ↓
Repository proxy
  ↓
JPA/Hibernate
  ↓
Database
  ↑
DTO / ApiResponse
  ↑
Jackson serialize JSON
  ↑
HTTP response
~~~

Nếu có exception:

~~~text
service/controller exception
  ↓
@RestControllerAdvice
  ↓
ApiResponse error + HTTP status
~~~

Security exception có thể xảy ra trong filter chain trước controller nên thường được xử lý bởi security entry point/access denied handler, không phải mọi lỗi đều tới controller advice.

---

## 9. Bước 1 — Servlet filter và SecurityFilterChain

[SecurityConfig.java](../../security/SecurityConfig.java) cấu hình:

~~~java
authorize
    .requestMatchers(
        "/api/v1/auth/email/login",
        "/api/v1/auth/refresh",
        "/h2-console/**"
    )
    .permitAll()
    .anyRequest()
    .authenticated();
~~~

Trước khi controller chạy:

1. CORS policy được áp dụng.
2. Bearer token được resolve.
3. JWT được decode và kiểm tra.
4. Claims được đổi thành Authentication/authorities.
5. Authorization rule quyết định cho đi tiếp hay trả 401/403.

Vì vậy breakpoint đầu tiên khi debug authenticated request có thể nằm trong security layer, không phải controller.

---

## 10. Bước 2 — DispatcherServlet

DispatcherServlet là front controller của Spring MVC. Nó nhận request đã qua filter chain và điều phối:

- Chọn controller method theo path và HTTP method.
- Tạo argument.
- Gọi handler.
- Chuyển return value thành response.
- Tìm exception resolver khi có lỗi.

Ví dụ:

~~~http
POST /api/v1/chat_conversations
~~~

được map tới:

~~~java
@PostMapping
ApiResponse<ConversationView> create(...) {
}
~~~

Mapping không chỉ dựa trên tên method Java. Nó dựa trên annotation, path, HTTP method, content negotiation và các condition khác.

---

## 11. Bước 3 — Tạo method arguments

Controller method:

~~~java
ApiResponse<ConversationView> create(
        @Valid @RequestBody ConversationRequest request,
        Authentication authentication
) {
}
~~~

Spring tạo từng argument bằng cơ chế phù hợp:

- <code>@RequestBody</code>: Jackson đọc JSON thành record.
- <code>@Valid</code>: Bean Validation kiểm constraint.
- <code>Authentication</code>: lấy từ SecurityContext.
- <code>@PathVariable</code>: lấy segment trong URL.
- <code>@RequestParam</code>: lấy query parameter và convert type.

Nếu JSON sai cú pháp hoặc type không convert được, controller có thể chưa được gọi.

---

## 12. Bước 4 — Controller là HTTP adapter

Controller nên:

- Nhận HTTP input.
- Validate syntax/boundary.
- Lấy identity đã xác thực.
- Chuyển request DTO thành command nếu cần.
- Gọi use case.
- Chọn status và response contract.

Controller không nên:

- Viết SQL.
- Giữ transaction dài.
- Chứa toàn bộ business rule.
- Tin owner ID/email do client gửi.
- Gọi trực tiếp hàng loạt third-party SDK.

Code hiện tại làm ownership đúng:

~~~java
service.create(
        request.title(),
        authentication.getName()
);
~~~

Owner lấy từ authenticated principal, không phải JSON.

---

## 13. Bước 5 — Service là use-case boundary

[ConversationService.java](../../conversation/ConversationService.java) thực hiện:

- Chuẩn hóa pagination.
- Query theo owner.
- Tạo/đổi tên/xóa conversation.
- Bảo vệ ownership.
- Chuyển entity thành response view.
- Đặt transaction boundary.

Một service method nên diễn đạt một use case:

~~~java
@Transactional
public ConversationView rename(...) {
    Conversation conversation = requireOwned(id, ownerEmail);
    conversation.rename(title.trim());
    return ConversationView.from(conversation);
}
~~~

Controller không cần biết dirty checking hoặc query cụ thể. Repository không cần biết HTTP status.

---

## 14. Bước 6 — Repository proxy

Repository chỉ là interface:

~~~java
public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByIdAndOwnerEmail(
            Long id,
            String ownerEmail
    );
}
~~~

Spring Data tạo proxy implementation runtime. Proxy:

- Nhận method call.
- Phân tích derived query.
- Dùng EntityManager.
- Bind parameter.
- Trả entity/Page/Optional.

Bạn không thấy class implementation trong source nhưng object runtime vẫn tồn tại. Đây là một ví dụ framework sử dụng proxy và code generation.

---

## 15. Bước 7 — Response serialization

Service trả record <code>ConversationView</code>. Controller bọc bằng:

~~~java
ApiResponse.success("Conversation created", view);
~~~

Jackson đọc record accessor và tạo JSON. Với create, annotation:

~~~java
@ResponseStatus(HttpStatus.CREATED)
~~~

đặt status <code>201 Created</code>.

Response body có shape chung:

~~~json
{
  "code": "SUCCESS",
  "message": "Conversation created",
  "data": {},
  "meta": {}
}
~~~

Trong thực tế, <code>meta</code> chứa các field của <code>ApiMeta</code>, kể cả response không phân trang.

---

## 16. Exception flow

Service có thể ném:

~~~java
throw new NotFoundException(
        "Conversation " + id + " was not found"
);
~~~

[GlobalExceptionHandler.java](../../common/GlobalExceptionHandler.java) bắt loại exception:

~~~java
@ExceptionHandler(NotFoundException.class)
ResponseEntity<ApiResponse<Void>> handleNotFound(...) {
}
~~~

Luồng:

~~~text
Repository không tìm thấy
  ↓
Service ném NotFoundException
  ↓
Controller không catch
  ↓
RestControllerAdvice
  ↓
HTTP 404 + error contract
~~~

Không catch exception ở mỗi layer chỉ để log rồi rethrow. Điều đó tạo log trùng. Hãy log có context ở boundary phù hợp.

---

## 17. Proxy và AOP

Spring thường bọc bean bằng proxy để thêm cross-cutting behavior:

~~~text
Caller
  ↓
Spring proxy
  ├─ mở transaction / kiểm quyền / chuyển thread
  ↓
Target method
  ↓
Spring proxy
  └─ commit/rollback / hoàn tất
~~~

Các annotation trong repo phụ thuộc proxy:

- <code>@Transactional</code>.
- <code>@Async</code>.
- <code>@PreAuthorize</code>.

Annotation không phải câu lệnh tự chạy. Infrastructure của Spring phải intercept method call.

---

## 18. Self-invocation bypass proxy

Giả sử:

~~~java
@Service
class AgentService {
    public void execute() {
        runAsync();
    }

    @Async
    public void runAsync() {
    }
}
~~~

<code>execute()</code> gọi <code>this.runAsync()</code> trong cùng object. Call không đi ra ngoài rồi quay qua proxy, nên <code>@Async</code> có thể không có tác dụng.

Repo tách [AgentTaskRunner.java](../../agent/AgentTaskRunner.java) thành bean riêng:

~~~java
taskRunner.run(executionId, prompt);
~~~

Call từ controller bean sang task runner bean đi qua proxy, vì vậy async interceptor có cơ hội chuyển execution sang executor.

Self-invocation tương tự cũng ảnh hưởng <code>@Transactional</code> và method security.

Các giải pháp:

- Tách responsibility sang bean khác.
- Thiết kế transaction boundary ở public use case.
- Dùng programmatic transaction khi thật sự phù hợp.
- Tránh self-injection hack nếu có thể refactor rõ hơn.

---

## 19. Transaction proxy và rollback

Khi gọi <code>ChatService.send()</code> từ controller:

~~~text
Controller
  ↓ gọi bean
Transactional proxy
  ↓ BEGIN
ChatService.send()
  ↓ repository calls
method return
  ↓ COMMIT
~~~

Nếu runtime exception thoát khỏi method:

~~~text
RuntimeException
  ↓
proxy đánh dấu rollback
  ↓ ROLLBACK
exception đi lên advice
~~~

Mặc định, checked exception không phải lúc nào cũng rollback như runtime exception. Nếu use case cần khác mặc định, cấu hình <code>rollbackFor</code> có chủ đích thay vì đoán.

Private method không phải transaction entry point phù hợp vì proxy không intercept call nội bộ/private theo mô hình thông thường.

---

## 20. Circular dependency

Ví dụ:

~~~text
ConversationService cần ChatService
ChatService cần ConversationService
~~~

Container không thể dễ dàng tạo object hoàn chỉnh theo constructor cycle. Circular dependency thường báo hiệu:

- Hai service chia responsibility chưa tốt.
- Có orchestration nên chuyển sang service thứ ba.
- Domain boundary bị lẫn.
- Một abstraction/event có thể phù hợp hơn.

Đừng dùng field injection hoặc lazy injection chỉ để giấu mọi cycle. Trước hết hãy xem lại thiết kế.

---

## 21. Auto-configuration không phải phép thuật

Dependencies trong [pom.xml](../../../../../../../../pom.xml) cho Spring Boot các tín hiệu:

- Web starter → Spring MVC và embedded server.
- Data JPA starter → EntityManager, transaction manager, repository scanning.
- Security/resource server → filter chain và JWT support.
- Validation starter → Bean Validation.
- H2 runtime → DataSource demo.

Boot kiểm classpath, property và bean hiện có để quyết định cấu hình mặc định. Khi app cung cấp bean riêng, auto-configuration thường “back off” ở điểm tương ứng.

Khi debug auto-configuration, hãy kiểm:

- Dependency có trên classpath không?
- Property có đúng prefix không?
- Bean custom có làm default bean bị bỏ không?
- Package scan có bao phủ class không?
- Condition report nói gì?

---

## 22. Configuration và environment

[application.yml](../../../../../../resources/application.yml) chứa:

~~~yaml
server:
  port: 8080

app:
  jwt:
    access-ttl: PT15M
~~~

Value được inject qua constructor:

~~~java
public TokenService(
        JwtEncoder jwtEncoder,
        @Value("${app.jwt.access-ttl}") Duration accessTtl,
        ...
) {
}
~~~

Spring resolve placeholder thành giá trị cấu hình rồi convert chuỗi thời lượng thành <code>Duration</code>.

Khi nhóm config lớn, <code>@ConfigurationProperties</code> thường type-safe, dễ validate và test hơn nhiều <code>@Value</code> rời rạc.

Không hard-code secret production trong source. Dùng environment/secret manager và rotation phù hợp.

---

## 23. Luồng cụ thể: tạo conversation

Request:

~~~http
POST /api/v1/chat_conversations
Authorization: Bearer access-token
Content-Type: application/json

{
  "title": "Java interview practice"
}
~~~

Sequence:

~~~text
Client
  → SecurityFilterChain: xác thực JWT
  → DispatcherServlet
  → Jackson: JSON thành ConversationRequest
  → Validator: @NotBlank, @Size
  → ConversationController.create()
  → ConversationService proxy: BEGIN
  → ConversationService.create()
  → ConversationRepository proxy
  → Hibernate: INSERT
  → service map entity thành ConversationView
  → transaction proxy: COMMIT
  → controller bọc ApiResponse
  → Jackson serialize
  → HTTP 201
~~~

Nếu title rỗng, flow dừng trước controller business call:

~~~text
Bean Validation fail
  → MethodArgumentNotValidException
  → GlobalExceptionHandler
  → HTTP 400 VALIDATION_ERROR
~~~

---

## 24. Cách debug request lifecycle

Chạy:

~~~powershell
./mvnw.cmd spring-boot:run
~~~

Hoặc chạy <code>JavaBeginApplication</code> bằng IDE ở debug mode.

Đặt breakpoint theo thứ tự:

1. <code>SecurityConfig.authorities</code>.
2. <code>ConversationController.create</code>.
3. <code>ConversationService.create</code>.
4. Entity callback <code>Conversation.onCreate</code>.
5. <code>GlobalExceptionHandler.handleValidation</code>.

Quan sát:

- Thread name.
- Runtime class của injected repository; thường là proxy.
- <code>Authentication.getName()</code>.
- Request DTO.
- Entity ID trước/sau save.
- Call stack.
- SQL log.

Dùng [requests.http](../../../../../../../../requests.http) hoặc [Postman collection](../../../../../../../../postman/README.md) để gửi request lặp lại.

---

## 25. Test không cần khởi động Spring khi nào?

Constructor injection cho phép unit test thuần Java:

~~~java
ConversationRepository fake = new InMemoryConversationRepository();
ConversationService service = new ConversationService(fake);
~~~

Không cần ApplicationContext nếu đang test business logic độc lập.

Dùng Spring integration test khi cần kiểm:

- Component wiring.
- Transaction thật.
- JPA mapping/query.
- MVC JSON/validation.
- Security filter chain.

Đừng dùng <code>@SpringBootTest</code> cho mọi test; context start chậm và làm unit test kém tập trung.

---

## 26. Những lỗi thường gặp

### Dùng field injection

Dependency ẩn, khó test và object có thể tồn tại chưa hoàn chỉnh. Ưu tiên constructor.

### Để mutable request state trong singleton

Nhiều thread dùng cùng instance, gây race condition và rò dữ liệu.

### Controller chứa toàn bộ nghiệp vụ

Khó tái sử dụng, khó transaction và khó unit test. Controller nên là adapter mỏng.

### Service trả JPA entity thẳng ra API

Gây coupling schema, lazy serialization và lộ field. Map sang DTO/view.

### Nghĩ annotation tự chạy

Nhiều annotation phụ thuộc bean lifecycle/proxy. Object tạo bằng <code>new</code> ngoài container không được Spring intercept.

### Self-invocation với <code>@Async</code>/<code>@Transactional</code>

Call nội bộ bypass proxy nên behavior không được áp dụng như kỳ vọng.

### Catch mọi exception trong controller

Làm contract lỗi không nhất quán. Dùng exception type rõ và advice tập trung.

### Tạo circular dependency rồi dùng workaround ngay

Cycle thường là tín hiệu responsibility chưa rõ. Refactor trước khi dùng lazy reference.

---

## 27. Bài thực hành

### Bài 1 — Vẽ bean graph

Vẽ dependency:

~~~text
ConversationController
  → ConversationService
    → ConversationRepository

ChatController
  → ChatService
    → ConversationService
    → ChatTurnRepository
~~~

Đánh dấu bean nào là proxy và annotation nào cần proxy.

### Bài 2 — Sequence diagram

Vẽ POST conversation từ browser tới database và trở lại. Phải có filter chain, DispatcherServlet, validation, transaction proxy và exception advice.

### Bài 3 — Unit test thủ công

Tạo fake repository và khởi tạo service bằng constructor. Kiểm create, find owned và not found mà không start Spring.

### Bài 4 — Quan sát proxy

In hoặc debug runtime class của:

- ConversationService.
- ConversationRepository.
- AgentTaskRunner.

Giải thích vì sao runtime class có thể khác source class.

### Bài 5 — Race condition singleton

Trong một branch học, thêm field <code>currentUser</code> vào service, gửi nhiều request concurrent và giải thích vì sao kết quả không an toàn. Sau đó xóa field và dùng local variable.

### Bài 6 — Self-invocation

Tạo một demo nhỏ có public method gọi method <code>@Async</code> trong cùng class. Quan sát thread, rồi tách method sang bean khác và so sánh.

### Bài 7 — Error flow

Gửi lần lượt:

- JSON hợp lệ.
- Title rỗng.
- Không có JWT.
- ID không tồn tại.

Ghi layer nào chặn request và status nào được trả.

---

## 28. Câu hỏi phỏng vấn và đáp án ngắn

### IoC là gì?

Là việc framework/container điều khiển việc tạo, cấu hình, lifecycle và phối hợp object thay vì application tự làm tất cả.

### DI là gì?

Là cung cấp dependency từ bên ngoài object, thường qua constructor.

### Bean là gì?

Là object được Spring container tạo và quản lý.

### Vì sao constructor injection tốt hơn field injection?

Dependency rõ, bắt buộc, có thể final, dễ test và không cần reflection.

### Scope mặc định của Spring bean?

Singleton trong một ApplicationContext.

### Singleton service có thread-safe tự động không?

Không. Service phải tránh mutable shared state hoặc đồng bộ đúng cách.

### <code>@Component</code> khác <code>@Service</code>?

Service là stereotype chuyên biệt diễn đạt role business/use case; cả hai đều tạo component được scan.

### Repository interface có implementation ở đâu?

Spring Data tạo proxy implementation runtime dựa trên interface và metadata.

### DispatcherServlet làm gì?

Nó là front controller của Spring MVC, chọn handler, resolve argument, gọi controller và xử lý response/exception.

### Spring AOP proxy dùng để làm gì?

Chèn cross-cutting behavior như transaction, async và method authorization quanh method call.

### Vì sao self-invocation là vấn đề?

Call qua <code>this</code> không đi qua proxy nên interceptor có thể không chạy.

### Circular dependency nói lên điều gì?

Thường cho thấy responsibility/boundary bị rối hoặc thiếu một abstraction/orchestrator phù hợp.

### Khi nào dùng <code>@SpringBootTest</code>?

Khi cần kiểm toàn bộ wiring/integration; không cần cho logic unit nhỏ có thể khởi tạo trực tiếp.

---

## 29. Liên hệ với SoftAIBox

Frontend React/TanStack Query chỉ nhìn thấy HTTP contract, nhưng backend flow thực tế là:

~~~text
Query/mutation hook
  → Axios request
  → Security filters
  → Controller DTO
  → Service use case
  → Repository
  → Database
  → ApiResponse
  → Query cache
~~~

Khi UI nhận 400, 401, 403, 404 hoặc 500, cần xác định lỗi phát sinh ở boundary nào. Việc hiểu request lifecycle giúp không “debug mù” từ frontend.

Kiến trúc lớp trong mini backend cũng tạo vocabulary để phỏng vấn:

- Controller là inbound adapter.
- Service orchestration use case.
- Repository là persistence abstraction.
- Entity bảo vệ state.
- DTO giữ public contract.
- Security filter xác thực trước business code.

---

## 30. Kế hoạch học 4–6 giờ

### 60 phút đầu

- Học IoC, DI, bean và component scan.
- Vẽ bean graph của conversation/chat.
- Tự nối cùng graph bằng <code>new</code> để thấy container thay mình làm gì.

### 60 phút tiếp

- Đọc controller → service → repository.
- Vẽ request lifecycle.
- Phân biệt HTTP, business và persistence responsibility.

### 60–90 phút tiếp

- Học scope và thread safety.
- Tạo ví dụ singleton mutable state.
- Refactor thành local state.

### 60–90 phút tiếp

- Học proxy, transaction và self-invocation.
- Đặt breakpoint ở <code>AgentTaskRunner</code>.
- Quan sát thread trước/sau async boundary.

### 45 phút cuối

- Chạy bốn error cases.
- Trả lời câu hỏi phỏng vấn thành tiếng.
- Tự giải thích POST conversation trong dưới ba phút.

---

## Definition of Done

Bạn hoàn thành ngày 11 khi có thể:

- Phân biệt IoC và DI bằng ví dụ.
- Giải thích Spring container tạo bean graph hiện tại ra sao.
- Dùng constructor injection không cần nhìn mẫu.
- Phân biệt các stereotype chính.
- Giải thích singleton scope và nguy cơ mutable field.
- Lần request từ filter tới JSON response.
- Nêu trách nhiệm controller, service và repository.
- Giải thích repository runtime proxy.
- Giải thích proxy đứng sau transaction/async/method security.
- Mô tả self-invocation và cách refactor.
- Theo exception tới <code>GlobalExceptionHandler</code>.
- Vẽ sequence diagram POST conversation.
- Đặt breakpoint và quan sát identity, DTO, service, entity và SQL.
- Chỉ ra ít nhất ba điểm Spring tự cấu hình nhờ dependency/property.
- Trả lời bộ câu hỏi phỏng vấn mà không nhìn tài liệu.
