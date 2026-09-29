# Ngày 5 — Exception, Optional, ngày giờ và file I/O

Ngày 5 học cách chương trình biểu diễn trường hợp thất bại, kết quả có thể vắng mặt,
thời gian và dữ liệu file. Đây là các boundary dễ phát sinh lỗi nhất trong backend.

Code thực hành:

- [ExceptionsAndIoDemo.java](./ExceptionsAndIoDemo.java): tạo file tạm, import từng
  dòng bằng try-with-resources, wrap `IOException`, tìm dữ liệu bằng `Optional` và ghi
  timestamp bằng `Instant`.

## Mục tiêu

Sau ngày này, bạn cần:

1. Đọc được exception stack trace và lần theo cause.
2. Phân biệt checked exception, unchecked exception và `Error`.
3. Biết khi nào catch, khi nào declare và khi nào wrap exception.
4. Preserve nguyên nhân bằng exception chaining.
5. Dùng try-with-resources để đóng tài nguyên.
6. Dùng `Optional` đúng chủ yếu ở return type.
7. Chọn đúng type trong `java.time`.
8. Hiểu timezone, UTC và daylight-saving time ở mức nền tảng.
9. Đọc file an toàn và nhận biết rủi ro upload/path traversal.
10. Chuyển lỗi domain thành HTTP status phù hợp.

---

## 1. Chạy và đọc code mẫu

```powershell
java .\src\main\java\vn\dangquangdat\javabegin\learning\day05\ExceptionsAndIoDemo.java
```

Output có dạng:

```text
Imported at 2026-09-29T...Z: [DataSource[id=1, name=github],
    DataSource[id=2, name=postgres], DataSource[id=3, name=notion]]
Found: DataSource[id=2, name=postgres]
```

Timestamp thay đổi ở mỗi lần chạy. Dòng rỗng trong file bị bỏ qua; ID chỉ tăng khi
thêm một data source hợp lệ.

Luồng chạy:

```text
createTempFile
    │
writeString
    │
DataSourceImporter.importFrom(path)
    ├─ kiểm tra readable
    ├─ mở BufferedReader
    ├─ đọc từng dòng
    ├─ trim + bỏ blank
    ├─ tạo immutable snapshot
    └─ tự đóng reader
    │
findByName(..., "postgres") → Optional<DataSource>
    │
in kết quả
    │
deleteIfExists
```

`main` declare `throws IOException` vì việc tạo, ghi và xóa file tạm vẫn có thể ném
checked exception. Phần đọc trong importer được chuyển sang custom runtime exception.

---

## 2. Exception dùng để làm gì?

Exception biểu diễn việc luồng thực thi bình thường không thể tiếp tục theo contract
hiện tại:

- Input vi phạm rule.
- File không tồn tại hoặc không đọc được.
- Kết nối database thất bại.
- Resource được yêu cầu không tồn tại.
- Hệ thống bên ngoài timeout.

Không dùng exception thay cho mọi nhánh bình thường. “Không tìm thấy kết quả tìm kiếm”
có thể là `Optional.empty()`; “file hệ thống phải có nhưng không đọc được” là lỗi.

Exception propagation:

```text
main()
  └─ service()
       └─ importFrom()
            └─ Files.newBufferedReader() throws IOException

nếu không catch ở importFrom:
exception đi ngược call stack lên caller
```

Khi exception thoát khỏi một method, phần code còn lại của method không chạy, trừ
cleanup trong `finally` hoặc try-with-resources.

---

## 3. Exception hierarchy

Mô hình rút gọn:

```text
Throwable
├─ Error
│  ├─ OutOfMemoryError
│  └─ StackOverflowError
└─ Exception
   ├─ IOException                 checked
   └─ RuntimeException            unchecked
      ├─ IllegalArgumentException
      ├─ NullPointerException
      └─ IllegalStateException
```

### Checked exception

Subclass của `Exception` nhưng không phải `RuntimeException`. Compiler buộc caller:

- Catch exception; hoặc
- Declare bằng `throws`.

`IOException` là ví dụ điển hình vì I/O phụ thuộc môi trường bên ngoài.

### Unchecked exception

`RuntimeException` và subclass không bị compiler bắt buộc catch/declare. Thường dùng
cho input/programming/domain invariant mà caller hiện tại không thể khôi phục cục bộ:

```java
if (path == null) {
    throw new IllegalArgumentException("path is required");
}
```

### Error

`Error` thường biểu diễn vấn đề nghiêm trọng của JVM/môi trường. Không catch
`OutOfMemoryError` hay `StackOverflowError` như business flow thông thường.

