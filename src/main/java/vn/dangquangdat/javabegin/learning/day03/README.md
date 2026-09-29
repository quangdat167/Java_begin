# Ngày 3 — OOP: encapsulation, abstraction và polymorphism

Ngày 2 giúp chương trình ra quyết định và chia logic thành method. Ngày 3 học cách
gom **dữ liệu và hành vi có liên quan** thành object, đồng thời làm cho các phần của
hệ thống giao tiếp qua contract rõ ràng.

Code thực hành:

- [OopDemo.java](./OopDemo.java): mô phỏng thanh toán subscription qua nhiều
  `PaymentGateway`, đồng thời minh họa interface, implementation, enum và record.

## Mục tiêu của ngày 3

Sau khi học xong, bạn cần tự giải thích được:

1. Class khác object thế nào.
2. State, behavior, constructor và invariant là gì.
3. Encapsulation không chỉ là private field cộng getter/setter.
4. Abstraction và interface giúp giảm coupling ra sao.
5. Polymorphism và dynamic dispatch hoạt động thế nào.
6. Overriding khác overloading thế nào.
7. Khi nào dùng interface, abstract class hoặc concrete class.
8. Vì sao thường ưu tiên composition over inheritance.
9. `enum` tốt hơn magic string ở điểm nào.
10. `record` phù hợp cho loại object nào và “shallow immutable” nghĩa là gì.
11. Vì sao `equals()` và `hashCode()` phải nhất quán.

---

## 1. Chạy và đọc code mẫu

Từ thư mục gốc dự án:

```powershell
java .\src\main\java\vn\dangquangdat\javabegin\learning\day03\OopDemo.java
```

Kết quả:

```text
Stripe charged 29.90
Waiting for bank transfer of 29.90
```

Luồng chạy:

```text
main
  ├─ tạo StripeGateway
  ├─ tạo BankTransferGateway
  ├─ đặt cả hai vào List<PaymentGateway>
  ├─ tạo Subscription
  └─ duyệt từng gateway
       ├─ gateway.charge(subscription)
       ├─ runtime chọn implementation
       └─ in PaymentResult.message
```

Demo gọi cả hai gateway với cùng subscription để thấy polymorphism. Trong production,
một payment attempt thường chỉ chọn đúng một gateway; không được vô tình thu tiền
hai lần.

---

## 2. Class và object

Class là bản mô tả một loại đối tượng; object là instance cụ thể được tạo theo bản mô
tả đó.

```java
class Counter {
    private int value;

    void increment() {
        value++;
    }

    int value() {
        return value;
    }
}

Counter first = new Counter();
Counter second = new Counter();
```

`first` và `second` là hai object độc lập. Cùng class không có nghĩa là cùng state.

Một object thường có:

- **Identity**: đây là object nào.
- **State**: dữ liệu hiện tại của object.
- **Behavior**: operation object cho phép.

Trong domain SoftAIBox:

```text
Subscription object
  state: id, plan, amount, status, expiresAt
  behavior: activate(), cancel(), renew()
  invariant: amount không âm, status chỉ chuyển theo rule hợp lệ
```

OOP không có nghĩa mọi danh từ đều phải thành class. Một class chỉ có getter/setter
nhưng không bảo vệ rule thường là “túi dữ liệu”, chưa tận dụng tốt object model.

---

## 3. Field, method và constructor

### 3.1 Field giữ state

```java
final class Subscription {
    private final String id;
    private Plan plan;
    private BigDecimal amount;
}
```

### 3.2 Method thể hiện behavior

Thay vì để code bên ngoài sửa trạng thái tùy ý:

```java
subscription.setStatus(Status.CANCELLED);
```

Ưu tiên một operation diễn đạt nghiệp vụ:

```java
subscription.cancel();
```

`cancel()` có thể kiểm tra subscription đã hết hạn hay đã bị hủy chưa, ghi thời điểm
hủy và phát domain event. Tên method nói rõ intent hơn setter.

