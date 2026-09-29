# Ngày 9 — Maven, JUnit và tư duy kiểm thử

Ngày 9 kết nối hai kỹ năng: dùng Maven để build dự án lặp lại được và dùng test để mô tả, bảo vệ hành vi. Mục tiêu không phải chạy theo coverage, mà là chọn ví dụ có giá trị, đặc biệt ở boundary và nhánh lỗi.

Code liên quan:

- [PromotionService.java](./PromotionService.java): pure business logic tính giá sau giảm.
- [PromotionServiceTest.java](../../../../../../../test/java/vn/dangquangdat/javabegin/learning/day09/PromotionServiceTest.java): happy path và invalid boundary bằng JUnit 5.
- [`pom.xml`](../../../../../../../../pom.xml): Java version, dependencies và build plugins.

## Mục tiêu

Sau ngày 9, bạn cần:

1. Hiểu Maven giải quyết vấn đề gì.
2. Đọc được cấu trúc cơ bản của `pom.xml`.
3. Phân biệt lifecycle, phase, goal và plugin.
4. Dùng `compile`, `test`, `package`, `verify`, `install`, `clean`.
5. Hiểu dependency transitive và scope.
6. Biết vì sao nên commit Maven Wrapper.
7. Phân biệt unit, integration và end-to-end test.
8. Viết test JUnit 5 theo Arrange–Act–Assert.
9. Test happy path, boundary và exception.
10. Hiểu test double: fake, stub, mock, spy.
11. Tránh flaky test và over-mocking.
12. So sánh `BigDecimal` đúng theo mục đích.

---

## 1. Maven giải quyết vấn đề gì?

Dự án Java thực tế cần:

- Tải và quản lý thư viện.
- Compile source/test.
- Chạy test.
- Đóng gói JAR.
- Chạy plugin kiểm tra.
- Tạo quy trình giống nhau trên máy dev và CI.

Maven dùng cấu hình khai báo trong `pom.xml`. Thay vì mỗi người tự tải JAR và viết command khác nhau, dự án mô tả dependency, plugin và metadata tập trung.

```text
pom.xml
  -> resolve dependencies
  -> compile main source
  -> compile test source
  -> run tests
  -> package artifact
```

Maven không chỉ là dependency downloader; nó là build tool có lifecycle và convention.

## 2. Convention về thư mục

Maven mặc định hiểu:

```text
src/main/java       Java production source
src/main/resources  Production resources
src/test/java       Test source
src/test/resources  Test resources
target              Build output/report
pom.xml             Project model
```

Nhờ convention, không cần cấu hình đường dẫn cho trường hợp thông thường.

`target` là generated output, có thể xóa và sinh lại. Không đặt source quan trọng thủ công trong đó.

## 3. Đọc `pom.xml` của dự án

Identity:

```xml
<groupId>vn.dangquangdat</groupId>
<artifactId>softaibox-java-lab</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

Tọa độ artifact thường viết:

```text
groupId:artifactId:version
```

`SNAPSHOT` biểu thị phiên bản đang phát triển, có thể thay đổi.

Java version:

```xml
<properties>
    <java.version>25</java.version>
</properties>
```

Parent:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>
```

Parent cung cấp dependency/plugin management và default phù hợp Spring Boot. Vì version có thể thay theo thời gian, khi nâng cấp phải đọc migration notes và chạy test; không tự nâng chỉ vì có bản mới.

## 4. Dependency và transitive dependency

Một starter có thể kéo theo các thư viện nó phụ thuộc; đó là **transitive dependencies**.

Xem dependency tree:

```powershell
.\mvnw.cmd dependency:tree
```

Dependency gián tiếp tiện nhưng có thể tạo version conflict, thư viện thừa hoặc lỗ hổng. Hãy đọc dependency tree và không thêm thư viện chỉ để dùng một tiện ích nhỏ mà Java chuẩn đã có.

## 5. Dependency scope

Các scope quan trọng:

| Scope | Main compile | Test | Runtime/artifact |
|---|---:|---:|---:|
| `compile` mặc định | Có | Có | Có |
| `test` | Không | Có | Không |
| `runtime` | Không trực tiếp | Có | Có |
| `provided` | Có | Có | Do môi trường cung cấp |

Trong dự án:

- `spring-boot-starter-test`: `test`, không đi vào production artifact.
- H2: `runtime`, source chính không cần compile trực tiếp với API H2 nhưng app cần driver lúc chạy.

