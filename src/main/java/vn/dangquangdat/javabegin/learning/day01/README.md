# Ngày 1 — Java chạy như thế nào, kiểu dữ liệu và bộ nhớ

Tài liệu này giải thích kỹ những kiến thức nền tảng cần nắm trước khi học OOP,
Collections, Spring Boot và backend Java. Hãy đọc từng phần, tự gõ lại ví dụ và
đoán kết quả trước khi chạy.

Code thực hành trong cùng thư mục:

- [BasicsDemo.java](./BasicsDemo.java): ví dụ về primitive, reference, `String`,
  `BigDecimal` và `record`.
- [Day1Exercise.java](./Day1Exercise.java): bài tập tính giá theo tháng, giá theo năm
  và phần trăm giảm giá.

## Mục tiêu của ngày 1

Sau khi học xong, bạn cần tự giải thích được:

1. JDK, JRE và JVM khác nhau thế nào.
2. Source Java biến thành chương trình đang chạy như thế nào.
3. Primitive khác reference type ở đâu.
4. `==` khác `equals()` thế nào.
5. Vì sao `String` immutable.
6. Vì sao không dùng `double` cho tiền.
7. Java pass-by-value nghĩa là gì.
8. `final` variable có đồng nghĩa object immutable không.
9. `int` khác `Integer` thế nào.
10. Stack và heap dùng để hình dung điều gì.

---

## 1. JDK, JRE và JVM khác nhau thế nào?

### 1.1 JVM — Java Virtual Machine

JVM là máy ảo chịu trách nhiệm thực thi Java bytecode. Source code Java không được
CPU chạy trực tiếp. Source được biên dịch thành bytecode, sau đó JVM đọc và thực thi
bytecode đó.

JVM thực hiện nhiều công việc:

- Nạp các file `.class`.
- Kiểm tra bytecode có hợp lệ và an toàn hay không.
- Quản lý vùng nhớ khi chương trình chạy.
- Thực thi bytecode bằng interpreter và JIT compiler.
- Thu hồi object không còn được sử dụng bằng Garbage Collector.
- Cung cấp môi trường chạy tương đối thống nhất trên Windows, Linux và macOS.

Mỗi hệ điều hành có một JVM được xây dựng riêng cho hệ điều hành đó, nhưng JVM đều
hiểu cùng một định dạng bytecode. Đây là ý nghĩa của câu:

> Write once, run anywhere.

Không phải một file thực thi native duy nhất chạy trực tiếp trên mọi hệ điều hành.
Điều đúng hơn là cùng bytecode có thể được chạy bởi JVM phù hợp với từng hệ điều hành.

### 1.2 JRE — Java Runtime Environment

JRE là môi trường cần thiết để **chạy** chương trình Java. Về mặt khái niệm, JRE bao
gồm:

- JVM.
- Các thư viện chuẩn cần trong lúc chạy.
- Những thành phần runtime hỗ trợ JVM.

JRE không tập trung vào công cụ phát triển như compiler hay debugger. Trong các bản
Java hiện đại, nhà phát triển thường cài JDK và không cần cài một gói JRE riêng. Tuy
vậy, khái niệm JRE vẫn quan trọng để phân biệt môi trường chạy với bộ công cụ phát
triển.

### 1.3 JDK — Java Development Kit

JDK là bộ công cụ dùng để **phát triển** ứng dụng Java. JDK chứa runtime và các công
cụ như:

| Công cụ | Vai trò |
|---|---|
| `javac` | Biên dịch source `.java` thành bytecode `.class` |
| `java` | Khởi động JVM và chạy chương trình |
| `javadoc` | Sinh tài liệu từ Java source |
| `jar` | Đóng gói class và resource thành file JAR |
| `jdb` | Debugger dòng lệnh của Java |

Mối quan hệ dễ nhớ:

```text
JDK = công cụ phát triển + môi trường chạy
JRE = JVM + thư viện/runtime để chạy
JVM = máy ảo thực thi bytecode
```