### 3.3 Constructor tạo object hợp lệ

```java
Subscription(String id, Plan plan, BigDecimal amount) {
    if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("id is required");
    }
    if (plan == null) {
        throw new IllegalArgumentException("plan is required");
    }
    if (amount == null || amount.signum() < 0) {
        throw new IllegalArgumentException("amount must not be negative");
    }

    this.id = id;
    this.plan = plan;
    this.amount = amount;
}
```

`this.id` là field của object; `id` là parameter. Constructor nên đảm bảo object không
được sinh ra trong trạng thái vô nghĩa.

---

## 4. Encapsulation và invariant

Encapsulation là giữ chi tiết state bên trong một ranh giới và chỉ cho phép thay đổi
qua operation an toàn.

### 4.1 Access modifier

| Modifier | Có thể truy cập từ |
|---|---|
| `private` | Chính class đó |
| không ghi modifier | Cùng package |
| `protected` | Cùng package và subclass theo quy tắc Java |
| `public` | Mọi nơi nhìn thấy class |

Không nên chọn `public` theo thói quen. Public API càng lớn thì càng nhiều code có thể
phụ thuộc vào chi tiết implementation.

### 4.2 Invariant

Invariant là điều phải luôn đúng với object hợp lệ, ví dụ:

- Amount không âm.
- Subscription ACTIVE mới được cancel.
- Conversation title không blank.
- User email đã normalize và hợp lệ.

Object nên bảo vệ invariant tại constructor và mọi operation làm đổi state.

### 4.3 Vì sao setter tự do có thể nguy hiểm?

```java
subscription.setAmount(new BigDecimal("-100"));
subscription.setStatus(null);
```

Nếu mọi field đều có setter, caller có thể tạo tổ hợp state không hợp lệ. Một API như
`changePlan(newPlan, newPrice)` có thể cập nhật đồng thời các field liên quan và kiểm
tra rule ở một nơi.

Encapsulation không có nghĩa không bao giờ dùng DTO có accessor. DTO ở boundary chủ
yếu vận chuyển dữ liệu; domain object mới là nơi behavior và invariant quan trọng.

---

## 5. Interface là contract

Code mẫu khai báo:

```java
interface PaymentGateway {
    PaymentResult charge(Subscription subscription);
}
```

Interface nói rằng một payment gateway có khả năng nhận subscription và trả
`PaymentResult`. Nó không nói Stripe SDK, HTTP request hay bank transfer được thực
hiện ra sao.

Method interface trên mặc định là `public abstract`. Class implementation phải cung
cấp implementation tương thích:

```java
static final class StripeGateway implements PaymentGateway {
    @Override
    public PaymentResult charge(Subscription subscription) {
        return new PaymentResult(
                true,
                "Stripe charged " + subscription.amount()
        );
    }
}
```

`@Override` giúp compiler bắt lỗi sai tên hoặc sai signature.

### Dependency direction

```text
PaymentService ─────depends on────> PaymentGateway
                                      ▲
                                      │ implements
                     ┌────────────────┴────────────────┐
                     │                                 │
              StripeGateway                  BankTransferGateway
```

Business service phụ thuộc contract ổn định. Adapter cụ thể phụ thuộc Stripe hoặc hệ
thống ngân hàng. Đây là nền cho dependency inversion và dependency injection.

---

## 6. Abstraction

Abstraction tập trung vào điều caller cần biết và che chi tiết không cần thiết.

Caller cần biết:

```java
PaymentResult result = gateway.charge(subscription);
```

Caller không cần biết:

- URL Stripe API.
- Cách ký request.
- Timeout và retry policy.
- Cấu trúc JSON của nhà cung cấp.
- Cách xác minh webhook.

Abstraction tốt không chỉ “che code”; nó tạo vocabulary đúng với domain. Contract quá
chung như `execute(Object data)` thường làm mất type safety và intent.

