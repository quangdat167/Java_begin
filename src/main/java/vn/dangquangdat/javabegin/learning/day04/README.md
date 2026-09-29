# Ngày 4 — Collections, generics và độ phức tạp

Ngày 4 học cách lưu, tìm, sắp xếp và nhóm nhiều object. Đây là nền tảng của hầu hết
backend: danh sách conversation, tập role, bảng tra user theo ID và kết quả phân trang.

Code thực hành:

- [CollectionsDemo.java](./CollectionsDemo.java): generic store dùng `HashMap`,
  sắp xếp conversation và hợp nhất tag bằng `LinkedHashSet`.

## Mục tiêu

Sau ngày này, bạn cần:

1. Chọn đúng `List`, `Set` hoặc `Map` theo ý nghĩa dữ liệu.
2. Biết khác biệt chính giữa các implementation phổ biến.
3. Hiểu `equals/hashCode` quyết định hành vi của hash collection.
4. Dùng generic để có type safety ở compile time.
5. Hiểu invariance, wildcard và quy tắc PECS.
6. Biết giới hạn cơ bản do type erasure.
7. Sắp xếp bằng `Comparable` hoặc `Comparator`.
8. Không để lộ collection mutable nội bộ.
9. Ước lượng Big-O của operation thường gặp.

---

## 1. Chạy code mẫu

```powershell
java .\src\main\java\vn\dangquangdat\javabegin\learning\day04\CollectionsDemo.java
```

Kết quả có dạng:

```text
Sorted conversations: [Conversation[id=2, title=Java interview, ...],
                       Conversation[id=1, title=Spring Security, ...]]
Unique tags: [...]
```

Thứ tự conversation ổn định vì code gọi `sort` theo title. Thứ tự tag bên trong
`Set.of(...)` không được contract đảm bảo, vì vậy thứ tự cuối của `uniqueTags` không
nên được dùng làm business rule.

Luồng dữ liệu:

```text
Conversation ──save──> HashMap<K,V>
                           │ values()
                           ▼
                    immutable snapshot
                           │ copy vào ArrayList
                           ▼
                    sort theo title
                           │
                           ▼
                    LinkedHashSet tags
```

---

## 2. Collection Framework là gì?

Collection Framework gồm interface, implementation và thuật toán để làm việc với
nhóm object. Ba abstraction quan trọng:

| Abstraction | Câu hỏi nó trả lời |
|---|---|
| `List<E>` | Các phần tử theo thứ tự nào? |
| `Set<E>` | Những phần tử duy nhất nào tồn tại? |
| `Map<K,V>` | Với key này, value là gì? |

`Map` không extend `Collection` vì nó biểu diễn cặp key-value, nhưng vẫn thuộc Java
Collections Framework.

Khai báo biến theo interface:

```java
List<String> names = new ArrayList<>();
Set<String> roles = new HashSet<>();
Map<Long, User> usersById = new HashMap<>();
```

Caller phụ thuộc capability cần dùng, không phụ thuộc quá chặt vào implementation.

---

## 3. `List` — có thứ tự và cho phép trùng

```java
List<String> models = new ArrayList<>();
models.add("gpt");
models.add("claude");
models.add("gpt");

System.out.println(models.get(0)); // gpt
System.out.println(models.size()); // 3
```

### `ArrayList`

`ArrayList` dùng array có khả năng tăng kích thước:

- Đọc/ghi theo index: `O(1)`.
- Thêm cuối: amortized `O(1)`.
- Chèn/xóa ở đầu hoặc giữa: `O(n)` vì phải dịch phần tử.
- Tìm theo giá trị: `O(n)`.

“Amortized” nghĩa là đa số lần thêm cuối rẻ, đôi lúc phải cấp array lớn hơn và copy;
chi phí trung bình trên nhiều lần vẫn là hằng số.

### `LinkedList`

`LinkedList` dùng node nối nhau. Truy cập index là `O(n)`. Dù thêm/xóa ở node đã biết
có thể rẻ, trong business code `ArrayList` thường là lựa chọn mặc định tốt hơn nhờ
cache locality và ít overhead.

### List không sửa được

```java
List<String> plans = List.of("FREE", "PRO");
// plans.add("ENTERPRISE"); // UnsupportedOperationException
```

`List.of` không nhận `null` và không cho mutation. `List.copyOf` tạo snapshot không
sửa được từ collection nguồn.

---

## 4. `Set` — phần tử duy nhất

```java
Set<String> tags = new HashSet<>();
tags.add("java");
tags.add("java");
System.out.println(tags.size()); // 1
```

Uniqueness được xác định qua equality của phần tử.