Checked không tự động “tốt hơn” unchecked. Hãy chọn theo contract và khả năng caller
phục hồi, không chỉ theo nguồn gốc kỹ thuật của lỗi.

---

## 4. `throw`, `throws`, `try` và `catch`

- `throw` ném một exception object.
- `throws` khai báo method có thể để exception thoát ra.
- `try` bao code có thể thất bại.
- `catch` xử lý loại exception phù hợp.

```java
static String read(Path path) throws IOException {
    if (path == null) {
        throw new IllegalArgumentException("path is required");
    }
    return Files.readString(path);
}
```

Catch từ cụ thể đến tổng quát:

```java
try {
    importData();
} catch (NoSuchFileException exception) {
    // xử lý file không tồn tại
} catch (IOException exception) {
    // xử lý lỗi I/O còn lại
}
```

Nếu catch `Exception` trước, catch cụ thể phía sau trở thành unreachable.

Chỉ catch khi lớp hiện tại có thể:

- Khôi phục hợp lý.
- Bổ sung context rồi rethrow.
- Chuyển sang abstraction phù hợp hơn.
- Chuyển thành response tại application boundary.

---

## 5. Preserve cause khi wrap exception

Code mẫu:

```java
catch (IOException exception) {
    throw new DataSourceImportException(
            "Cannot import " + path,
            exception
    );
}
```

Custom exception:

```java
static final class DataSourceImportException
        extends RuntimeException {

    DataSourceImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Message mới bổ sung ngữ cảnh nghiệp vụ; `cause` giữ stack trace gốc.

Sai:

```java
catch (IOException exception) {
    throw new DataSourceImportException(
            "Import failed",
            null
    );
}
```

Khi mất cause, người vận hành không biết lỗi thật là permission, file biến mất hay
encoding/I/O.

### Log ở đâu?

Không log rồi rethrow ở mọi layer vì cùng lỗi xuất hiện nhiều lần. Thường exception
được làm giàu context qua các layer và log một lần tại boundary có đủ request ID,
user ID và operation. Không log secret, token hoặc dữ liệu nhạy cảm.

---

## 6. Đọc stack trace

Ví dụ:

```text
DataSourceImportException: Cannot import data.txt
    at DataSourceImporter.importFrom(ExceptionsAndIoDemo.java:...)
    at ExceptionsAndIoDemo.main(ExceptionsAndIoDemo.java:...)
Caused by: java.nio.file.NoSuchFileException: data.txt
    at ...