Scope sai có thể làm build chạy local nhưng artifact thiếu class, hoặc kéo test library vào production.

---

## 6. Lifecycle, phase và goal

Maven có các lifecycle. Hai loại thường dùng:

- `default`: compile, test, package, verify, install, deploy.
- `clean`: xóa output build cũ.

Các phase default theo thứ tự rút gọn:

```text
validate
-> compile
-> test
-> package
-> verify
-> install
-> deploy
```

Chạy một phase sẽ chạy các phase trước nó trong cùng lifecycle:

```powershell
.\mvnw.cmd package
```

sẽ compile và test trước khi package, trừ khi bị cấu hình skip.

Ý nghĩa:

- `compile`: compile production code.
- `test`: compile và chạy unit test.
- `package`: tạo JAR/WAR.
- `verify`: chạy kiểm tra bổ sung sau package/integration flow.
- `install`: đưa artifact vào local Maven repository.
- `deploy`: publish lên remote repository.

`clean` thuộc lifecycle khác:

```powershell
.\mvnw.cmd clean test
```

Nó xóa `target`, rồi chạy đến phase `test`.

### Goal và plugin

Plugin thực hiện công việc; goal là task cụ thể:

```powershell
.\mvnw.cmd dependency:tree
```

Trong đó `dependency` là prefix plugin, `tree` là goal. Phase có thể bind nhiều plugin goal.

Không cần chạy `install` cho mọi lần test. `test` hoặc `verify` thường đủ; `install` có ý nghĩa khi project khác trên máy cần artifact.

## 7. Maven Wrapper

Dự án commit `mvnw`, `mvnw.cmd` và `.mvn/wrapper`. Trên Windows:

```powershell
.\mvnw.cmd test
```

Wrapper giúp dev/CI dùng Maven version thống nhất. Nó không khóa JDK, OS hay external service, nhưng loại một nguồn sai khác quan trọng.

---

## 8. Vì sao viết test?

Test tốt phản hồi nhanh, mô tả behavior, hỗ trợ refactor và bảo vệ boundary/bug cũ. Test không chứng minh chương trình hết bug; coverage cao với assertion yếu vẫn ít giá trị. Ưu tiên theo rủi ro: tiền/làm tròn, security/ownership, state transition, idempotency và logic nhiều nhánh.

## 9. Các tầng kiểm thử

| Loại | Phạm vi | Đặc điểm |
|---|---|---|
| Unit | Một đơn vị logic | Nhanh, deterministic, không Spring/DB/network |
| Integration | Nhiều component thật | Bắt lỗi query, mapping, wiring/configuration |
| End-to-end | Hệ thống từ boundary ngoài | Giá trị cao nhưng chậm và setup khó |

```text
HTTP -> security -> controller -> service -> database
```

`PromotionServiceTest` là unit test. Thường dùng nhiều unit test có giá trị, integration test vừa đủ và ít E2E cho critical journey; không coi test pyramid là tỷ lệ cứng.

## 10. FIRST và test tốt

FIRST nhắc rằng test nên **Fast**, **Independent**, **Repeatable**, **Self-validating** và **Timely**. Tránh phụ thuộc clock không kiểm soát, production network, thứ tự test, dữ liệu test trước hoặc `Thread.sleep` với timing mong manh.

---

## 11. Cấu trúc JUnit 5

```java
class PromotionServiceTest {
    private final PromotionService service =
            new PromotionService();

    @Test
    void shouldApplyPromotionWithoutFloatingPointError() {
        BigDecimal result = service.applyPercent(
                new BigDecimal("19.99"), 25);

        assertEquals(new BigDecimal("14.99"), result);
    }
}
```

JUnit 5 dùng `org.junit.jupiter.api.Test`. Tên test nên mô tả behavior theo dạng `should + expected behavior + context`, không cần lặp tên method production.

## 12. Arrange–Act–Assert

```java
@Test
void shouldApplyTwentyFivePercentDiscount() {
    // Arrange
    BigDecimal original = new BigDecimal("19.99");

    // Act
    BigDecimal result = service.applyPercent(original, 25);

    // Assert
    assertEquals(new BigDecimal("14.99"), result);
}
```

Các phần có thể không cần comment nếu code ngắn, nhưng tư duy vẫn nên rõ:

- Arrange: tạo input/dependency.
- Act: gọi đúng behavior.
- Assert: kiểm kết quả/observable effect.

Mỗi test nên thất bại vì một lý do rõ. “Một assertion mỗi test” không phải luật tuyệt đối; nhiều assertion liên quan cùng behavior có thể hợp lý.

## 13. Test exception

```java
@Test
void shouldRejectPercentGreaterThanOneHundred() {
    assertThrows(
            IllegalArgumentException.class,
            () -> service.applyPercent(
                    new BigDecimal("19.99"), 101)
    );
}
```

`assertThrows` trả exception để kiểm thêm:

```java
IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> service.applyPercent(price, 101)
);

assertEquals(
        "percentOff must be between 0 and 100",
        exception.getMessage()
);
```

Chỉ bọc call dự kiến ném lỗi. Nếu lambda chứa nhiều thao tác, test có thể pass vì sai nguyên nhân.

Không nên test stack trace hoặc chi tiết implementation không phải contract.

## 14. Boundary và equivalence partition

Rule:

```text
0 <= percentOff <= 100
originalPrice >= 0 và khác null
```

Các boundary quan trọng:

```text
percent: -1, 0, 1, 99, 100, 101
price: null, -0.01, 0.00, positive
```

Equivalence partition giúp không test mọi số:

- Nhỏ hơn 0: invalid.
- 0 đến 100: valid.
- Lớn hơn 100: invalid.

Chọn boundary đại diện thường bắt lỗi `>`/`>=` tốt hơn chọn ngẫu nhiên.

## 15. Parameterized test

Nhiều case cùng cấu trúc:

```java
@ParameterizedTest
@CsvSource({
        "19.99, 0,   19.99",
        "19.99, 25,  14.99",
        "19.99, 100, 0.00"
})
void shouldApplyValidPercent(
        String original,
        int percent,
        String expected
) {
    BigDecimal result = service.applyPercent(
            new BigDecimal(original),
            percent
    );

    assertEquals(new BigDecimal(expected), result);
}
```

Parameterized test giảm lặp nhưng đừng nhồi case có setup/assertion rất khác nhau vào một bảng khó đọc.

Invalid boundary:

```java
@ParameterizedTest
@ValueSource(ints = {-1, 101})
void shouldRejectInvalidPercent(int percent) {
    assertThrows(
            IllegalArgumentException.class,
            () -> service.applyPercent(
                    new BigDecimal("19.99"), percent)
    );
}
```

---

## 16. Phân tích `PromotionService`

Guard giá:

```java
if (originalPrice == null
        || originalPrice.signum() < 0) {
    throw new IllegalArgumentException(
            "originalPrice must be non-negative");
}
```

Short-circuit `||` tránh gọi `signum()` trên `null`.

Guard phần trăm:

```java
if (percentOff < 0 || percentOff > 100) {
    throw new IllegalArgumentException(
            "percentOff must be between 0 and 100");
}
```

Multiplier:

```java
BigDecimal multiplier =
        BigDecimal.valueOf(100L - percentOff)
                .divide(
                        BigDecimal.valueOf(100),
                        4,
                        RoundingMode.HALF_UP
                );
```

Với 25%:

```text
(100 - 25) / 100 = 0.7500
```

Kết quả:

```java
return originalPrice
        .multiply(multiplier)
        .setScale(2, RoundingMode.HALF_UP);
```

Tính tay:

```text
19.99 × 0.7500 = 14.992500
scale 2, HALF_UP = 14.99
```

Service gần pure function:

- Output chỉ phụ thuộc input.
- Không DB/network/clock/global mutable state.
- Không sửa input vì `BigDecimal` immutable.

Do đó unit test rất nhanh và không cần mock.

## 17. `BigDecimal` trong assertion

`BigDecimal.equals()` xét cả value và scale:

```java
new BigDecimal("10.0")
        .equals(new BigDecimal("10.00")); // false
```

`compareTo` chỉ xét giá trị số:

```java
new BigDecimal("10.0")
        .compareTo(new BigDecimal("10.00")); // 0
```

Trong service này, contract trả scale 2:

```java
.setScale(2, RoundingMode.HALF_UP)
```

Vì vậy `assertEquals(new BigDecimal("14.99"), result)` hợp lý: nó đồng thời bảo vệ value và scale.

Nếu nghiệp vụ chỉ quan tâm numerical equality, có thể:

```java
assertEquals(
        0,
        expected.compareTo(actual)
);
```

Hãy quyết định scale có phải contract không, đừng đổi assertion chỉ để test xanh.

Không tạo tiền bằng `new BigDecimal(19.99)` vì mang sai số `double`. Dùng String hoặc `BigDecimal.valueOf` khi đầu vào vốn là double.

---

## 18. Test double: fake, stub, mock, spy

Test double là object thay collaborator thật:

| Loại | Vai trò |
|---|---|
| Fake | Implementation đơn giản nhưng hoạt động, ví dụ repository in-memory |
| Stub | Trả dữ liệu lập trình sẵn để đưa code qua một nhánh |
| Mock | Cho phép verify interaction mong đợi |
| Spy | Bọc object thật và can thiệp/quan sát một phần |

Ví dụ Mockito:

```java
when(repository.findById("a1"))
        .thenReturn(Optional.of(agent));
verify(notificationService).send("user-01", "completed");
```

Spy dễ làm test gắn chặt implementation nên dùng hạn chế. Không mock value object như `BigDecimal`, DTO record hoặc collection chuẩn.

## 19. Khi nào mock?

Mock hợp lý cho network client, publisher, email sender, clock/random ID và repository trong unit test service. Không mock mọi lớp rồi chỉ verify method A gọi method B: test đó gắn chặt implementation và vỡ khi refactor dù behavior không đổi. Query repository cần integration test với database vì mock không chứng minh query đúng.

## 20. Có test private method không?

Thông thường không test private method trực tiếp; kiểm qua public behavior. Nếu private method quá phức tạp và khó test qua public API, đó có thể là dấu hiệu cần tách thành class có trách nhiệm rõ, không phải lý do dùng reflection.

## 21. Flaky test

Flaky test lúc pass lúc fail mà code không đổi. Nguyên nhân:

- Race condition/timing.
- Clock/timezone.
- Random không seed.
- Shared database state.
- Network thật.
- Phụ thuộc thứ tự test.
- Assert collection không có ordering contract.

Cách giảm:

- Inject `Clock`.
- Dùng test data riêng và cleanup/rollback.
- Chờ condition thay vì `Thread.sleep` cố định.
- Không gọi production external API.
- Sort hoặc assert không phụ thuộc order khi order không phải contract.
- Cô lập mutable static state.

Không “sửa” flaky test bằng retry vô hạn. Retry có thể che bug concurrency thật.

## 22. Test pyramid và Spring

Không cần `@SpringBootTest` cho `PromotionService`; chỉ `new PromotionService()`. Dùng `@WebMvcTest` cho web slice, `@DataJpaTest` cho JPA và `@SpringBootTest` khi thật sự cần wiring rộng. Chọn scope nhỏ nhất chứng minh behavior.

## 23. Maven chạy test như thế nào?

```powershell
.\mvnw.cmd test
```

Maven compile main/test source rồi Surefire chạy test; report nằm trong `target/surefire-reports`. Chạy riêng class bằng `.\mvnw.cmd -Dtest=PromotionServiceTest test`. Có thể dùng `-Dtest=Class#method` và nên quote argument chứa `#` trong PowerShell. Không dùng skip flag như cách “sửa” build đỏ.

---

## 24. Liên hệ SoftAIBox

### Promotion/payment

Test 0%, 25%, 100%, invalid -1%/101%, giá null/âm/0 và rounding ở nửa cent. Backend phải lookup promotion thay vì tin discount từ frontend.

### Conversation ownership

Test owner đúng được đọc, user khác bị từ chối và repository query bằng `id + owner`.

### Agent execution

```text
PENDING -> RUNNING -> SUCCEEDED
PENDING -> RUNNING -> FAILED
```

Không dùng sleep thật; tách executor/model client hoặc dùng test scheduler.

### Auth

Unit test token policy, integration test SecurityFilterChain, E2E test login → endpoint. UI route guard không phải security boundary.

### External provider

Unit test bằng fake/mock; integration test adapter với test server/sandbox, không gọi production API.

## 25. Lỗi thường gặp