Một abstraction có giá trị khi có ranh giới thay đổi hoặc cần test độc lập. Không cần
tạo interface cho mọi class một cách máy móc.

---

## 7. Polymorphism và dynamic dispatch

Code mẫu có:

```java
List<PaymentGateway> gateways = List.of(
        new StripeGateway(),
        new BankTransferGateway()
);

for (PaymentGateway gateway : gateways) {
    PaymentResult result = gateway.charge(subscription);
    System.out.println(result.message());
}
```

Kiểu khai báo của `gateway` là `PaymentGateway`. Object runtime có thể là
`StripeGateway` hoặc `BankTransferGateway`.

```text
gateway reference
      │
      ├─> StripeGateway object       → StripeGateway.charge()
      └─> BankTransferGateway object → BankTransferGateway.charge()
```

Runtime chọn implementation phù hợp với object thật. Đó là dynamic dispatch.

Nhờ polymorphism, caller không cần:

```java
if (type.equals("STRIPE")) {
    // Stripe logic
} else if (type.equals("BANK")) {
    // Bank logic
}
```

Khi thêm `MockPaymentGateway` cho test, business service vẫn gọi cùng contract.

---

## 8. Overriding và quy tắc cơ bản

Override cần:

- Cùng tên và danh sách parameter.
- Kiểu trả về tương thích.
- Không giảm mức truy cập của method.
- Checked exception không được mở rộng tùy ý so với contract.

```java
interface Sender {
    Object send(String message);
}

final class EmailSender implements Sender {
    @Override
    public String send(String message) {
        return "sent"; // covariant return type
    }
}
```

Method `static` được chọn theo class và không polymorphic như instance method.
`private` method không được override vì subclass không nhìn thấy nó.

Nhắc lại:

- **Overload**: cùng tên, khác parameter; compiler chọn method.
- **Override**: cùng contract trong hierarchy; runtime chọn implementation.

---

## 9. Interface, abstract class hay concrete class?

### Interface

Dùng khi cần mô tả capability hoặc contract mà nhiều class khác nhau có thể thực hiện:

```java
interface NotificationSender {
    void send(Notification notification);
}
```

Một class có thể implement nhiều interface.

### Abstract class

Dùng khi các subtype thực sự có quan hệ gần, cần chia sẻ state hoặc implementation:

```java
abstract class BaseGateway {
    protected final String merchantId;

    protected BaseGateway(String merchantId) {
        this.merchantId = merchantId;
    }

    abstract PaymentResult charge(Subscription subscription);
}
```

Một class chỉ extend được một class. Inheritance làm coupling mạnh hơn nên cần cân
nhắc.

### Concrete class

Dùng trực tiếp khi không có nhu cầu thay implementation hoặc abstraction chưa mang
lại giá trị. Không cần interface “cho tương lai” nếu chưa có boundary thực sự.

| Nhu cầu | Lựa chọn thường phù hợp |
|---|---|
| Contract/capability | Interface |
| Chia sẻ state và template behavior | Abstract class |
| Implementation hoàn chỉnh | Concrete class |
| Data/value carrier | Record |
| Tập hằng hữu hạn | Enum |

---

## 10. Composition over inheritance

Inheritance biểu diễn quan hệ “is-a”:

```text
StripeGateway is a PaymentGateway
```

Composition biểu diễn “has-a”:

```text
PaymentService has a PaymentGateway
```

```java
final class PaymentService {
    private final PaymentGateway gateway;

    PaymentService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    PaymentResult pay(Subscription subscription) {
        return gateway.charge(subscription);
    }
}
```

Composition thường linh hoạt hơn:

- Có thể đổi collaborator khi khởi tạo.
- Dễ cung cấp fake/mock trong test.
- Không kế thừa field và behavior ngoài ý muốn.
- Tránh hierarchy sâu, khó hiểu.

