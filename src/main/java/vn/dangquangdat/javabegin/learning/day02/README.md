# Ngày 2 — Control flow, method, scope và array

Ngày 1 trả lời câu hỏi Java lưu và xử lý dữ liệu như thế nào. Ngày 2 chuyển sang câu
hỏi quan trọng tiếp theo: chương trình **ra quyết định**, **lặp lại công việc** và
**chia một bài toán lớn thành các method nhỏ** như thế nào.

Code thực hành trong cùng thư mục:

- [ControlFlowDemo.java](./ControlFlowDemo.java): phân loại mức sử dụng request,
  tính quota còn lại và duyệt nhiều gói dịch vụ.

## Mục tiêu của ngày 2

Sau khi học xong, bạn cần tự giải thích và tự viết được:

1. Biểu thức boolean và các toán tử so sánh, logic.
2. `if`, `else if`, `else` được kiểm tra theo thứ tự nào.
3. Short-circuit của `&&` và `||` giúp tránh lỗi ra sao.
4. Guard clause là gì và vì sao giúp method dễ đọc.
5. Khi nào dùng `switch` expression thay cho chuỗi `if/else`.
6. Parameter, argument, return value và side effect khác nhau thế nào.
7. Overload khác override ở đâu.
8. Scope và lifetime của local variable.
9. Cách chọn giữa `for`, enhanced `for`, `while` và `do-while`.
10. Array khác `List` thế nào và varargs liên quan gì tới array.
11. Big-O cơ bản của duyệt, tìm tuyến tính và binary search.

---

## 1. Đọc và chạy code mẫu

Chạy trực tiếp bằng PowerShell từ thư mục gốc dự án:

```powershell
java .\src\main\java\vn\dangquangdat\javabegin\learning\day02\ControlFlowDemo.java
```

Hoặc mở file trong VS Code:

- `Ctrl + F5`: chạy không debug.
- `F5`: chạy với debugger.
- Đặt breakpoint trong `classify` và `remainingQuota` để theo dõi tham số.

Với dữ liệu hiện tại, kết quả là:

```text
FREE -> NORMAL, remaining=0
PRO -> HEAVY, remaining=3800
ENTERPRISE -> HEAVY, remaining=90000
```

Luồng thực thi:

```text
main()
  │
  ├─ tạo List<RequestUsage>
  │
  └─ for: lấy từng RequestUsage
       │
       ├─ classify(requestCount)
       ├─ remainingQuota(plan, requestCount)
       └─ printf kết quả
```

Ví dụ với gói `PRO`:

```text
requestCount = 1_200
classify(1_200) = "HEAVY"
quota của PRO = 5_000
remaining = max(0, 5_000 - 1_200) = 3_800
```

---

## 2. Biểu thức boolean

Điều kiện của `if`, `while` và `for` phải tạo ra giá trị `boolean`: chỉ có `true`
hoặc `false`.

### 2.1 Toán tử so sánh

```java
count == 0  // bằng
count != 0  // khác
count < 100 // nhỏ hơn
count <= 100
count > 100
count >= 100
```

Không nhầm `==` với `=`:

```java
int count = 10;       // gán
boolean full = count == 10; // so sánh
```

### 2.2 Toán tử logic

```java
boolean canRun = active && remainingQuota > 0;
boolean needsAttention = failed || remainingQuota == 0;
boolean disabled = !active;
```

| Toán tử | Ý nghĩa | Kết quả đúng khi |
|---|---|---|
| `&&` | AND | Cả hai vế đều đúng |
| `\|\|` | OR | Ít nhất một vế đúng |
| `!` | NOT | Đảo ngược giá trị |

Java không có truthy/falsy như JavaScript:

```java
String plan = "PRO";

// if (plan) {} // không compile

if (plan != null && !plan.isBlank()) {
    System.out.println(plan);
}
```

### 2.3 Short-circuit

Java đánh giá biểu thức từ trái sang phải và có thể bỏ qua vế sau.

Với `&&`, nếu vế trái đã `false` thì toàn biểu thức chắc chắn `false`:

```java
if (plan != null && !plan.isBlank()) {
    // isBlank chỉ chạy khi plan khác null
}
```

Với `||`, nếu vế trái đã `true` thì toàn biểu thức chắc chắn `true`:

```java
if (plan == null || plan.isBlank()) {
    throw new IllegalArgumentException("plan is required");
}
```