### Câu trả lời phỏng vấn ngắn

> JDK dùng để phát triển và biên dịch Java. JRE là môi trường cần để chạy ứng dụng.
> JVM là thành phần thực thi bytecode, quản lý bộ nhớ và garbage collection.

---

## 2. Source Java biến thành chương trình đang chạy như thế nào?

Xét một file:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello Java");
    }
}
```

Quá trình chạy có thể hình dung như sau:

```text
Hello.java
    │
    │ javac Hello.java
    ▼
Hello.class — Java bytecode
    │
    │ java Hello
    ▼
Class Loader → Bytecode Verifier → JVM → kết quả
                                  │
                                  ├─ Interpreter
                                  ├─ JIT Compiler
                                  ├─ Memory management
                                  └─ Garbage Collector
```

### Bước 1: Viết source code

Source nằm trong file có đuôi `.java`. Nếu có một `public class Hello`, tên file phải
là `Hello.java`.

### Bước 2: Compiler kiểm tra và tạo bytecode

```powershell
javac Hello.java
```

Compiler kiểm tra cú pháp và kiểu dữ liệu. Nếu hợp lệ, compiler tạo `Hello.class`.
File `.class` chứa bytecode, không phải source code và cũng chưa phải mã máy dành riêng
cho CPU Windows hay Linux.

Java là statically typed, vì vậy nhiều lỗi được phát hiện trước khi chương trình chạy:

```java
int age = "twenty"; // compile error
```

### Bước 3: Class Loader nạp class

Khi chạy:

```powershell
java Hello
```

JVM tìm `Hello.class` trên classpath và Class Loader nạp class vào JVM. Với class có
package, cần dùng fully qualified class name:

```powershell
java -cp target\classes vn.dangquangdat.javabegin.learning.day01.BasicsDemo
```

Trong đó:

- `-cp target\classes` đặt classpath.
- Phần còn lại là `package + class name`.

### Bước 4: JVM kiểm tra bytecode

Bytecode Verifier kiểm tra bytecode có vi phạm các quy tắc của JVM hay không, ví dụ
thao tác sai kiểu hoặc truy cập vùng nhớ không hợp lệ.

### Bước 5: JVM thực thi

JVM có thể bắt đầu bằng cách diễn giải bytecode. Những đoạn code chạy nhiều có thể được
JIT — Just-In-Time compiler — chuyển thành mã máy tối ưu cho CPU hiện tại.

Vì vậy Java vừa có bước compile trước khi chạy, vừa có tối ưu runtime.

### Entry point `main`

Ứng dụng Java cơ bản bắt đầu tại:

```java
public static void main(String[] args) {
}
```

- `public`: JVM có thể gọi method.
- `static`: không cần tạo object của class trước khi gọi.
- `void`: method không trả về giá trị.
- `String[] args`: các argument nhận từ command line.

---

## 3. Primitive khác reference type ở đâu?

Java có hai nhóm kiểu lớn: primitive type và reference type.

### 3.1 Tám primitive type

| Primitive | Dùng cho | Ví dụ |
|---|---|---|
| `byte` | Số nguyên rất nhỏ, dữ liệu nhị phân | `byte level = 10;` |
| `short` | Số nguyên nhỏ | `short year = 2026;` |
| `int` | Số nguyên thông dụng | `int count = 100;` |
| `long` | Số nguyên lớn | `long id = 9_000_000_001L;` |
| `float` | Số thực độ chính xác thấp | `float rate = 1.5F;` |
| `double` | Số thực gần đúng thông dụng | `double rate = 98.5;` |
| `char` | Một UTF-16 code unit | `char grade = 'A';` |
| `boolean` | Giá trị logic | `boolean active = true;` |

Primitive variable trực tiếp biểu diễn một giá trị primitive:

```java
int first = 10;
int second = first;
second = 20;