| Implementation | Thứ tự | Chi phí thường gặp |
|---|---|---|
| `HashSet` | Không đảm bảo | add/contains trung bình `O(1)` |
| `LinkedHashSet` | Giữ thứ tự chèn | trung bình `O(1)`, thêm overhead |
| `TreeSet` | Sắp theo natural order/comparator | `O(log n)` |

Code mẫu dùng `LinkedHashSet` để bỏ tag trùng đồng thời giữ thứ tự tag được gặp:

```java
Set<String> uniqueTags = new LinkedHashSet<>();
sorted.forEach(
        conversation -> uniqueTags.addAll(conversation.tags())
);
```

Lưu ý: nó giữ thứ tự **được thêm vào**, không tự sắp alphabet.

---

## 5. `Map` — tra value theo key

```java
Map<Long, String> titlesById = new HashMap<>();
titlesById.put(1L, "Java");
titlesById.put(2L, "Spring");

String title = titlesById.get(1L);
```

`put` với key đã tồn tại thay value cũ và trả value cũ.

Các method hay dùng:

```java
map.containsKey(key);
map.getOrDefault(key, defaultValue);
map.putIfAbsent(key, value);
map.computeIfAbsent(key, ignored -> new ArrayList<>());
map.merge(word, 1, Integer::sum);
```

| Implementation | Đặc điểm |
|---|---|
| `HashMap` | Tra cứu trung bình `O(1)`, không đảm bảo thứ tự |
| `LinkedHashMap` | Giữ insertion order hoặc access order |
| `TreeMap` | Sắp key, operation `O(log n)` |
| `ConcurrentHashMap` | Hỗ trợ truy cập concurrent tốt hơn map thường |

`HashMap` cho phép một key `null`; nhiều collection factory như `Map.of` không cho
`null`. Không nên dựa vào `null` key trong domain nếu một type rõ ràng tốt hơn.

---

## 6. `equals`, `hashCode` và hash collection

`HashMap`/`HashSet` dùng `hashCode` để chọn vùng tìm kiếm, sau đó dùng `equals` để xác
nhận key/phần tử.

```text
key
 │ hashCode()
 ▼
bucket
 │ equals() với candidate
 ▼
đúng entry
```

Contract bắt buộc:

> Nếu `a.equals(b)` là true thì `a.hashCode() == b.hashCode()` phải true.

Hai object khác nhau vẫn có thể cùng hash code; đó là collision. Collection vẫn phải
dùng `equals` để phân biệt.

### Key mutable nguy hiểm

Nếu field tham gia `equals/hashCode` bị đổi sau khi object làm key, map có thể tìm ở
bucket mới trong khi entry nằm tại bucket cũ:

```java
Map<MutableKey, String> map = new HashMap<>();
map.put(key, "value");
key.setId(999);
// map.get(key) có thể không tìm thấy
```

Ưu tiên key immutable như `String`, `Long`, UUID hoặc value object bất biến.

Record tự sinh `equals/hashCode` từ components nên phù hợp làm value key nếu tất cả
component có equality ổn định.

---

## 7. Generics và type safety

Code mẫu:

```java
static final class ConversationStore<K, V> {
    private final Map<K, V> data = new HashMap<>();

    void save(K id, V value) {
        data.put(id, value);
    }

    V find(K id) {
        return data.get(id);
    }
}
```

`K` và `V` là type parameter. Khi khởi tạo:

```java
ConversationStore<Long, Conversation> store =
        new ConversationStore<>();
```

Compiler thay cách hiểu:

```text
K → Long
V → Conversation
save(Long, Conversation)
find(Long) → Conversation
```

Nhờ đó, lỗi sau bị chặn lúc compile:

```java
// store.save("wrong-id", conversation);
// User user = store.find(1L);
```

Không dùng raw type:

```java
ConversationStore raw = new ConversationStore(); // mất type safety
```

Raw type tồn tại chủ yếu để tương thích code Java cũ.

---

## 8. Generic invariance và wildcard

`List<Dog>` không phải subtype của `List<Animal>`:

```java
// List<Animal> animals = new ArrayList<Dog>(); // không compile
```

Nếu được phép, caller có thể thêm `Cat` vào list vốn chỉ chấp nhận `Dog`.

### Producer Extends

Method chỉ đọc phần tử như `Animal`:

```java
static void printNames(List<? extends Animal> animals) {
    for (Animal animal : animals) {
        System.out.println(animal.name());
    }
}
```

Có thể truyền `List<Dog>`. Không thể thêm một `Dog` tùy ý vì compiler không biết list
thật là `List<Dog>` hay `List<Cat>`.

### Consumer Super

Method cần thêm `Dog`:

```java
static void addDog(List<? super Dog> destination) {
    destination.add(new Dog("Milo"));
}
```

Có thể truyền `List<Dog>`, `List<Animal>` hoặc `List<Object>`.

Quy tắc nhớ: **PECS — Producer Extends, Consumer Super**. Nếu vừa đọc vừa ghi chính xác
một type, thường dùng `List<T>`.

---

## 9. Type erasure

Phần lớn generic type information bị erase khi compile để tương thích JVM:

```text
List<String>  ─┐
               ├─ runtime chủ yếu thấy List
List<Integer> ─┘
```

Hệ quả:

```java
// new T();                         // không hợp lệ
// new T[10];                       // không hợp lệ
// value instanceof List<String>;   // không hợp lệ
```

Không thể overload chỉ bằng generic argument:

```java
// void process(List<String> values)
// void process(List<Integer> values)
```

Sau erasure, hai signature xung đột.

Khi cần tạo `T`, có thể truyền factory:

```java
static <T> T create(Supplier<T> factory) {
    return factory.get();
}
```

---

## 10. Sắp xếp với `Comparable` và `Comparator`

Code mẫu:

```java
sorted.sort(Comparator.comparing(Conversation::title));
```

`Conversation::title` là method reference tương đương:

```java
conversation -> conversation.title()
```

### Comparable

Class tự định nghĩa natural order:

```java
final class Score implements Comparable<Score> {
    @Override
    public int compareTo(Score other) {
        return Integer.compare(value, other.value);
    }
}
```

### Comparator

Định nghĩa thứ tự bên ngoài và có thể có nhiều cách sắp:

```java
Comparator<Conversation> byTitle =
        Comparator.comparing(Conversation::title);

Comparator<Conversation> byTitleThenId =
        Comparator.comparing(Conversation::title)
                .thenComparingLong(Conversation::id);
```

Dùng `reversed()` cho thứ tự ngược và `nullsFirst/nullsLast` khi contract thật sự cho
phép null. Comparator nên nhất quán với equality nếu dùng trong `TreeSet/TreeMap`,
nếu không hai object khác nhau có thể bị xem như cùng key theo thứ tự.

---

## 11. Defensive copy và collection view

Constructor record trong code:

```java
record Conversation(long id, String title, Set<String> tags) {
    Conversation {
        tags = Set.copyOf(tags);
    }
}
```

Nếu caller sửa set ban đầu, state của conversation không đổi:

```text
caller mutable set ──copy──> immutable set trong Conversation
```

Method store:

```java
List<V> values() {
    return List.copyOf(data.values());
}
```

`data.values()` là view gắn với map. `List.copyOf` tạo snapshot và không cho caller
thêm/xóa. Đây là bảo vệ encapsulation của store.

Defensive copy là shallow: nếu `V` mutable, caller vẫn có thể sửa từng object. Muốn
deep immutability phải thiết kế cả object graph.

---

## 12. Độ phức tạp cơ bản

| Operation | Cấu trúc | Big-O thường gặp |
|---|---|---|
| `get(index)` | ArrayList | `O(1)` |
| tìm value | List | `O(n)` |
| `contains/add` | HashSet | trung bình `O(1)` |
| `get/put` | HashMap | trung bình `O(1)` |
| `get/put` | TreeMap | `O(log n)` |
| sort | List | `O(n log n)` |
| duyệt | mọi collection | `O(n)` |

Big-O không nói toàn bộ hiệu năng. Hashing, allocation, locality, concurrency và kích
thước dữ liệu đều quan trọng. Với collection nhỏ, code đơn giản thường quan trọng hơn
tối ưu vi mô.

Ví dụ: tìm email của 1 triệu user bằng List có thể phải so sánh gần 1 triệu lần; map
được xây theo email có lookup trung bình gần hằng số nhưng tốn thêm memory và chi phí
duy trì index.

---

## 13. Liên hệ với `CollectionsDemo` và SoftAIBox

### Generic store

`ConversationStore<Long, Conversation>` minh họa repository in-memory. Production có
thể thay bằng JPA repository nhưng ý tưởng key-value và contract typed vẫn còn.

### Sắp xếp

Code copy values sang `ArrayList` vì snapshot trả về không sửa được, sau đó sort bản
copy. Không làm lộ state map nội bộ.

### Unique tags

`LinkedHashSet` loại trùng tag. Trong API thật cần quyết định:

- Tag có phân biệt hoa thường không?
- Có trim khoảng trắng không?
- Output cần insertion order hay alphabet?
- Có giới hạn số tag không?

### Các mapping điển hình

