# Ngày 8 — SOLID, design pattern và kiến trúc lớp

Ngày 8 không nhằm học thuộc năm chữ cái hay nhồi pattern vào mọi class. Mục tiêu là nhận ra code khó thay đổi vì trách nhiệm lẫn lộn, dependency sai hướng hoặc contract không rõ, rồi refactor vừa đủ.

Code trong cùng thư mục:

- [DesignPatternsDemo.java](./DesignPatternsDemo.java): Strategy + Factory cho kênh notification của SoftAIBox.

## Mục tiêu

Sau ngày 8, bạn cần:

1. Giải thích từng nguyên lý SOLID bằng ví dụ thực tế.
2. Nhận diện dấu hiệu vi phạm thay vì áp dụng máy móc.
3. Phân biệt dependency inversion và dependency injection.
4. Hiểu Strategy, Factory, Adapter, Observer và Builder.
5. Đọc được hướng dependency controller → service → repository.
6. Biết pattern là vocabulary, không phải mục tiêu.
7. Phân tích đầy đủ `DesignPatternsDemo`.
8. Mở rộng thêm kênh notification với thay đổi tối thiểu.

---

## 1. Vì sao cần nguyên lý thiết kế?

Code chạy đúng hôm nay chưa chắc dễ thay đổi ngày mai. Các dấu hiệu thường gặp:

- Một class sửa vì quá nhiều lý do.
- Thêm một loại mới phải sửa nhiều `if/else`.
- Test business rule buộc khởi động database/network.
- Interface lớn khiến implementation có method không dùng.
- Class cấp cao tạo trực tiếp SDK hoặc repository cụ thể.

SOLID giúp quản lý **chi phí thay đổi**, không bảo đảm code tự động tốt. Với chương trình nhỏ, tách quá nhiều lớp cũng tạo chi phí đọc và điều hướng.

## 2. S — Single Responsibility Principle

> Một module/class nên có một lý do thay đổi.

Ví dụ class làm quá nhiều việc:

```java
class AgentService {
    void executeAgent() {
        // validate request
        // query database
        // call model
        // format HTML
        // send email
    }
}
```

Nó thay đổi khi:

- Rule agent đổi.
- Database đổi.
- AI provider đổi.
- Email template đổi.
- Kênh notification đổi.

Có thể tách theo trách nhiệm:

```text
AgentService          : orchestration/use case
AgentRepository       : persistence
ModelClient           : gọi model
NotificationStrategy  : gửi notification
```

SRP không có nghĩa “mỗi class chỉ có một method”. Nhiều method có thể cùng phục vụ một trách nhiệm thống nhất.

Controller không nên chứa SQL; repository không nên quyết định gửi email; DTO không nên điều phối transaction.

## 3. O — Open/Closed Principle

> Mở cho extension, đóng cho modification.

Phiên bản chuỗi điều kiện:

```java
void send(Channel channel, String receiver, String message) {
    if (channel == Channel.EMAIL) {
        sendEmail(receiver, message);
    } else if (channel == Channel.DASHBOARD) {
        saveDashboardNotification(receiver, message);
    }
}
```

Mỗi kênh mới làm method trung tâm dài thêm. Strategy tách biến thể:

```java
interface NotificationStrategy {
    void send(String receiver, String message);
}
```

Mỗi implementation chịu trách nhiệm riêng:

```java
final class EmailNotification
        implements NotificationStrategy {
    @Override
    public void send(String receiver, String message) {
        // send email
    }
}
```

Thêm Slack bằng class mới và wiring mới, không sửa logic của Email/Dashboard hoặc thuật toán chọn trong factory.

OCP không có nghĩa không bao giờ sửa file cũ. Composition root/configuration vẫn cần biết implementation nào được cài. Mục tiêu là giảm sửa logic ổn định và giảm nguy cơ phá nhánh cũ.

## 4. L — Liskov Substitution Principle

> Subtype phải dùng thay parent mà không phá contract mong đợi.

Giả sử contract:

```java
interface FileStorage {
    URI store(byte[] content);
}
```

Nếu implementation đôi lúc trả `null` dù contract hứa URI, caller sẽ vỡ khi thay implementation. Vi phạm không chỉ là khác kiểu; nó là phá kỳ vọng hành vi.

Subtype không nên:

- Yêu cầu precondition chặt hơn bất ngờ.
- Làm yếu postcondition đã hứa.
- Phá invariant.
- Ném exception không phù hợp contract.
- Thay đổi semantics khiến caller phải `instanceof`.

Ví dụ notification:

```java
void notifyUser(NotificationStrategy strategy) {
    strategy.send("user-01", "Agent completed");
}
```

Mọi strategy phải tôn trọng contract chung. Nếu một strategy âm thầm yêu cầu receiver là email nhưng interface không biểu đạt điều đó, abstraction đang quá chung hoặc model receiver cần thiết kế lại.

LSP đặt câu hỏi: “Caller chỉ biết interface có dùng implementation này an toàn không?”

## 5. I — Interface Segregation Principle

> Client không nên phụ thuộc method nó không dùng.

Interface quá lớn:

```java
interface AgentGateway {
    Agent read(String id);
    void save(Agent agent);
    void delete(String id);
    void exportCsv();
    void sendNotification();
}
```

Component chỉ đọc agent vẫn phụ thuộc tất cả chức năng. Có thể tách:

```java
interface AgentReader {
    Agent read(String id);
}

interface AgentWriter {
    void save(Agent agent);
}
```

Lợi ích:

- Contract theo nhu cầu client.
- Fake/mock nhỏ hơn.
- Implementation không có method rỗng hoặc `UnsupportedOperationException`.

Không tách mỗi method thành một interface theo công thức. Hãy tách theo vai trò/cohesion thật.

## 6. D — Dependency Inversion Principle

> Policy cấp cao không phụ thuộc chi tiết cấp thấp; cả hai phụ thuộc abstraction.

Coupling trực tiếp:

```java
class BillingService {
    private final StripeSdk stripe = new StripeSdk();
}
```

Business policy bị gắn với SDK:

```java
interface PaymentGateway {
    PaymentResult charge(Money amount);
}

class BillingService {
    private final PaymentGateway gateway;

    BillingService(PaymentGateway gateway) {
        this.gateway = gateway;
    }
}
```

Adapter cấp thấp:

```java
class StripePaymentGateway implements PaymentGateway {
    // dịch contract domain sang Stripe SDK
}
```

Hướng dependency:

```text
BillingService ----> PaymentGateway <---- Stripe adapter
 policy cao            abstraction          chi tiết thấp
```

### Dependency inversion khác dependency injection

- **DIP** là nguyên lý về hướng dependency.
- **DI** là kỹ thuật truyền dependency từ bên ngoài.
- Spring IoC là framework/container giúp thực hiện DI.

Constructor injection một concrete class vẫn là DI nhưng có thể chưa đạt DIP nếu policy vẫn phụ thuộc chi tiết sai tầng.

---

## 7. Strategy pattern

Strategy đóng gói các thuật toán/hành vi có thể thay thế cùng một contract:

```java
interface NotificationStrategy {
    void send(String receiver, String message);
}
```

Các strategy:

```java
EmailNotification
DashboardNotification
SlackNotification
```

Context chọn strategy rồi gọi contract, không biết chi tiết gửi:

```java
NotificationStrategy strategy =
        factory.forChannel(channel);
strategy.send(receiver, message);
```

Strategy phù hợp khi:

- Có nhiều cách thực hiện cùng một hành vi.
- Cách được chọn lúc runtime.
- Chuỗi `if/else` theo type/channel tăng dần.
- Mỗi biến thể cần test riêng.

Không cần Strategy nếu chỉ có một thuật toán ổn định và không có nhu cầu thay đổi; thêm interface lúc đó có thể là abstraction sớm.

## 8. Factory pattern

Factory tập trung cách chọn/tạo implementation:

```java
NotificationStrategy forChannel(Channel channel) {
    NotificationStrategy strategy = strategies.get(channel);
    if (strategy == null) {
        throw new IllegalArgumentException(
                "Unsupported channel: " + channel);
    }
    return strategy;
}
```

Caller không cần:

```java
if (channel == EMAIL) new EmailNotification();
```

Trong demo, factory là registry-backed factory: map chứa `Channel -> Strategy`.

Factory không phải lúc nào cũng cần class tên `Factory`. Spring container có thể wiring map/list các implementation, còn service chọn theo key.

Phân biệt:

- Strategy: hành vi nào được thực hiện.
- Factory: lấy/tạo object nào thực hiện hành vi.

## 9. Một số pattern liên quan

### Adapter

Bọc API không khớp contract ứng dụng:

```text
ModelClient (domain port)
     ^
OpenAiModelAdapter -> OpenAI SDK
```

Adapter dịch request, response và exception của third-party. Business service không phụ thuộc SDK trực tiếp.

### Observer/Event

Publisher phát sự kiện, nhiều listener phản ứng:

```text
AgentCompleted
  -> notification listener
  -> audit listener
  -> metric listener
```

Observer giảm coupling trực tiếp nhưng làm luồng khó theo dõi hơn. Event in-memory không bảo đảm bền vững; nghiệp vụ quan trọng có thể cần transactional outbox/message broker.

### Builder

Hữu ích khi object có nhiều tùy chọn:

```java
AgentRequest request = AgentRequest.builder()
        .name("reviewer")
        .model("gpt")
        .timeout(seconds)
        .build();
```

Builder không thay validation/invariant. Với vài field bắt buộc, constructor hoặc record thường đơn giản hơn.

### Repository

Che chi tiết persistence sau contract theo aggregate/query cần thiết. Repository không nên chỉ là tên mới cho mọi method database và không nên chứa business orchestration.

### Decorator

Bọc object cùng interface để thêm caching, logging hoặc retry:

```text
RetryingModelClient
  -> MetricsModelClient
    -> HttpModelClient
```

Thứ tự decorator có thể thay đổi semantics.

---

## 10. Phân tích `DesignPatternsDemo`

Khởi tạo factory:

```java
NotificationFactory factory = new NotificationFactory(
        Map.of(
                Channel.EMAIL, new EmailNotification(),
                Channel.DASHBOARD,
                new DashboardNotification()
        )
);
```

Map là composition/wiring: key xác định implementation.

Gọi:

```java
factory.forChannel(Channel.EMAIL)
        .send("dat@example.com", "Agent completed");
```

Luồng:

```text
Channel.EMAIL
  -> NotificationFactory.forChannel
  -> Map lookup
  -> EmailNotification
  -> send
```

Interface là Strategy contract:

```java
interface NotificationStrategy {
    void send(String receiver, String message);
}
```

Hai class là concrete strategy. `NotificationFactory` không biết SMTP hay database; nó chỉ biết mapping và contract.

Constructor defensive copy:

```java
this.strategies = Map.copyOf(strategies);
```

Điều này ngăn caller sửa map registration sau khi factory được tạo. Nó là shallow copy: strategy object bên trong không tự trở thành immutable.

Nếu không tìm thấy:

```java
throw new IllegalArgumentException(
        "Unsupported channel: " + channel);
```

Fail-fast tốt hơn trả `null` rồi gây `NullPointerException` xa nguyên nhân.

`null` channel cũng dẫn đến không có strategy và exception “Unsupported channel: null”; hệ thống thật có thể validate `channel` riêng để thông báo rõ hơn.

Output:

```text
EMAIL to dat@example.com: Agent completed
DASHBOARD for user-01: New datasource was shared
```

## 11. Thêm Slack đúng cách

Mở rộng enum:

```java
enum Channel {
    EMAIL, DASHBOARD, SLACK
}
```

Thêm strategy:

```java
final class SlackNotification
        implements NotificationStrategy {
    @Override
    public void send(String receiver, String message) {
        System.out.printf(
                "SLACK to %s: %s%n",
                receiver,
                message
        );
    }
}
```

Đăng ký ở composition root:

```java
Map.of(
        Channel.EMAIL, new EmailNotification(),
        Channel.DASHBOARD, new DashboardNotification(),
        Channel.SLACK, new SlackNotification()
)
```

Không sửa `EmailNotification`, `DashboardNotification` hay algorithm của `forChannel`.

Trong Spring, implementation có thể là bean và wiring tại configuration. Không nên gọi `new` rải rác trong business service.

## 12. Kiến trúc lớp

Với CRUD vừa phải:

```text
HTTP
  -> Controller
  -> Service/use case
  -> Repository
  -> Database
```

### Controller

- Parse HTTP/DTO.
- Trigger validation boundary.
- Chuyển kết quả thành status/response.
- Không chứa SQL và business workflow dài.

### Service

- Điều phối use case.
- Kiểm ownership/business rule.
- Xác định transaction boundary.
- Phụ thuộc repository/client abstraction.

### Repository

- Truy vấn và persistence.
- Không gửi email hoặc quyết định HTTP status.

### Adapter/client

- Bọc external API/SDK.
- Chuyển exception và data model ngoài sang contract nội bộ.

Layering không có nghĩa mọi request bắt buộc đi qua class rỗng chỉ forward method. Domain phức tạp có thể tách use case/domain/port/adapter; CRUD nhỏ không cần “clean architecture” hình thức.

## 13. Composition root

Composition root là nơi nối object graph:

```java
NotificationFactory factory =
        new NotificationFactory(strategies);
```

Trong Spring, `@Configuration`, `@Bean` và component scanning thường đảm nhận wiring.

Business class nên nhận dependency đã sẵn sàng:

```java
class AgentService {
    private final NotificationFactory factory;

    AgentService(NotificationFactory factory) {
        this.factory = factory;
    }
}
```

Không dùng service locator/global singleton để dependency bị ẩn. Constructor làm contract phụ thuộc rõ và test dễ hơn.

---

## 14. Trade-off và overengineering

Pattern có chi phí:

- Nhiều file/type.
- Luồng gọi gián tiếp.
- Onboarding lâu hơn.
- Debug qua nhiều lớp.

Trước khi tạo abstraction, hỏi:

1. Có ít nhất hai biến thể thật hoặc thay đổi rất gần không?
2. Chúng có contract ổn định chung không?
3. Tách ra có làm business code dễ đọc/test hơn không?
4. Dependency có thực sự cần đảo hướng không?

Không tạo `IUserService`, `UserServiceImpl`, `UserServiceFactory` chỉ vì “pattern”. Interface có giá trị khi thể hiện boundary/role, có nhiều implementation, hoặc giúp policy không phụ thuộc chi tiết.

## 15. Liên hệ SoftAIBox

### Notification

Strategy cho EMAIL, DASHBOARD, SLACK; Factory/registry chọn theo channel. Production còn cần retry, idempotency, template, user preference và delivery status.

### AI provider

```text
ModelClient
  <- OpenAIAdapter
  <- AnthropicAdapter
  <- FakeModelClient trong test
```

Agent service phụ thuộc `ModelClient`, không phụ thuộc SDK cụ thể.

### Auth/token

```text
AuthController -> TokenService -> TokenEncoder
                                      ^
                                  JWT adapter
```

### Frontend liên hệ

Nếu một component React vừa fetch, format data, quản modal và toast, nó có nhiều lý do thay đổi. Có thể tách query hook, formatter và presentational component, nhưng chỉ khi ranh giới làm code rõ hơn.

### Kiến trúc và transaction

Strategy gửi network notification không nên chạy mù quáng trong transaction dài. Có thể lưu event/outbox rồi gửi sau commit để tránh DB rollback nhưng email đã gửi.

## 16. Lỗi thường gặp