System.out.println(first);  // 10
System.out.println(second); // 20
```

Khi gán `second = first`, giá trị `10` được copy. Thay đổi `second` không ảnh hưởng
`first`.

### 3.2 Reference type

Class, record, enum, interface và array đều liên quan tới reference type.

```java
String name = "Dat";
BigDecimal price = new BigDecimal("19.99");
int[] numbers = {1, 2, 3};
UserSummary user = new UserSummary(1L, "dat@example.com", true);
```

Biến reference không chứa toàn bộ object. Nó chứa một giá trị tham chiếu tới object.

```text
Biến reference                    Object

user ───────────────────────────> UserSummary(...)
price ──────────────────────────> BigDecimal("19.99")
```

Một reference có thể là `null`:

```java
String name = null;
System.out.println(name.length()); // NullPointerException
```

Primitive không thể là `null`:

```java
int count = null; // compile error
```

### 3.3 Local variable và field

Local variable phải được gán trước khi đọc:

```java
void printCount() {
    int count;
    // System.out.println(count); // compile error
}
```

Field của object có default value nếu chưa được gán rõ ràng:

```java
class Example {
    int count;        // 0
    boolean active;   // false
    String name;      // null
}
```

Không nên dựa quá nhiều vào default value trong business code. Constructor rõ ràng
giúp object được tạo ra với trạng thái hợp lệ.

---

## 4. `==` khác `equals()` thế nào?

### 4.1 Với primitive

`==` so sánh giá trị primitive:

```java
int a = 10;
int b = 10;

System.out.println(a == b); // true
```

### 4.2 Với reference

`==` kiểm tra hai reference có trỏ tới **cùng một object** hay không.

```java
String first = new String("Java");
String second = new String("Java");

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // true
```

Hai object khác nhau nhưng có cùng nội dung:

```text
first  ─────> String object "Java"
second ─────> String object "Java"
```

- `first == second` là `false` vì identity khác nhau.
- `first.equals(second)` là `true` vì nội dung bằng nhau.

### 4.3 Vì sao đôi khi so sánh String bằng `==` vẫn trả về `true`?

```java
String first = "Java";
String second = "Java";

System.out.println(first == second); // có thể là true do String pool
```

JVM có thể tái sử dụng cùng String literal trong String pool. Đây là tối ưu implementation,
không phải lý do để dùng `==` so sánh nội dung. Code đúng vẫn là:

```java
first.equals(second);
```

Nếu biến có thể `null`, dùng:

```java
Objects.equals(first, second);
```

`Objects.equals` xử lý an toàn trường hợp một hoặc cả hai reference là `null`.

### 4.4 Contract của `equals()` và `hashCode()`

Khi hai object bằng nhau theo `equals()`, chúng bắt buộc phải có cùng `hashCode()`.
Điều này rất quan trọng khi dùng `HashMap` và `HashSet`.

Record tự sinh `equals()` và `hashCode()` dựa trên các component:

```java
record User(long id, String email) {}
```

Hai `User` có cùng `id` và `email` sẽ bằng nhau theo `equals()`.

### 4.5 Trường hợp đặc biệt của `BigDecimal`

```java
BigDecimal a = new BigDecimal("10.0");
BigDecimal b = new BigDecimal("10.00");

System.out.println(a.equals(b));    // false: khác scale
System.out.println(a.compareTo(b)); // 0: cùng giá trị số học
```

Với kiểm tra lớn hơn, nhỏ hơn hoặc bằng nhau về giá trị số học, thường dùng
`compareTo()`.

---

## 5. Vì sao `String` immutable?

Immutable nghĩa là trạng thái của object không thể thay đổi sau khi object được tạo.

```java
String modelName = "gpt-model";
String displayName = modelName.toUpperCase();