Thứ tự sau có thể gây `NullPointerException`:

```java
if (plan.isBlank() || plan == null) {
}
```

Short-circuit không chỉ là tối ưu hiệu năng; nó còn là công cụ bảo vệ an toàn khi
truy cập reference có thể `null`.

---

## 3. `if`, `else if` và `else`

Cấu trúc đầy đủ:

```java
if (conditionA) {
    // chạy nếu A đúng
} else if (conditionB) {
    // chỉ xét B nếu A sai
} else {
    // chạy khi tất cả điều kiện trước đều sai
}
```

Java kiểm tra từ trên xuống và chỉ chạy nhánh đầu tiên thỏa mãn.

Method trong code mẫu dùng nhiều `if` kết hợp `return`:

```java
static String classify(long requestCount) {
    if (requestCount == 0) {
        return "INACTIVE";
    }
    if (requestCount < 1_000) {
        return "NORMAL";
    }
    return "HEAVY";
}
```

Khi gặp `return`, method kết thúc ngay. Vì vậy không cần viết `else` sau nhánh đã
`return`.

Các mốc cần kiểm tra:

| Input | Nhánh | Kết quả |
|---:|---|---|
| `0` | `requestCount == 0` | `INACTIVE` |
| `1` | `requestCount < 1_000` | `NORMAL` |
| `999` | `requestCount < 1_000` | `NORMAL` |
| `1_000` | nhánh cuối | `HEAVY` |
| `10_000` | nhánh cuối | `HEAVY` |

Code hiện tại chưa chặn số âm. `classify(-1)` sẽ trả `NORMAL` dù dữ liệu không hợp
lệ. Đây là nơi guard clause phát huy tác dụng.

---

## 4. Guard clause và invariant đầu vào

Guard clause kiểm tra trường hợp không hợp lệ hoặc trường hợp đặc biệt ở đầu method,
sau đó dừng sớm bằng `throw` hoặc `return`.

```java
static String classify(long requestCount) {
    if (requestCount < 0) {
        throw new IllegalArgumentException(
                "requestCount must not be negative"
        );
    }

    if (requestCount == 0) {
        return "INACTIVE";
    }

    if (requestCount < 1_000) {
        return "NORMAL";
    }

    return "HEAVY";
}
```

Mental model:

```text
Input đi vào
    │
    ├─ không hợp lệ? ── yes ──> dừng và báo lỗi
    │
    └─ hợp lệ
         └─ xử lý business rule chính
```

Guard clause giúp phần còn lại của method được đọc với giả định đầu vào đã hợp lệ,
đồng thời giảm nhiều tầng `if` lồng nhau.

Trong backend SoftAIBox, guard clause có thể kiểm tra:

- ID phải dương.
- Prompt không được rỗng và không vượt độ dài cho phép.
- Số request đã dùng không được âm.
- Conversation phải thuộc user hiện tại.
- Subscription phải đang hoạt động trước khi chạy agent.

Validation ở boundary không thay thế hoàn toàn invariant trong domain. Dữ liệu có thể
đến từ HTTP, job nền, message queue hoặc test; object quan trọng vẫn nên tự bảo vệ
trạng thái hợp lệ.

---

## 5. `switch` statement và `switch` expression

Code mẫu dùng `switch expression`:

```java
long quota = switch (plan) {
    case "FREE" -> 100;
    case "PRO" -> 5_000;
    case "ENTERPRISE" -> 100_000;
    default -> throw new IllegalArgumentException(
            "Unknown plan: " + plan
    );
};
```

Đây là expression vì toàn bộ `switch` tạo ra một giá trị để gán cho `quota`.

### 5.1 Vì sao dạng expression dễ kiểm soát?

- Mỗi nhánh diễn đạt rõ giá trị trả về.
- Không cần biến tạm được gán ở nhiều nơi.
- Dạng mũi tên không bị fall-through ngoài ý muốn.
- Compiler kiểm tra các nhánh phải tạo giá trị hoặc kết thúc bằng `throw`.

Switch statement kiểu cũ có nguy cơ quên `break`:

```java
switch (plan) {
    case "FREE":
        System.out.println("Free");
        break;
    case "PRO":
        System.out.println("Pro");
        break;
    default:
        throw new IllegalArgumentException("Unknown plan");
}
```

### 5.2 Nhánh cần nhiều câu lệnh

Dùng block và `yield` để trả giá trị từ nhánh:

```java
long quota = switch (plan) {
    case "FREE" -> 100;
    case "PRO" -> {
        System.out.println("Applying PRO quota");
        yield 5_000;
    }
    default -> throw new IllegalArgumentException("Unknown plan");
};
```

### 5.3 `default` không phải kiểm tra `null`

Nếu `plan == null`, switch trên String ném `NullPointerException` trước khi vào
`default`. Hãy validate trước:

```java
if (plan == null || plan.isBlank()) {
    throw new IllegalArgumentException("plan is required");
}
```

Ở Ngày 3, `enum Plan` sẽ thay các magic string như `"PRO"`. Khi switch trên enum và
liệt kê đủ mọi constant, compiler có thể giúp phát hiện trường hợp còn thiếu.

---

## 6. Method: chia bài toán thành đơn vị nhỏ

Cấu trúc method:

```java
static long remainingQuota(String plan, long used) {
    // body
    return 0;
}
```

- `static`: thuộc class, có thể gọi mà chưa tạo object.
- `long` trước tên: kiểu kết quả trả về.
- `remainingQuota`: tên method, nên diễn đạt intent.
- `String plan` và `long used`: parameters.
- `return`: trả giá trị và kết thúc method.

Khi gọi:

```java
long remaining = remainingQuota("PRO", 1_200);
```

- `plan`, `used` là **parameter** trong khai báo.
- `"PRO"`, `1_200` là **argument** tại vị trí gọi.
- `remaining` nhận **return value**.

### 6.1 Method `void`

Method chỉ thực hiện hành động và không trả dữ liệu dùng `void`:

```java
static void printUsage(String plan, long used) {
    System.out.printf("%s used %d%n", plan, used);
}
```

### 6.2 Pure function và side effect

`classify` gần với pure function:

- Kết quả chỉ phụ thuộc `requestCount`.
- Không sửa object bên ngoài.
- Không đọc thời gian, file hay mạng.
- Cùng input luôn cho cùng output.

`System.out.println` là side effect vì nó làm thay đổi trạng thái bên ngoài method.
Pure function thường dễ kiểm thử và suy luận hơn, nhưng ứng dụng thực tế vẫn cần side
effect ở boundary.

Một cách tách hợp lý:

```text
đọc input → parse → validate → calculate → format/in kết quả
             pure logic ở giữa       side effect ở rìa
```

### 6.3 Một method nên làm một việc

Tên `remainingQuota` cho biết method tính quota còn lại. Nó không nên đồng thời ghi
file, gọi API và gửi email.

Dấu hiệu method cần tách:

- Tên phải dùng từ “và”.
- Có nhiều mức indent.
- Có nhiều lý do để thay đổi.
- Khó đặt tên ngắn và chính xác.
- Test một nhánh buộc thiết lập quá nhiều dữ liệu không liên quan.

---

## 7. Overload và override

### 7.1 Overload

Overload là nhiều method cùng tên nhưng khác danh sách parameter:

```java
static long remainingQuota(String plan) {
    return remainingQuota(plan, 0);
}

static long remainingQuota(String plan, long used) {
    // tính quota
    return 0;
}
```

Java phân biệt overload dựa vào:

- Số lượng parameter.
- Kiểu parameter.
- Thứ tự kiểu parameter.

Không thể overload chỉ bằng kiểu trả về:

```java
// Không hợp lệ:
// static long find(String id) { ... }
// static String find(String id) { ... }
```

Caller không thể chọn method chỉ dựa vào kiểu biến nhận kết quả một cách không mơ hồ.

### 7.2 Override

Override là class con hoặc class implementation cung cấp cách thực hiện mới cho method
đã được khai báo bởi parent class hoặc interface:

```java
@Override
public String toString() {
    return "custom";
}
```

| Overload | Override |
|---|---|
| Thường trong cùng class | Có quan hệ kế thừa/interface |
| Khác danh sách parameter | Cùng signature tương thích |
| Chọn ở compile time | Dispatch implementation ở runtime |
| Không bắt buộc `@Override` | Nên luôn dùng `@Override` |

Override và polymorphism được học kỹ ở Ngày 3.

---

## 8. Scope và lifetime của biến

Scope là vùng source code nơi một tên biến có thể được truy cập.

```java
if ("FREE".equals(plan)) {
    long quota = 100;
    System.out.println(quota); // hợp lệ
}

// System.out.println(quota); // compile error
```

Biến `quota` chỉ tồn tại trong block `{}` đã khai báo nó.