```

Cách đọc:

1. Đọc exception type và message đầu tiên.
2. Tìm `Caused by` sâu nhất để biết nguyên nhân gốc.
3. Tìm frame đầu tiên thuộc package của mình.
4. Mở đúng file và dòng.
5. Kiểm tra input và state tại frame đó.
6. Không chỉ sửa để exception biến mất; xác định contract bị vi phạm.

Stack trace đi từ nơi lỗi được tạo qua chuỗi lời gọi. Các frame framework rất nhiều
không có nghĩa framework là nguyên nhân.

---

## 7. Try-with-resources

`BufferedReader` implement `AutoCloseable`. Code:

```java
try (BufferedReader reader = Files.newBufferedReader(path)) {
    String line;
    while ((line = reader.readLine()) != null) {
        // xử lý
    }
}
```

Java tự gọi `close()` dù block:

- Chạy thành công.
- `return` sớm.
- Ném exception.

Nhiều resource được đóng theo thứ tự ngược lúc tạo:

```java
try (InputStream input = ...;
     BufferedInputStream buffered = new BufferedInputStream(input)) {
    // dùng buffered
}
```

`buffered` đóng trước, rồi `input`.

Nếu body và `close()` cùng ném lỗi, lỗi đóng thường được lưu dưới dạng suppressed
exception và có thể xem bằng `getSuppressed()`.

Không dựa vào Garbage Collector để đóng file, socket hay database connection. GC quản
lý memory, không bảo đảm thời điểm giải phóng tài nguyên hệ điều hành.

---

## 8. Phân tích `importFrom`

### Kiểm tra đầu vào

```java
if (!Files.isReadable(path)) {
    throw new IllegalArgumentException(
            "File is not readable: " + path
    );
}
```

Nên kiểm tra `path == null` trước. `isReadable` chỉ là quan sát tại một thời điểm; file
có thể bị xóa hoặc permission đổi ngay sau đó. Vì vậy vẫn phải catch lỗi khi mở/đọc.

### Đọc streaming

```java
String line;
while ((line = reader.readLine()) != null) {
```

`readLine()` trả `null` ở cuối file. Cách này xử lý từng dòng và không cần giữ toàn bộ
file raw trong memory.

### Normalize dữ liệu

```java
if (!line.isBlank()) {
    result.add(new DataSource(id++, line.trim()));
}
```

- `isBlank` nhận biết chuỗi rỗng hoặc chỉ whitespace.
- `trim` bỏ khoảng trắng đầu/cuối theo semantics của method.
- `id++` dùng ID hiện tại rồi mới tăng.

### Không để lộ list mutable

```java
return List.copyOf(result);
```

Caller không thể thêm/xóa. Đây vẫn là shallow copy; may mắn `DataSource` là record chỉ
chứa `long` và `String` immutable.

---

## 9. Optional biểu diễn kết quả có thể vắng

Method:

```java
Optional<DataSource> findByName(
        List<DataSource> sources,
        String name
) {
    return sources.stream()
            .filter(source ->
                    source.name().equalsIgnoreCase(name))
            .findFirst();
}
```

Return type buộc caller nhìn thấy khả năng không có kết quả.

### Tạo Optional

```java
Optional.of(value);          // value không được null
Optional.ofNullable(value);  // null → empty
Optional.empty();
```

### Dùng Optional

```java
source.ifPresent(System.out::println);

String name = source
        .map(DataSource::name)
        .orElse("unknown");

DataSource required = source.orElseThrow(
        () -> new DataSourceNotFoundException("not found")
);
```

Tránh:

```java
source.get(); // ném NoSuchElementException nếu empty
```

### `orElse` và `orElseGet`

```java
value.orElse(expensiveFallback());           // fallback luôn được tính
value.orElseGet(() -> expensiveFallback());  // chỉ tính khi empty
```

### Optional nên dùng ở đâu?

Phù hợp nhất ở return type “có thể không tìm thấy”. Thường tránh dùng làm:

- Field của JPA entity.
- Field request/response DTO.
- Method parameter.
- Wrapper cho collection; collection rỗng đã diễn đạt “không có phần tử”.

Optional không thay thế validation. Code mẫu vẫn nên quyết định rõ behavior khi
`sources` hoặc `name` là null.

---

## 10. `java.time` và mental model

| Type | Biểu diễn | Ví dụ dùng |
|---|---|---|
| `Instant` | Một điểm trên timeline UTC | createdAt, audit timestamp |
| `LocalDate` | Ngày không có giờ/timezone | ngày sinh |
| `LocalTime` | Giờ trong ngày | giờ mở cửa |
| `LocalDateTime` | Ngày + giờ nhưng chưa có zone | giá trị lịch địa phương |
| `ZonedDateTime` | Ngày giờ + region zone | lịch họp theo địa phương |
| `OffsetDateTime` | Ngày giờ + UTC offset | API timestamp có offset |
| `Duration` | Khoảng thời gian theo giây/nano | timeout, latency |
| `Period` | Khoảng theo năm/tháng/ngày | chu kỳ lịch |

Code mẫu:

```java
Instant now = Instant.now();
```

`Instant` thích hợp cho “sự kiện xảy ra lúc nào” và thường được lưu theo UTC.

### Chuyển timezone

```java
ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
ZonedDateTime local = instant.atZone(zone);
```

Đổi zone không đổi khoảnh khắc, chỉ đổi cách hiển thị địa phương.

### Parse lịch địa phương thành Instant

```java
LocalDateTime input = LocalDateTime.parse(
        "2026-09-29T09:30"
);
Instant instant = input
        .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
        .toInstant();
```

`LocalDateTime` một mình không xác định duy nhất điểm trên timeline. Phải biết zone.

### Vì sao dùng region zone thay offset cố định?

`Europe/Paris` chứa rule lịch sử/daylight-saving; `+01:00` chỉ là offset tại một thời
điểm. Việt Nam hiện không DST, nhưng hệ thống quốc tế vẫn phải dùng đúng zone theo
nghiệp vụ.

Các class `java.time` là immutable và thread-safe. Formatter tạo từ pattern cũng
thread-safe, khác nhiều API ngày giờ cũ.

---

## 11. File I/O với `Path` và `Files`

`Path` biểu diễn đường dẫn; `Files` cung cấp operation:

```java
Path path = Path.of("data", "sources.txt");
boolean exists = Files.exists(path);
String content = Files.readString(path);
Files.writeString(path, content);
```

API overload không truyền charset của `readString/writeString/newBufferedReader` dùng
UTF-8. Khi contract yêu cầu charset khác, truyền `Charset` rõ ràng.

### Đọc toàn bộ hay streaming?

- `Files.readString/readAllLines`: đơn giản, phù hợp file nhỏ có giới hạn.
- `BufferedReader` hoặc `Files.lines`: xử lý dần file lớn; stream/resource phải đóng.

### Cleanup file tạm

Code mẫu gọi `deleteIfExists` ở cuối. Nếu exception xảy ra trước dòng đó, file có thể
còn lại. Một bản thực hành an toàn hơn:

```java
Path file = Files.createTempFile("sources-", ".txt");
try {
    // ghi, đọc và xử lý
} finally {
    Files.deleteIfExists(file);
}
```

`finally` gần như luôn chạy khi rời `try`, nhưng không được bảo đảm nếu process/JVM bị
kill, crash nghiêm trọng hoặc mất điện.

---

## 12. An toàn khi upload file

Production không được tin filename hay `Content-Type` từ client.

Các biện pháp cơ bản:

- Giới hạn kích thước request và kích thước sau giải nén.
- Allowlist loại file cần thiết; kiểm tra nội dung/signature khi phù hợp.
- Sinh tên server-side, không dùng filename làm đường dẫn trực tiếp.
- Normalize path và bảo đảm target vẫn nằm trong thư mục cho phép.
- Chặn path traversal như `../../secret.txt`.
- Cẩn thận symlink và race condition.
- Lưu ngoài web root; phân quyền tối thiểu.
- Scan malware nếu threat model yêu cầu.
- Không log nội dung nhạy cảm.
- Cleanup file tạm kể cả khi xử lý lỗi.

Ví dụ kiểm tra target:

```java
Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();
Path target = uploadRoot.resolve(serverGeneratedName).normalize();

if (!target.startsWith(uploadRoot)) {
    throw new SecurityException("Invalid upload path");
}
```

Đây chỉ là một lớp bảo vệ; thiết kế thật còn phụ thuộc OS, storage và deployment.

---

## 13. Custom exception và HTTP error

Custom exception hữu ích khi caller cần phân biệt lỗi theo domain:

```java
class DataSourceNotFoundException extends RuntimeException {
    DataSourceNotFoundException(String message) {
        super(message);
    }
}
```

Không cần tạo một class exception mới chỉ để đổi vài chữ nếu không có semantics hoặc
handling khác biệt.

Mapping phổ biến:

| Tình huống SoftAIBox | HTTP status |
|---|---:|
| Request sai định dạng/rule đầu vào | 400 |
| Chưa xác thực/token sai | 401 |
| Đã xác thực nhưng không có quyền | 403 |
| Data source/conversation không tồn tại | 404 |
| Tên trùng hoặc state conflict | 409 |
| Lỗi bất ngờ phía server | 500 |

Không trả raw stack trace hoặc internal exception message ra production. Response nên
có error code ổn định, message an toàn và request/correlation ID.

---

## 14. Liên hệ SoftAIBox

Một luồng import data source:

```text
HTTP upload
  → validate metadata/size/type
  → lưu file tạm bằng tên server sinh
  → importer đọc streaming
  → parse từng dòng
  → thu thập lỗi có line number
  → service lưu dữ liệu trong transaction
  → cleanup file
  → response summary
```

Không nên dừng toàn bộ ở dòng CSV lỗi đầu tiên nếu nghiệp vụ muốn người dùng sửa một
lần. Có thể trả:

```java
record ImportError(long lineNumber, String code, String message) {}

record ImportResult(
        List<DataSource> imported,
        List<ImportError> errors
) {}
```

Phân biệt lỗi kỹ thuật tạm thời với lỗi dữ liệu vĩnh viễn để quyết định retry. Retry
một file sai định dạng không giúp gì; retry network timeout có thể hợp lý nếu operation
idempotent.

---

## 15. Những lỗi thường gặp

### Catch rồi bỏ qua

```java
try {
    importData();
} catch (Exception ignored) {
}
```

Chương trình giả vờ thành công và mất thông tin chẩn đoán.

### Catch quá rộng

`catch (Exception)` có thể nuốt cả programming bug. Catch loại mà layer biết xử lý.

### Wrap nhưng làm mất cause

Luôn truyền cause khi chuyển lỗi kỹ thuật thành lỗi domain/application.

### Log và rethrow ở mọi layer

Sinh log trùng, khó đọc. Log một lần tại boundary phù hợp.

### Không đóng resource

Dẫn đến cạn file descriptor, socket hoặc connection. Dùng try-with-resources.

### Gọi `Optional.get()`

Nó chỉ chuyển null problem thành `NoSuchElementException`. Dùng `map`,
`orElseThrow` hoặc handling rõ ràng.

### Dùng `LocalDateTime` cho timestamp tuyệt đối

Thiếu timezone nên cùng text có thể trỏ đến khoảnh khắc khác nhau.

### Tin filename từ client

Có thể dẫn đến path traversal, overwrite hoặc lộ file.

---

## 16. Bài thực hành

### Cấp 1 — Đọc lỗi

Chạy với path không tồn tại. Ghi lại:

- Exception type ngoài cùng.
- Root cause.
- Frame đầu tiên thuộc code của bạn.
- Input gây lỗi.

### Cấp 2 — Optional

Viết:

```java
Optional<DataSource> findById(List<DataSource> values, long id)
Optional<String> findNameById(List<DataSource> values, long id)
```

Không dùng `get()` và không trả `null`.

### Cấp 3 — CSV có lỗi

Import định dạng `email,role`. Không dừng ở lỗi đầu; trả danh sách lỗi với số dòng.
Validate email blank, role không hợp lệ và thừa/thiếu cột.

### Cấp 4 — Thời gian

Parse `2026-09-29 09:30` theo `Asia/Ho_Chi_Minh` thành `Instant`, sau đó hiển thị cùng
khoảnh khắc ở `Europe/Paris`. Giải thích vì sao giờ địa phương khác.

### Cấp 5 — Cleanup

Viết chương trình tạo file tạm, cố tình ném exception giữa quá trình và chứng minh
file vẫn được xóa trong `finally`.

### Cấp 6 — Error contract

Thiết kế JSON lỗi thống nhất gồm `code`, `message`, `timestamp`, `path` và
`requestId`. Ánh xạ 400/401/403/404/409/500 bằng ví dụ domain.

---

## 17. Câu hỏi phỏng vấn và đáp án ngắn

### Checked và unchecked exception khác nhau thế nào?

Checked exception phải được catch hoặc declare ở compile time. Unchecked
`RuntimeException` không bị bắt buộc. Việc chọn loại phụ thuộc contract và khả năng
caller phục hồi.

### `throw` khác `throws`?

`throw` thực sự ném exception object; `throws` khai báo exception có thể thoát khỏi
method.

### Vì sao preserve cause?

Để vừa có context ở abstraction hiện tại vừa giữ root cause và stack trace phục vụ
debug.

### Try-with-resources có lợi gì?

Nó tự đóng `AutoCloseable` kể cả khi return hoặc có exception, đồng thời quản lý lỗi
phát sinh lúc close.

### `finally` có luôn chạy không?

Nó chạy khi control flow rời try/catch bình thường, kể cả return/exception, nhưng
không bảo đảm khi JVM/process bị dừng cưỡng bức hoặc crash nghiêm trọng.

### Optional nên dùng ở đâu?

Chủ yếu ở return type để biểu diễn có thể không có kết quả. Thường tránh field entity,
DTO field và parameter.

### `orElse` khác `orElseGet`?

Argument của `orElse` được tính ngay; supplier của `orElseGet` chỉ chạy khi Optional
empty.

### Instant khác LocalDateTime?

Instant là một điểm tuyệt đối trên timeline UTC. LocalDateTime chỉ là ngày giờ địa
phương chưa gắn zone, nên chưa xác định duy nhất một khoảnh khắc.

### Vì sao lưu timestamp theo UTC?

UTC tạo mốc thống nhất để so sánh và trao đổi giữa hệ thống; chuyển sang timezone địa
phương khi hiển thị hoặc áp dụng rule lịch.

### Khi nào tạo custom exception?

Khi cần semantics domain/application riêng, handling/mapping riêng hoặc cần bổ sung
context ở abstraction hiện tại.

## Definition of done

Bạn hoàn thành Ngày 5 khi có thể:

- Giải thích hierarchy exception và checked/unchecked.
- Đọc stack trace từ wrapper tới root cause.
- Catch đúng mức, wrap và preserve cause.
- Dùng try-with-resources cho reader/stream.
- Dùng Optional mà không gọi `get()` mù quáng.
- Chọn `Instant`, `LocalDate`, `LocalDateTime` hoặc `ZonedDateTime` đúng ngữ nghĩa.
- Parse thời gian địa phương thành Instant.
- Đọc file theo dòng và cleanup file tạm khi lỗi.
- Nêu các biện pháp chống path traversal/upload nguy hiểm.
- Thiết kế import result có lỗi theo số dòng.
- Ánh xạ lỗi SoftAIBox sang HTTP status phù hợp.