System.out.println(modelName);   // gpt-model
System.out.println(displayName); // GPT-MODEL
```

`toUpperCase()` không sửa String ban đầu. Nó tạo hoặc trả về một String khác.

### 5.1 Lợi ích của String immutable

#### An toàn khi chia sẻ

Nhiều phần code có thể giữ reference tới cùng một String mà không lo một nơi bất ngờ
thay đổi nội dung String.

#### Phù hợp làm key của HashMap

`HashMap` dựa vào `hashCode()`. Nếu nội dung key thay đổi sau khi được đưa vào map,
map có thể không tìm lại được key. String immutable nên hash code của nội dung không
bị thay đổi bất ngờ.

#### Hỗ trợ String pool

Vì String không thể bị sửa, JVM có thể tái sử dụng String literal an toàn.

#### Dễ dùng trong môi trường nhiều thread

Object immutable không có trạng thái bị sửa đồng thời, nhờ đó giảm nhiều vấn đề race
condition. Điều này không có nghĩa mọi phép kết hợp nhiều String đều tự động tối ưu.

### 5.2 Nối String trong vòng lặp

Đoạn sau có thể tạo nhiều String trung gian:

```java
String result = "";
for (int i = 0; i < 1_000; i++) {
    result = result + i;
}
```

Khi cần xây dựng chuỗi qua nhiều lần thay đổi, dùng `StringBuilder`:

```java
StringBuilder builder = new StringBuilder();
for (int i = 0; i < 1_000; i++) {
    builder.append(i);
}

String result = builder.toString();
```

---

## 6. Vì sao không dùng `double` cho tiền?

`double` dùng floating-point nhị phân. Nhiều số thập phân quen thuộc không thể được
biểu diễn chính xác bằng một số lượng hữu hạn bit nhị phân.

```java
double result = 0.1 + 0.2;
System.out.println(result); // thường là 0.30000000000000004
```

Sai số nhỏ có thể tích lũy qua nhiều phép tính. Với giá tiền, thuế, giảm giá hoặc số
dư tài khoản, sai số đó không được chấp nhận.

### 6.1 Tạo `BigDecimal` đúng cách

Nên tạo từ String:

```java
BigDecimal price = new BigDecimal("19.99");
```

Không nên tạo trực tiếp từ literal `double`:

```java
BigDecimal price = new BigDecimal(19.99); // mang theo sai số của double
```

Khi đầu vào đã là `double`, `BigDecimal.valueOf(double)` thường dễ dự đoán hơn constructor
nhận `double`, nhưng với tiền cố định trong code, String vẫn là cách rõ ràng nhất.

### 6.2 `BigDecimal` là immutable

Các phép tính trả về object mới:

```java
BigDecimal price = new BigDecimal("10.00");
price.add(new BigDecimal("5.00"));

System.out.println(price); // vẫn là 10.00
```

Phải nhận kết quả:

```java
price = price.add(new BigDecimal("5.00"));
System.out.println(price); // 15.00
```

### 6.3 Các phép tính thông dụng

```java
BigDecimal a = new BigDecimal("10.00");
BigDecimal b = new BigDecimal("3.00");

a.add(b);       // cộng
a.subtract(b);  // trừ
a.multiply(b);  // nhân
a.divide(b, 2, RoundingMode.HALF_UP); // chia và làm tròn
```

Phép chia có thể tạo số thập phân vô hạn như `10 / 3`, vì vậy phải xác định scale và
rounding mode khi kết quả không thể biểu diễn chính xác.

### 6.4 Scale và làm tròn

```java
BigDecimal amount = new BigDecimal("215.895");
BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);

System.out.println(rounded); // 215.90
```

- `2` là hai chữ số sau dấu thập phân.
- `HALF_UP` làm tròn lên khi chữ số bị bỏ bắt đầu từ `5`.

Quy tắc làm tròn trong dự án thật phải theo nghiệp vụ và loại tiền, không nên mặc định
mọi hệ thống tài chính đều dùng cùng một quy tắc.

### 6.5 So sánh `BigDecimal`

Không dùng `<` hoặc `>` với object. Dùng `compareTo()`:

```java
int comparison = price.compareTo(BigDecimal.ZERO);