Trong code mẫu:

```java
for (RequestUsage usage : usages) {
    String state = classify(usage.requestCount());
    long remaining = remainingQuota(
            usage.plan(),
            usage.requestCount()
    );
}
```

`usage`, `state` và `remaining` chỉ có scope trong mỗi vòng lặp.

### 8.1 Local variable phải được gán chắc chắn

```java
long quota;

// System.out.println(quota); // compile error
```

Compiler dùng definite assignment analysis để chắc chắn local variable đã có giá trị
trước khi đọc.

```java
long quota;
if ("FREE".equals(plan)) {
    quota = 100;
} else {
    quota = 5_000;
}
System.out.println(quota); // mọi nhánh đều gán
```

Field có default value, nhưng local variable thì không. Không nên mở rộng scope chỉ để
“tiện dùng”; scope nhỏ giúp giảm trạng thái phải ghi nhớ.

---

## 9. Các loại vòng lặp

### 9.1 Enhanced `for`

Code mẫu dùng:

```java
for (RequestUsage usage : usages) {
    System.out.println(usage);
}
```

Dùng khi cần lần lượt đọc từng phần tử và không cần index.

### 9.2 `for` theo index

```java
int[] values = {10, 20, 30};

for (int index = 0; index < values.length; index++) {
    System.out.printf("%d: %d%n", index, values[index]);
}
```

Dùng khi cần index, cần đi lùi hoặc cần xét cặp phần tử theo vị trí.

### 9.3 `while`

```java
int attempts = 0;
while (attempts < 3) {
    attempts++;
}
```

Phù hợp khi chưa biết chính xác số lần lặp và tiếp tục dựa trên điều kiện.

### 9.4 `do-while`

```java
int attempts = 10;
do {
    System.out.println(attempts);
} while (attempts < 3);
```

Body luôn chạy ít nhất một lần vì điều kiện được kiểm tra ở cuối.

### 9.5 `break` và `continue`

```java
for (int number = 1; number <= 10; number++) {
    if (number == 3) {
        continue; // bỏ phần còn lại của vòng hiện tại
    }
    if (number == 8) {
        break;    // kết thúc cả vòng lặp
    }
    System.out.println(number);
}
```

Không lạm dụng `break`/`continue` trong logic quá dài vì luồng nhảy khó theo dõi.
Trong method nhỏ, chúng có thể làm intent rõ hơn.

---

## 10. Array

Array chứa nhiều phần tử cùng kiểu và có kích thước cố định:

```java
int[] usages = {100, 1_200, 10_000};
```

Index bắt đầu từ `0`:

```java
usages[0]; // 100
usages[1]; // 1_200
usages[2]; // 10_000
```

Số phần tử dùng field `length`, không phải method:

```java
int size = usages.length;
```

Truy cập `usages[3]` ném `ArrayIndexOutOfBoundsException`.

### 10.1 Tạo array với kích thước trước

```java
long[] quotas = new long[3];
quotas[0] = 100;
quotas[1] = 5_000;
quotas[2] = 100_000;
```

Các phần tử ban đầu có default value: số là `0`, boolean là `false`, reference là
`null`.

### 10.2 Array và `List`

| Array | `List` |
|---|---|
| Kích thước cố định | Kích thước có thể linh hoạt |
| Hỗ trợ primitive như `int[]` | Generic dùng wrapper như `List<Integer>` |
| Truy cập kích thước bằng `length` | Dùng `size()` |
| Cú pháp mức thấp, quan trọng cho thuật toán | Phổ biến trong business code |
| Covariant, có thể gây lỗi runtime | Generic kiểm tra kiểu chặt hơn |

`List.of(...)` trong code mẫu tạo list không cho `add`, `remove` hoặc `set`:

```java
List<String> plans = List.of("FREE", "PRO");
// plans.add("ENTERPRISE"); // UnsupportedOperationException
```

“Không sửa được list” không đồng nghĩa các object bên trong đều immutable.

---

## 11. Varargs thực chất là array

Varargs cho phép caller truyền số argument linh hoạt:

```java
static void printTags(String... tags) {
    for (String tag : tags) {
        System.out.println(tag);
    }
}

printTags("java", "backend", "spring");
```

Bên trong method, `tags` có kiểu `String[]`. Có thể gọi bằng array có sẵn:

```java
String[] values = {"java", "backend"};
printTags(values);
```