- Học thuộc chữ SOLID nhưng không nêu được trade-off.
- Hiểu SRP là “mỗi class một method”.
- Hiểu OCP là “không bao giờ sửa code cũ”.
- Subtype làm caller phải `instanceof`.
- Interface lớn với method rỗng/unsupported.
- Gọi SDK trực tiếp trong domain service.
- Nhầm DI với DIP.
- Thêm pattern khi chưa có vấn đề.
- Factory trả `null` thay vì fail rõ.
- Dùng global service locator làm dependency ẩn.
- Event hóa mọi method khiến flow khó theo dõi.
- Repository chứa cả HTTP, email và business workflow.
- Tạo layer chỉ forward mà không thêm semantics.

## 17. Bài thực hành

1. Thêm `SLACK` strategy mà không sửa hai strategy có sẵn.
2. Viết test factory trả đúng implementation cho từng channel.
3. Test unsupported và `null` channel.
4. Tạo `ModelClient` cùng fake và HTTP adapter.
5. Vẽ dependency `AuthController -> TokenService -> TokenEncoder`.
6. Refactor một class giả định vừa query DB vừa gửi email theo SRP.
7. Tách một interface “god interface” theo client role.
8. Viết ví dụ vi phạm LSP và sửa contract.
9. Dùng Builder cho object nhiều optional field, rồi giải thích khi record tốt hơn.
10. Tìm một chỗ có `if/else` nhưng **không** cần Strategy và bảo vệ quyết định đó.

## 18. Câu hỏi phỏng vấn và đáp án ngắn

### SOLID dùng để làm gì?

Giảm coupling, làm trách nhiệm/contract rõ và giảm chi phí thay đổi; không phải luật tạo càng nhiều class càng tốt.

### SRP có nghĩa một class chỉ một method?

Không. Nó nghĩa class có một trách nhiệm kết dính và một nhóm tác nhân/lý do thay đổi.

### OCP có nghĩa không sửa file cũ?

Không tuyệt đối. Ta mở rộng biến thể mà ít sửa logic ổn định; wiring vẫn có thể thay đổi.

### LSP kiểm tra điều gì?

Subtype có thay parent mà không phá contract và kỳ vọng của caller hay không.

### ISP giúp gì?

Client chỉ phụ thuộc contract nhỏ đúng nhu cầu, tránh method thừa và implementation giả.

### DIP khác DI?

DIP là nguyên lý hướng dependency về abstraction; DI là kỹ thuật cung cấp dependency từ bên ngoài.

### Strategy khác Factory?

Strategy đóng gói hành vi thay thế; Factory chọn hoặc tạo object thực hiện hành vi.

### Adapter dùng khi nào?

Khi API/SDK bên ngoài không khớp contract nội bộ và cần lớp dịch/cách ly.

### Pattern có luôn tốt?

Không. Pattern giải quyết lực thiết kế cụ thể và mang chi phí abstraction; áp dụng không có nhu cầu là overengineering.

---

## 19. Cách chạy

```powershell
java .\src\main\java\vn\dangquangdat\javabegin\learning\day08\DesignPatternsDemo.java
```

Kết quả:

```text
EMAIL to dat@example.com: Agent completed
DASHBOARD for user-01: New datasource was shared
```

Biên dịch/test:

```powershell
.\mvnw.cmd compile
.\mvnw.cmd test
```

Đặt breakpoint ở `forChannel`, quan sát key lookup, runtime type của strategy và Call Stack.

## Definition of done

Bạn hoàn thành ngày 8 khi có thể:

- Giải thích từng chữ SOLID bằng ví dụ và phản ví dụ.
- Phân biệt DIP với DI.
- Chỉ ra Strategy và Factory trong demo.
- Thêm Slack strategy với thay đổi tập trung ở extension/wiring.
- Giải thích `Map.copyOf` và fail-fast.
- Mô tả Adapter, Observer, Builder, Repository.
- Vẽ hướng dependency controller–service–repository/client.
- Nhận diện overengineering.
- Tự viết lại demo và test factory.
- Liên hệ notification/model provider của SoftAIBox.