comparison < 0;  // price nhỏ hơn 0
comparison == 0; // price bằng 0
comparison > 0;  // price lớn hơn 0
```

Ví dụ chặn giá âm nhưng cho phép gói miễn phí có giá bằng 0:

```java
if (monthlyPrice.compareTo(BigDecimal.ZERO) < 0) {
    throw new IllegalArgumentException("monthlyPrice must not be negative");
}
```

---

## 7. Java pass-by-value nghĩa là gì?

Java **luôn truyền tham số bằng giá trị**. Điều dễ gây nhầm là giá trị của biến reference
chính là một reference.

### 7.1 Truyền primitive

```java
static void changeNumber(int value) {
    value = 100;
}

public static void main(String[] args) {
    int number = 10;
    changeNumber(number);
    System.out.println(number); // 10
}
```

Giá trị `10` được copy vào parameter `value`. Thay đổi bản copy không thay đổi biến
`number` của caller.

### 7.2 Truyền reference

```java
static class Counter {
    int value;
}

static void increment(Counter counter) {
    counter.value++;
}

public static void main(String[] args) {
    Counter original = new Counter();
    increment(original);
    System.out.println(original.value); // 1
}
```

Reference được copy, nhưng cả hai reference cùng trỏ tới một object:

```text
original ─────────────┐
                     ├────> Counter object
counter parameter ───┘
```

Method có thể sửa trạng thái của object đó.

### 7.3 Gán parameter sang object khác không thay đổi caller

```java
static void replace(Counter counter) {
    counter = new Counter();
    counter.value = 999;
}

public static void main(String[] args) {
    Counter original = new Counter();
    original.value = 10;

    replace(original);
    System.out.println(original.value); // 10
}
```

`replace` chỉ đổi bản copy của reference. Biến `original` của caller vẫn trỏ tới object
cũ.

### Câu trả lời phỏng vấn ngắn

> Java luôn pass-by-value. Với primitive, giá trị primitive được copy. Với object,
> giá trị reference được copy, nên method có thể sửa object được cùng trỏ tới nhưng
> không thể gán lại biến reference của caller.

---

## 8. `final` variable có đồng nghĩa object immutable không?

Không. `final` trên một biến có nghĩa biến đó chỉ được gán một lần.

### 8.1 `final` primitive

```java
final int maxSize = 100;
// maxSize = 200; // compile error
```

### 8.2 `final` reference

```java
final List<String> names = new ArrayList<>();
names.add("Dat"); // hợp lệ: object vẫn mutable

// names = new ArrayList<>(); // compile error: không được gán reference khác
```

`final` khóa reference, không tự động khóa trạng thái object.

```text
final names ─────────> ArrayList object
      │                       │
      │ không được đổi        └─ vẫn có thể add/remove
      │ sang object khác
```

### 8.3 Immutable object

Một object immutable thường có các đặc điểm:

- Trạng thái được thiết lập khi khởi tạo.
- Không có method làm thay đổi trạng thái nội bộ.
- Không để lộ collection mutable nội bộ.
- Nếu chứa object mutable, phải defensive copy hoặc quản lý chúng cẩn thận.

Record hỗ trợ tạo data carrier bất biến ở mức component reference, nhưng là **shallow
immutability**:

```java
record Team(List<String> members) {}
```

Reference `members` của record không được gán lại, nhưng caller vẫn có thể sửa List nếu
record giữ trực tiếp List mutable đó. Có thể bảo vệ bằng:

```java
record Team(List<String> members) {
    Team {
        members = List.copyOf(members);
    }
}
```

### 8.4 Các nghĩa khác của `final`

- `final` variable: chỉ được gán một lần.
- `final` method: subclass không được override.
- `final` class: không được kế thừa.

---

## 9. `int` khác `Integer` thế nào?

`int` là primitive. `Integer` là wrapper class đại diện cho một giá trị `int` dưới
dạng object.

| `int` | `Integer` |
|---|---|
| Primitive | Reference type |
| Không thể là `null` | Có thể là `null` |
| Thường ít overhead hơn | Có object/reference và utility methods |
| Dùng trực tiếp cho phép toán | Cần boxing/unboxing khi chuyển qua lại |
| Không dùng được làm generic type | Dùng được như `List<Integer>` |

### 9.1 Autoboxing và unboxing

Java tự động chuyển đổi trong nhiều trường hợp:

```java
Integer boxed = 10; // autoboxing: int → Integer
int value = boxed;  // unboxing: Integer → int
```

### 9.2 Nguy cơ NullPointerException khi unboxing

```java
Integer boxed = null;
int value = boxed; // NullPointerException
```

Compiler cho phép vì kiểu có thể unbox, nhưng runtime thất bại vì không có giá trị
`int` bên trong `null`.

### 9.3 Generic không nhận primitive

Không thể viết:

```java
// List<int> numbers; // compile error
```

Phải dùng wrapper:

```java
List<Integer> numbers = List.of(1, 2, 3);
```

### 9.4 Không dùng `==` để so sánh `Integer` object

```java
Integer a = 1_000;
Integer b = 1_000;