Varargs phải là parameter cuối cùng:

```java
static void log(String prefix, String... messages) {
}
```

Mỗi lời gọi varargs thường tạo array, nên không lạm dụng trong hot path chỉ vì cú pháp
ngắn.

---

## 12. `Math.max` và ý nghĩa nghiệp vụ

Code mẫu trả:

```java
return Math.max(0, quota - used);
```

Nếu quota là `100` và đã dùng `120`:

```text
quota - used = -20
max(0, -20) = 0
```

Kết quả “quota còn lại” không âm. Tuy nhiên nó làm mất thông tin đã vượt bao nhiêu.
Nếu cần tính phí over-quota, tách công thức:

```java
static long overQuota(long quota, long used) {
    return Math.max(0, used - quota);
}
```

Một kết quả `remaining = 0` có thể nghĩa là “vừa dùng hết” hoặc “đã vượt rất nhiều”.
Tên và dữ liệu trả về phải phù hợp câu hỏi nghiệp vụ.

---

## 13. Big-O nhập môn

Big-O mô tả tốc độ tăng của lượng công việc khi kích thước input `n` tăng. Nó không
phải số mili-giây chính xác.

| Thao tác | Độ phức tạp |
|---|---|
| Đọc `array[index]` | `O(1)` |
| Duyệt toàn bộ array | `O(n)` |
| Linear search | `O(n)` |
| Hai vòng lặp lồng nhau cùng theo `n` | thường `O(n²)` |
| Binary search trên dữ liệu đã sort | `O(log n)` |

Linear search:

```java
static int indexOf(int[] values, int target) {
    for (int index = 0; index < values.length; index++) {
        if (values[index] == target) {
            return index;
        }
    }
    return -1;
}
```

Binary search liên tục bỏ đi một nửa khoảng tìm kiếm, nhưng chỉ đúng khi array đã được
sắp xếp.

---

## 14. Liên hệ với SoftAIBox

Các khái niệm Ngày 2 xuất hiện trong hầu hết use case:

```text
HTTP request
    │
    ├─ validate input bằng guard clause
    ├─ switch theo plan/role/status
    ├─ gọi các method tính quota, giá, quyền
    ├─ duyệt agent, conversation hoặc data source
    └─ trả DTO hoặc ném domain exception
```

Ví dụ phân quyền sơ bộ:

```java
static boolean canCreateAgent(String role, long currentAgents) {
    if (role == null) {
        return false;
    }

    long limit = switch (role) {
        case "ADMIN" -> Long.MAX_VALUE;
        case "USER" -> 5;
        default -> 0;
    };

    return currentAgents < limit;
}
```

Trong code production, role và plan nên là enum, authorization nên nằm ở boundary phù
hợp, và quota có thể cần đọc từ database thay vì hard-code.

---

## 15. Những lỗi thường gặp

### Điều kiện biên sai một đơn vị

```java
if (requestCount <= 1_000) { // 1_000 bị xếp NORMAL
}
```

Hãy viết bảng boundary cho `999`, `1_000`, `1_001` trước khi code.

### Quên validate số âm

`classify(-1)` không tự động vô nghĩa đối với compiler. Business rule phải nói rõ.

### So sánh String bằng `==`

```java
if ("PRO".equals(plan)) {
}
```

### Switch không xử lý `null`

`default` xử lý giá trị không khớp case, không bảo vệ reference `null`.

### Vòng lặp không tiến triển

```java
int index = 0;
while (index < 10) {
    // quên index++ -> vòng lặp vô hạn
}
```

### Sai điều kiện index

```java
for (int i = 0; i <= values.length; i++) {
    // lần cuối i == length, truy cập values[i] sẽ lỗi
}
```

Điều kiện thông thường là `i < values.length`.

### Nghĩ `List.of` là mutable

`add` và `remove` sẽ ném `UnsupportedOperationException` tại runtime.

### Nhồi mọi logic vào `main`

`main` nên điều phối ví dụ. Logic tính toán nên ở method có tên rõ và dễ test.

---

## 16. Bài thực hành theo cấp độ

### Cấp 1 — Nắm cú pháp

1. In các số từ 1 đến 100.
2. Với số chia hết cho 3 in `Fizz`, chia hết cho 5 in `Buzz`, chia hết cả hai in
   `FizzBuzz`.
3. Viết method trả `INACTIVE`, `NORMAL`, `HEAVY` và chặn số âm.
4. Tìm `min`, `max`, `sum` trong `int[]` mà không dùng Stream.