Không phải inheritance luôn sai. Hãy dùng khi subtype thật sự thay thế được parent mà
không phá contract — tư tưởng Liskov substitution.

---

## 11. `final` trong code mẫu

Hai implementation là `static final class`:

```java
static final class StripeGateway implements PaymentGateway {
}
```

- `static`: nested class không giữ reference ngầm tới instance `OopDemo`.
- `final`: không class nào extend `StripeGateway`.

`final` có nhiều nghĩa:

| Vị trí | Ý nghĩa |
|---|---|
| Variable | Chỉ được gán một lần |
| Method | Không được override |
| Class | Không được extend |

`final class` không đảm bảo mọi field bên trong immutable. Immutability phụ thuộc vào
cách class quản lý toàn bộ state.

---

## 12. Enum thay magic string

Code mẫu:

```java
enum Plan {
    FREE, PRO, ENTERPRISE
}
```

So với String `"PRO"`, enum có:

- Compiler kiểm tra type.
- Không có lỗi `"pro"`, `"PR0"`.
- IDE autocomplete và refactor an toàn.
- Có thể dùng trong exhaustive switch.
- Có thể chứa field và behavior.

```java
enum Plan {
    FREE(100),
    PRO(5_000),
    ENTERPRISE(100_000);

    private final long quota;

    Plan(long quota) {
        this.quota = quota;
    }

    long quota() {
        return quota;
    }
}
```

Enum constant là object duy nhất của enum đó, không chỉ là số nguyên được đặt tên.
Không lưu ordinal vào database vì thứ tự enum có thể thay đổi; lưu tên hoặc mã ổn
định được định nghĩa rõ.

---

## 13. Record cho value/data carrier

Code mẫu:

```java
record PaymentResult(boolean successful, String message) {
}
```

Compiler tạo:

- Private final field cho mỗi component.
- Canonical constructor.
- Accessor `successful()` và `message()`.
- `equals()`, `hashCode()`, `toString()`.

Record phù hợp cho DTO, command, response hoặc value object nhỏ khi identity không phải
điểm chính.

### 13.1 Compact constructor

```java
record Subscription(String id, Plan plan, BigDecimal amount) {
    Subscription {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "amount cannot be negative"
            );
        }
    }
}
```

Đây là compact canonical constructor. Sau phần validate, Java tự gán parameters vào
fields.

Code hiện tại có thể ném `NullPointerException` nếu `amount == null` và chưa validate
`id` hoặc `plan`. Đây là điểm luyện tập tốt để bổ sung invariant.

### 13.2 Record chỉ shallow immutable

```java
record Team(List<String> members) {
}
```

Không thể gán lại field `members`, nhưng list được truyền vào vẫn có thể mutable. Dùng
defensive copy:

```java
record Team(List<String> members) {
    Team {
        members = List.copyOf(members);
    }
}
```

Record không tự phù hợp với mọi JPA entity: entity thường cần lifecycle, proxy,
identity và mutable state theo cách ORM quản lý.

---

## 14. Equality và hash code

### 14.1 Identity và logical equality

- `==` với reference hỏi: có cùng object không?
- `equals()` hỏi: hai object có bằng nhau theo định nghĩa của class không?

Record sinh value equality từ tất cả component:

```java
PaymentResult first = new PaymentResult(true, "paid");
PaymentResult second = new PaymentResult(true, "paid");

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // true
```

### 14.2 Contract `equals/hashCode`

Nếu `a.equals(b)` là `true` thì `a.hashCode() == b.hashCode()` bắt buộc phải đúng.
Nếu vi phạm, `HashMap` và `HashSet` có thể không tìm thấy object đã thêm.

Equality nên có các tính chất: reflexive, symmetric, transitive, consistent và xử lý
`null` bằng `false`.

### 14.3 Lưu ý `BigDecimal` trong record