System.out.println(a == b);      // thường false
System.out.println(a.equals(b)); // true
```

Một số Integer nhỏ có thể được JVM cache, làm cho `==` đôi khi trả về `true`. Đừng phụ
thuộc vào cache này. Dùng `equals()` để so sánh nội dung object hoặc unbox có chủ đích
khi chắc chắn không `null`.

---

## 10. Stack và heap dùng để hình dung điều gì?

Stack và heap là mô hình tư duy giúp hiểu lifecycle của method, local variable, object
và Garbage Collector. JVM thực tế có thể tối ưu phức tạp hơn mô hình đơn giản này.

### 10.1 Stack

Mỗi thread có call stack riêng. Mỗi lần gọi method, JVM tạo một stack frame chứa những
thông tin như:

- Local variables.
- Parameters.
- Dữ liệu trung gian cần cho method.
- Vị trí cần quay lại sau khi method kết thúc.

Ví dụ:

```java
static int multiply(int a, int b) {
    int result = a * b;
    return result;
}

public static void main(String[] args) {
    int total = multiply(10, 12);
}
```

Có thể hình dung:

```text
Call stack

┌──────────────────────────┐
│ multiply frame           │
│ a = 10, b = 12           │
│ result = 120             │
├──────────────────────────┤
│ main frame               │
│ total                    │
└──────────────────────────┘
```

Khi `multiply` trả kết quả, frame của `multiply` được pop khỏi stack.

Đệ quy không có điểm dừng hoặc quá sâu có thể gây `StackOverflowError`.

### 10.2 Heap

Heap là vùng nhớ dùng để cấp phát object và array:

```java
PlanPrice plan = new PlanPrice(
        "PRO",
        new BigDecimal("19.99"),
        "USD"
);
```

Mô hình đơn giản:

```text
Stack                              Heap

main frame                         PlanPrice object
plan ────────────────────────────> name ─────> "PRO"
                                   price ────> BigDecimal object
                                   currency ─> "USD"
```

Local variable `plan` nằm trong frame theo mô hình đơn giản, còn object `PlanPrice`
được cấp phát trên heap.

### 10.3 Garbage Collector

Garbage Collector tìm những object không còn reachable từ các GC root như active
thread stack, static field và một số reference nội bộ của JVM.

```java
PlanPrice plan = new PlanPrice(...);
plan = null;
```

Nếu không còn reference reachable nào trỏ tới object cũ, object đó trở thành ứng viên
để GC thu hồi. Không thể giả định GC sẽ thu hồi ngay lập tức tại dòng `plan = null`.

### 10.4 Stack không chứa toàn bộ object

Câu nói “primitive ở stack, object ở heap” chỉ là mô hình nhập môn và không hoàn toàn
đúng cho mọi trường hợp. Primitive có thể là field bên trong object trên heap, còn JVM
có thể dùng escape analysis và các tối ưu khác. Khi phỏng vấn Junior, nên nói:

> Mỗi thread có stack frame cho method call và local state; object thường được quản lý
> trên heap. Đây là mô hình logic, JVM có thể tối ưu cách lưu trữ thực tế.

### 10.5 StackOverflowError và OutOfMemoryError

- `StackOverflowError`: thường do call stack quá sâu, ví dụ recursion không dừng.
- `OutOfMemoryError`: JVM không thể cấp phát thêm vùng nhớ cần thiết, thường liên quan
  heap hoặc một vùng tài nguyên JVM khác.

Không nên catch hai lỗi này như business exception thông thường. Cần tìm nguyên nhân
thiết kế, cấu hình hoặc memory leak.

---

## 11. Áp dụng vào `PlanPrice`

Một phiên bản đầy đủ cho bài tập:

```java
import java.math.BigDecimal;
import java.math.RoundingMode;