```text
List<Conversation>       → kết quả trang hiện tại
Set<Role>                → quyền duy nhất của user
Map<UUID, Execution>     → execution theo ID
Map<String, Integer>     → tần suất token/tag
LinkedHashMap            → cache giữ thứ tự access
```

---

## 14. Những lỗi thường gặp

### Chọn collection theo thói quen

Dùng List rồi gọi `contains` liên tục có thể biến thuật toán thành `O(n²)`. Nếu câu
hỏi là membership, Set có thể đúng hơn.

### Dựa vào thứ tự HashMap/HashSet

Contract không đảm bảo thứ tự. Dùng LinkedHash hoặc Tree implementation khi thứ tự là
yêu cầu.

### Sửa collection trong enhanced for

```java
for (String value : values) {
    values.remove(value); // thường ConcurrentModificationException
}
```

Dùng `removeIf` hoặc iterator đúng cách.

### Dùng key mutable

Thay field tham gia hash sau khi `put` khiến lookup hỏng.

### Trả collection nội bộ

Caller có thể sửa state của object. Trả snapshot/view phù hợp hoặc API thao tác có
kiểm soát.

### Dùng raw type

Lỗi kiểu bị đẩy từ compile time sang `ClassCastException` runtime.

### Hiểu sai `List.copyOf`

Nó tạo shallow immutable snapshot, không clone sâu các phần tử.

---

## 15. Bài thực hành

### Cấp 1

1. Đếm tần suất từ trong prompt bằng `Map<String,Integer>` và `merge`.
2. Loại tag trùng nhưng giữ thứ tự xuất hiện bằng `LinkedHashSet`.
3. Sort conversation theo title rồi theo id.
4. Tìm conversation theo id bằng List và Map; ghi Big-O.

### Cấp 2

Mở rộng `ConversationStore<K,V>`:

```java
Optional<V> findOptional(K id)
boolean exists(K id)
V remove(K id)
int size()
```

Quyết định rõ behavior khi key hoặc value là null.

### Cấp 3

Viết method generic copy:

```java
static <T> void copy(
        List<? extends T> source,
        List<? super T> destination
)
```

Giải thích vì sao source dùng extends và destination dùng super.

### Cấp 4

Viết LRU cache nhỏ bằng `LinkedHashMap` ở access-order. Giới hạn 3 phần tử và chứng
minh entry ít dùng nhất bị loại.

### Cấp 5

Thiết kế index in-memory:

```text
conversationById
conversationIdsByOwnerId
conversationIdsByTag
```

Phân tích consistency khi thêm, sửa tag hoặc xóa conversation.

---

## 16. Câu hỏi phỏng vấn và đáp án ngắn

### List, Set và Map khác nhau thế nào?

List giữ thứ tự và cho phép trùng; Set biểu diễn phần tử duy nhất; Map ánh xạ key sang
value.

### HashMap hoạt động khái quát ra sao?

Nó dùng hash code để chọn bucket và equals để tìm đúng key trong các candidate. Lookup
trung bình `O(1)` khi hash phân bố tốt.

### Collision là gì?

Là khi nhiều key rơi vào cùng bucket/hash. HashMap vẫn phân biệt bằng equality.

### Vì sao key mutable nguy hiểm?

Nếu hash/equality đổi sau khi put, entry nằm ở vị trí cũ nhưng lookup tính vị trí mới.

### ArrayList và LinkedList chọn cái nào?

ArrayList thường là mặc định nhờ truy cập index nhanh, locality tốt và ít overhead.
LinkedList chỉ phù hợp với một số pattern thao tác node cụ thể.

### Generic đem lại gì?

Type safety và API tái sử dụng ở compile time, giảm cast và lỗi runtime.

### PECS là gì?

Producer Extends, Consumer Super: đọc T từ `? extends T`, ghi T vào `? super T`.

### Type erasure là gì?

Phần lớn type argument generic bị xóa khi compile, nên runtime không phân biệt đầy đủ
`List<String>` với `List<Integer>`.

### `List.of` khác `new ArrayList`?

`List.of` tạo list không sửa được và không nhận null; ArrayList mutable.

## Definition of done

Bạn hoàn thành Ngày 4 khi có thể:

- Chọn collection dựa trên semantics và complexity.
- Giải thích output và mọi bước trong `CollectionsDemo`.
- Viết generic class/method không dùng raw type.
- Giải thích invariance và dùng được PECS.
- Nêu hạn chế chính của type erasure.
- Sort bằng Comparator nhiều tiêu chí.
- Giải thích `equals/hashCode` và rủi ro key mutable.
- Dùng defensive copy để bảo vệ collection nội bộ.
- Hoàn thành bài đếm tần suất, generic copy và index in-memory.