```java
new BigDecimal("29.9").equals(new BigDecimal("29.90")) // false
```

Vì `BigDecimal.equals` xét cả scale, hai `Subscription` có amount trên có thể không
bằng nhau theo record equality dù giá trị số học bằng nhau. Hãy normalize scale nếu
domain yêu cầu.

---

## 15. Phân tích từng phần của `OopDemo`

### Danh sách theo abstraction

```java
List<PaymentGateway> gateways = List.of(
        new StripeGateway(),
        new BankTransferGateway()
);
```

List chỉ hứa rằng mỗi phần tử thực hiện contract `PaymentGateway`. Caller không phụ
thuộc class cụ thể.

### Tạo subscription

```java
Subscription subscription = new Subscription(
        "sub-01",
        Plan.PRO,
        new BigDecimal("29.90")
);
```

`Plan.PRO` có type safety. `BigDecimal` giữ decimal chính xác. Compact constructor
kiểm tra amount không âm.

### Gọi polymorphic

```java
PaymentResult result = gateway.charge(subscription);
```

Signature giống nhau; behavior phụ thuộc object runtime.

### Kết quả là value

```java
System.out.println(result.message());
```

Caller dùng accessor của record và không cần biết các field được lưu thế nào.

---

## 16. Liên hệ với SoftAIBox

Một thiết kế thanh toán thực tế có thể là:

```text
SubscriptionController
        │
        ▼
SubscriptionService ─────> PaymentGateway
        │                         ▲
        │                         │
        ▼                  StripePaymentAdapter
SubscriptionRepository
```

- Controller nhận và validate request HTTP.
- Service điều phối use case.
- Domain object bảo vệ invariant.
- Gateway là port/contract.
- Stripe adapter chuyển domain request sang Stripe SDK.
- Repository lưu subscription.

Mock test:

```java
final class FakePaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult charge(Subscription subscription) {
        return new PaymentResult(true, "fake payment");
    }
}
```

Nhờ contract, unit test không gọi mạng, không cần API key và chạy ổn định.

Production còn cần idempotency key, timeout, retry có kiểm soát, webhook verification,
transaction boundary và không log dữ liệu thanh toán nhạy cảm.

---

## 17. Những lỗi thường gặp

### Biến class thành túi getter/setter

State có thể rơi vào tổ hợp không hợp lệ. Dùng behavior mang ý nghĩa nghiệp vụ.

### Tạo interface cho mọi class

Abstraction không có boundary hoặc alternate implementation chỉ làm tăng file và mức
gián tiếp. Tạo khi contract đem lại giá trị rõ.

### Dùng inheritance chỉ để tái sử dụng vài dòng

Subclass bị coupling với state và lifecycle của parent. Cân nhắc composition.

### Quên `@Override`

Sai signature có thể vô tình tạo overload thay vì override.

### Dùng magic string

`"pro"` và `"PRO"` đều compile. Enum chuyển lỗi thành vấn đề được compiler phát hiện.

### Cho rằng record deep immutable

Collection mutable bên trong vẫn sửa được nếu không defensive copy.

### Override `equals` nhưng quên `hashCode`

Hash-based collection sẽ có hành vi sai khó chẩn đoán.

### Bắt caller biết implementation

Business service nhận `StripeGateway` trực tiếp sẽ khó đổi provider và khó test hơn
khi thứ nó thật sự cần chỉ là `PaymentGateway`.

---

## 18. Bài thực hành theo cấp độ

### Cấp 1 — Bổ sung invariant

Sửa trên bản code tự gõ:

- `id` không null/blank.
- `plan` không null.
- `amount` không null và không âm.
- `PaymentResult.message` không blank.

Viết bảng input hợp lệ và không hợp lệ trước khi chạy.

### Cấp 2 — Mock gateway

Tạo `MockPaymentGateway` lưu số lần `charge` được gọi. Kiểm tra:

- Đúng subscription được truyền vào.
- Kết quả success được service xử lý đúng.
- Không có network call.

### Cấp 3 — Notification

Thiết kế:

```java
interface NotificationSender {
    SendResult send(Notification notification);
}
```

Tạo implementation email và dashboard. Business service không dùng chuỗi
`if (type == ...)` để biết chi tiết từng kênh.

### Cấp 4 — Composition

Tạo `PaymentService` nhận `PaymentGateway` qua constructor. Thêm
`PaymentValidator` và `PaymentReceiptFactory` bằng composition, không tạo hierarchy
service sâu.

### Cấp 5 — Vẽ object diagram

Vẽ instance cụ thể:

```text
User ──owns──> Subscription ──selects──> Plan
  │                    │
  └─creates──> Agent   └─charged via──> PaymentGateway
```

Ghi rõ object nào có identity, object nào là value và object nào là service.

---

## 19. Câu hỏi phỏng vấn và đáp án ngắn

### Bốn tính chất OOP thường được nhắc đến là gì?

Encapsulation, abstraction, inheritance và polymorphism. Trong thiết kế thực tế,
composition thường được ưu tiên hơn hierarchy kế thừa sâu.

### Encapsulation có phải chỉ là private field không?

Không. Mục tiêu là bảo vệ invariant và chỉ cho phép thay đổi state qua operation hợp
lệ; private field chỉ là một công cụ.

### Interface khác abstract class thế nào?

Interface mô tả contract/capability và một class implement được nhiều interface.
Abstract class có thể chia sẻ instance state/implementation nhưng class chỉ extend
được một class.

### Polymorphism là gì?

Caller dùng một contract chung, còn runtime chọn implementation theo object thực tế,
ví dụ cùng `charge()` nhưng Stripe và bank transfer xử lý khác nhau.

### Vì sao composition thường tốt hơn inheritance?

Composition giảm coupling, đổi collaborator dễ, test dễ và không kéo theo state hoặc
behavior của parent ngoài ý muốn.

### Enum tốt hơn String thế nào?

Enum giới hạn tập giá trị, có type safety, hỗ trợ exhaustive switch và refactor bằng
compiler/IDE.

### Record có immutable hoàn toàn không?

Không nhất thiết. Reference component không gán lại được, nhưng object mutable mà nó
trỏ tới vẫn có thể đổi. Cần defensive copy.

### Quy tắc quan trọng của `equals/hashCode`?

Hai object bằng nhau theo `equals` bắt buộc có cùng hash code.

### Dependency inversion khác dependency injection thế nào?

Dependency inversion là nguyên tắc policy phụ thuộc abstraction. Dependency injection
là kỹ thuật cung cấp dependency từ bên ngoài, thường qua constructor.

---

## 20. Cách học trong ngày

1. Chạy `OopDemo` và đoán implementation nào được gọi.
2. Đặt breakpoint ở hai method `charge`, quan sát runtime class của `gateway`.
3. Tự gõ lại ví dụ mà không copy-paste.
4. Bổ sung validation vào bản thực hành riêng.
5. Tạo fake gateway và `PaymentService` dùng constructor injection.
6. Vẽ dependency graph và object diagram.
7. Trả lời bộ câu hỏi phỏng vấn thành tiếng.

## Definition of done

Bạn hoàn thành Ngày 3 khi có thể:

- Phân biệt class, object, state, behavior và identity.
- Giải thích invariant và viết constructor bảo vệ object.
- Thiết kế method nghiệp vụ thay cho setter tự do.
- Viết interface và hai implementation.
- Giải thích dynamic dispatch bằng code mẫu.
- Phân biệt overload và override.
- Chọn có lý do giữa interface, abstract class, class, record và enum.
- Giải thích composition over inheritance.
- Dùng defensive copy cho collection trong record.
- Giải thích contract `equals/hashCode`.
- Viết test với fake gateway mà không gọi mạng.