public record PlanPrice(
        String name,
        BigDecimal monthlyPrice,
        String currency
) {
    public PlanPrice {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        if (monthlyPrice == null) {
            throw new IllegalArgumentException("monthlyPrice is required");
        }

        if (monthlyPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("monthlyPrice must not be negative");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
    }

    public BigDecimal yearlyPriceWithDiscount(BigDecimal discountPercent) {
        if (discountPercent == null) {
            throw new IllegalArgumentException("discountPercent is required");
        }

        if (discountPercent.compareTo(BigDecimal.ZERO) < 0
                || discountPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "discountPercent must be between 0 and 100"
            );
        }

        BigDecimal yearlyPrice = monthlyPrice.multiply(BigDecimal.valueOf(12));

        BigDecimal discountAmount = yearlyPrice
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100));

        return yearlyPrice
                .subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
```

Kiến thức được áp dụng:

- `record` dùng làm value object/data carrier.
- Constructor bảo vệ invariant của object.
- `String` được kiểm tra `null` trước khi gọi `isBlank()`.
- Tiền và phần trăm dùng `BigDecimal`.
- `compareTo()` dùng để so sánh giá trị số học.
- Mỗi phép tính `BigDecimal` trả object mới.
- Kết quả tiền được đặt scale và rounding mode rõ ràng.

---

## 12. Những lỗi thường gặp trong ngày 1

### Dùng `==` để so sánh String

Sai:

```java
if (currency == "USD") {
}
```

Đúng:

```java
if ("USD".equals(currency)) {
}
```

Viết literal ở bên trái giúp tránh `NullPointerException` nếu `currency` là `null`.

### Tạo `BigDecimal` từ double

Không nên:

```java
new BigDecimal(0.1);
```

Nên:

```java
new BigDecimal("0.1");
```

### Quên nhận kết quả từ immutable object

Sai:

```java
price.add(tax);
```

Đúng:

```java
price = price.add(tax);
```

### Gọi method trước khi kiểm tra `null`

Sai:

```java
if (name.isBlank() || name == null) {
}
```

Đúng vì `||` short-circuit từ trái sang phải:

```java
if (name == null || name.isBlank()) {
}
```

### Nghĩ rằng `final List` không thể bị sửa

```java
final List<String> names = new ArrayList<>();
names.add("Dat"); // vẫn hợp lệ
```

`final` ngăn gán lại biến, không tự làm object immutable.

### Dùng `Integer == Integer`

Kết quả có thể gây nhầm vì Integer cache. Dùng `equals()` khi muốn so sánh giá trị
object và đã xử lý `null`.

---

## 13. Bài thực hành tự kiểm tra

### Bài 1: Dự đoán kết quả

Không chạy ngay. Hãy viết kết quả dự đoán trước:

```java
String a = new String("Java");
String b = new String("Java");