### Cấp 2 — Tách method

Viết các method:

```java
static long quotaOf(String plan)
static long remainingQuota(String plan, long used)
static long overQuota(String plan, long used)
static long sum(long... values)
```

Yêu cầu:

- Chặn `null`, blank và `used < 0`.
- Không lặp lại bảng quota ở nhiều method.
- Viết bảng test bằng tay cho mọi boundary.

### Cấp 3 — Tiền over-quota

Dùng `BigDecimal` tính phí:

```text
FREE:       0.05 USD/request vượt
PRO:        0.02 USD/request vượt
ENTERPRISE: 0.01 USD/request vượt
```

Không dùng `double`. Kết quả làm tròn hai chữ số theo rule được chọn rõ ràng.

### Cấp 4 — Thuật toán

1. Viết linear search trả index hoặc `-1`.
2. Viết binary search trên `int[]` đã sort.
3. Giải thích invariant của khoảng tìm kiếm sau mỗi vòng lặp.
4. So sánh số lần kiểm tra khi array có 1.000.000 phần tử.

### Cấp 5 — Refactor

Tạo một luồng:

```text
raw plan + raw usage
    → parse
    → validate
    → calculate
    → format
```

Mỗi bước là một method nhỏ. Chỉ bước cuối được in console.

---

## 17. Câu hỏi phỏng vấn và đáp án ngắn

### `&&` và `||` có luôn đánh giá cả hai vế không?

Không. Chúng short-circuit từ trái sang phải. `&&` bỏ vế sau khi vế trước sai;
`||` bỏ vế sau khi vế trước đúng.

### Guard clause là gì?

Là kiểm tra điều kiện lỗi hoặc trường hợp đặc biệt ở đầu method rồi dừng sớm, giúp
business path chính ít lồng nhau hơn.

### Switch expression khác switch statement thế nào?

Switch expression tạo ra giá trị, có thể dùng mũi tên và `yield`; compiler yêu cầu
các nhánh hợp lệ cho việc tạo kết quả. Statement chủ yếu điều khiển luồng.

### Parameter khác argument thế nào?

Parameter là biến trong khai báo method. Argument là giá trị hoặc biểu thức truyền vào
khi gọi method.

### Overload khác override thế nào?

Overload là cùng tên, khác danh sách parameter và được chọn lúc compile. Override là
implementation lại contract kế thừa và được dispatch theo object runtime.

### Local variable có default value không?

Không. Compiler yêu cầu local variable được gán chắc chắn trước khi đọc. Field mới có
default value.

### Array khác List thế nào?

Array có kích thước cố định và hỗ trợ primitive trực tiếp. List là abstraction
collection linh hoạt, dùng generic và phổ biến hơn trong business code.

### Varargs là gì?

`T... values` là cú pháp cho phép truyền số argument linh hoạt; bên trong method,
`values` là `T[]`.

### Binary search có điều kiện gì?

Dữ liệu phải được sắp xếp theo cùng quy tắc so sánh. Mỗi bước bỏ một nửa không gian
tìm kiếm nên có `O(log n)`.

---

## 18. Cách học trong ngày

1. Chạy code mẫu và dự đoán output trước.
2. Đặt breakpoint trong hai method, quan sát parameter và call stack.
3. Thử `0`, `999`, `1_000`, `-1` cho `classify`.
4. Thử `"FREE"`, `"free"`, `"VIP"` và `null` cho quota.
5. Gõ lại FizzBuzz, linear search và binary search.
6. Refactor bài quota thành các method nhỏ.
7. Trả lời câu hỏi phỏng vấn không nhìn đáp án.

## Definition of done

Bạn hoàn thành Ngày 2 khi có thể:

- Viết và giải thích `if/else`, `switch`, `for` và `while`.
- Giải thích short-circuit bằng ví dụ tránh `NullPointerException`.
- Dùng guard clause để bảo vệ đầu vào.
- Tách parse, validate, calculate và output thành method rõ ràng.
- Phân biệt parameter, argument, return value và side effect.
- Phân biệt overload với override.
- Giải thích scope và definite assignment của local variable.
- Duyệt array, tìm min/max/sum và không bị lỗi index.
- Giải thích varargs thực chất là array.
- Nêu Big-O của truy cập array, linear search và binary search.
- Hoàn thành bài phí over-quota mà không dùng `double`.