- Chạy theo coverage thay vì rủi ro.
- Chỉ test happy path.
- Test nhiều behavior không liên quan trong một method.
- Assertion quá yếu, ví dụ chỉ `assertNotNull`.
- Assert chi tiết implementation.
- Dùng `@SpringBootTest` cho pure function.
- Mock mọi object.
- Test private method bằng reflection.
- So sánh `BigDecimal` mà không hiểu scale.
- Phụ thuộc thời gian, network hoặc order không bảo đảm.
- Nuốt exception trong test.
- Dùng production database.
- Bỏ qua test đỏ bằng skip flag.
- Không đọc nguyên nhân gốc trong report/stack trace.

## 26. Bài thực hành

1. Viết parameterized test cho 0%, 25%, 100%.
2. Test invalid `-1`, `101`.
3. Test `null`, giá âm và giá 0.
4. Tìm input có chữ số thứ ba là 5 để kiểm `HALF_UP`.
5. Kiểm scale result là 2 nếu đó là contract.
6. Viết fake `AgentRepository` cho service.
7. Viết Mockito test notification được gửi đúng một lần.
8. Viết một test không verify internal helper.
9. Cố tạo flaky test bằng clock thật, sau đó sửa bằng `Clock.fixed`.
10. Chạy `clean test`, mở `target/surefire-reports` và giải thích report.
11. Chạy riêng class và riêng method.
12. Vẽ test strategy unit/integration/E2E cho flow checkout.

## 27. Câu hỏi phỏng vấn và đáp án ngắn

### Maven phase và goal khác nhau?

Phase là bước trong lifecycle; goal là task cụ thể của plugin và có thể được bind vào phase.

### `test`, `package`, `verify`, `install` khác nhau?

`test` chạy unit test; `package` tạo artifact; `verify` chạy kiểm tra sau package; `install` đưa artifact vào local repository.

### Scope `test` có vào production artifact không?

Không; nó chỉ có trên test classpath.

### Vì sao commit Maven Wrapper?

Để dev và CI dùng Maven version thống nhất và giảm phụ thuộc cài đặt global.

### Unit test khác integration test?

Unit test cô lập logic nhỏ và nhanh; integration test kiểm nhiều thành phần thật phối hợp như repository/database hoặc Spring wiring.

### Arrange–Act–Assert là gì?

Chuẩn bị input/dependency, thực thi behavior, rồi kiểm kết quả quan sát được.

### Có nên test private method?

Thường không; test qua public behavior. Private logic quá lớn có thể cần tách class.

### Fake, stub, mock, spy khác gì?

Fake là implementation đơn giản; stub trả dữ liệu định sẵn; mock kiểm interaction; spy bọc object thật và can thiệp một phần.

### Khi nào mock?

Tại collaborator/boundary khó kiểm soát; không mock value object và không mock query để thay thế integration test.

### Vì sao `BigDecimal("10.0")` không equals `BigDecimal("10.00")`?

`equals` xét cả value và scale; `compareTo` chỉ xét numerical value.

### Coverage cao có đủ không?

Không. Coverage không chứng minh assertion đúng, boundary đủ hoặc behavior quan trọng được kiểm.

---

## 28. Cách chạy

Chạy riêng test ngày 9:

```powershell
.\mvnw.cmd -Dtest=PromotionServiceTest test
```

Chạy sạch toàn bộ suite:

```powershell
.\mvnw.cmd clean test
```

Biên dịch/đóng gói:

```powershell
.\mvnw.cmd package
```

Đọc report:

```powershell
Get-ChildItem .\target\surefire-reports
```

Khi test fail:

1. Đọc test và assertion bị fail.
2. So sánh expected/actual.
3. Tìm `Caused by` gốc nếu có exception bọc.
4. Chạy riêng test để rút ngắn vòng lặp.
5. Debug production code, không sửa expected chỉ để xanh.
6. Chạy lại toàn suite sau khi sửa.

## Definition of done

Bạn hoàn thành ngày 9 khi có thể:

- Đọc tọa độ, property, dependency, scope và plugin trong POM.
- Giải thích lifecycle/phase/goal.
- Dùng Maven Wrapper chạy `clean test`, `package`, `verify`.
- Phân biệt unit, integration và E2E.
- Viết test theo AAA với tên mô tả behavior.
- Test happy path, boundary và exception.
- Viết parameterized test.
- Giải thích fake, stub, mock, spy và tránh over-mocking.
- So sánh `BigDecimal` đúng theo contract scale.
- Nhận diện flaky test.
- Đọc Surefire report và stack trace.
- Tự mở rộng `PromotionServiceTest`.
- Lập test strategy cho promotion, auth và agent execution của SoftAIBox.