System.out.println(a == b);
System.out.println(a.equals(b));
```

### Bài 2: Pass-by-value

Viết một mutable class `Counter`, sau đó tạo hai method:

- `increment(Counter counter)` sửa field của object.
- `replace(Counter counter)` gán parameter sang object mới.

Giải thích vì sao một thay đổi được caller nhìn thấy và thay đổi còn lại không.

### Bài 3: `final` và mutable object

Tạo `final ArrayList<String>`, thêm một phần tử rồi thử gán biến sang một ArrayList mới.
Ghi lại dòng nào compile và dòng nào không compile.

### Bài 4: BigDecimal

Tính giá một năm cho gói `19.99 USD/tháng`, giảm `10%` và làm tròn hai chữ số. Kết
quả phải là `215.89`.

### Bài 5: Boundary validation

Kiểm tra các trường hợp:

| Monthly price | Discount | Kết quả mong đợi |
|---:|---:|---|
| `10` | `0` | `120.00` |
| `10` | `10` | `108.00` |
| `10` | `100` | `0.00` |
| `0` | `10` | `0.00` |
| `-1` | `10` | Ném exception |
| `10` | `-1` | Ném exception |
| `10` | `101` | Ném exception |

---

## 14. Bộ câu hỏi phỏng vấn và đáp án ngắn

### JDK, JRE và JVM khác nhau thế nào?

JDK là bộ công cụ phát triển, gồm compiler và runtime. JRE là môi trường chạy. JVM là
máy ảo thực thi bytecode, quản lý bộ nhớ và GC.

### Java có phải pass-by-reference không?

Không. Java luôn pass-by-value. Khi truyền object, giá trị được copy là reference.

### `==` và `equals()` khác nhau thế nào?

Với primitive, `==` so sánh giá trị. Với reference, `==` so sánh identity. `equals()`
so sánh equality logic do class định nghĩa.

### Vì sao String immutable?

Immutability giúp String được chia sẻ an toàn, dùng ổn định làm hash key, hỗ trợ String
pool và dễ dùng hơn trong môi trường nhiều thread.

### Vì sao không dùng double cho tiền?

`double` là binary floating-point nên nhiều số thập phân không biểu diễn chính xác.
Tiền cần decimal arithmetic và rounding rule rõ ràng, thường dùng `BigDecimal`.

### `final` có làm object immutable không?

Không. `final` reference chỉ không được trỏ sang object khác. Trạng thái của object vẫn
có thể thay đổi nếu class cung cấp operation mutable.

### `int` và `Integer` khác nhau thế nào?

`int` là primitive, không thể `null`. `Integer` là wrapper object, có thể `null`, dùng
được trong generic nhưng có boxing/unboxing và nguy cơ NPE khi unbox `null`.

### Stack và heap khác nhau thế nào?

Mỗi thread có stack chứa method frame và local state. Object thường được quản lý trên
heap và được GC thu hồi khi không còn reachable. Đây là mô hình logic; JVM có thể tối
ưu cách lưu trữ thực tế.

---

## 15. Cách học tài liệu này

1. Đọc một mục trong 10–15 phút.
2. Đóng tài liệu và tự giải thích lại bằng lời.
3. Tự gõ ví dụ, không copy-paste.
4. Đoán output trước khi chạy.
5. Thay đổi input để tạo lỗi có chủ đích.
6. Đọc compile error hoặc stack trace từ trên xuống.
7. Cuối buổi trả lời bộ câu hỏi phỏng vấn không nhìn đáp án.

Chạy bài hiện tại nhanh nhất trong VS Code:

- `Ctrl + F5`: chạy không debug.
- `F5`: chạy debug.
- Hoặc bấm `Run` phía trên method `main`.

Chạy trực tiếp bằng PowerShell:

```powershell
java .\src\main\java\vn\dangquangdat\javabegin\learning\day01\Day1Exercise.java
```

## Definition of done

Bạn hoàn thành ngày 1 khi có thể:

- Giải thích 10 câu hỏi mục tiêu bằng lời của mình.
- Viết lại `PlanPrice` mà không nhìn lời giải.
- Dự đoán đúng kết quả của các ví dụ `==`, `equals()` và pass-by-value.
- Dùng `BigDecimal` đúng cho phép tính tiền.
- Phân biệt `final reference` với immutable object.
- Đọc được stack trace khi constructor ném `IllegalArgumentException`.
- Đặt breakpoint và quan sát local variable/call stack trong VS Code.
