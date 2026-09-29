# Ngày 10 — SQL, transaction, JPA và Hibernate

Ngày 10 là điểm chuyển từ Java core sang dữ liệu bền vững.
Mục tiêu không phải học thuộc annotation,
mà là hiểu một request thay đổi dữ liệu theo những bước nào,
transaction bảo vệ điều gì,
và Hibernate sinh SQL ra sao.

Code production dùng trong ngày này:

- [Conversation.java](../../conversation/Conversation.java):
  entity hội thoại, timestamp và optimistic locking.
- [ConversationRepository.java](../../conversation/ConversationRepository.java):
  Spring Data repository, truy vấn theo chủ sở hữu và phân trang.
- [ConversationService.java](../../conversation/ConversationService.java):
  ranh giới transaction và dirty checking.
- [ChatTurn.java](../../chat/ChatTurn.java):
  entity lưu một lượt hỏi–đáp.
- [ChatTurnRepository.java](../../chat/ChatTurnRepository.java):
  truy vấn lịch sử chat có thứ tự.
- [ChatService.java](../../chat/ChatService.java):
  transaction cập nhật hội thoại và tạo lượt chat.

> Hãy luôn hỏi ba câu:
> dữ liệu nằm ở bảng nào,
> transaction bắt đầu và kết thúc ở đâu,
> nếu một câu SQL thất bại thì trạng thái nào phải rollback?

---

## Mục tiêu của ngày 10

Sau khi học xong, bạn cần tự giải thích được:

1. Database quan hệ, bảng, hàng, cột, khóa chính và khóa ngoại là gì.
2. Constraint khác validation trong Java như thế nào.
3. Index giúp gì và phải trả giá gì.
4. Bốn thuộc tính ACID của transaction.
5. Dirty read, non-repeatable read, phantom read và lost update.
6. JPA khác Hibernate và Spring Data JPA ở đâu.
7. Bốn trạng thái chính của JPA entity.
8. Persistence context và dirty checking hoạt động thế nào.
9. <code>save()</code>, <code>flush()</code> và commit khác nhau ra sao.
10. <code>@Transactional</code> nên đặt ở lớp nào.
11. Vì sao lazy loading có thể gây lỗi hoặc N+1.
12. <code>@Version</code> phát hiện lost update như thế nào.
13. Luồng gửi chat trong repository này được commit nguyên tử ra sao.
14. Những phần nào của H2 demo chưa phù hợp production.

---

## 1. Từ object Java đến bảng SQL

Trong Java, ta làm việc với object:

~~~java
Conversation conversation =
        new Conversation("Java interview", "dat@softaibox.local");
~~~

Trong database quan hệ, dữ liệu được tổ chức thành bảng:

~~~text
chat_conversations
┌────┬────────────────┬──────────────────────┬────────────┐
│ id │ title          │ owner_email          │ turn_count │
├────┼────────────────┼──────────────────────┼────────────┤
│  1 │ Java interview │ dat@softaibox.local  │          2 │
└────┴────────────────┴──────────────────────┴────────────┘
~~~

Một cách ánh xạ đơn giản:

| Java/JPA | Database quan hệ |
|---|---|
| Class có <code>@Entity</code> | Bảng |
| Một entity instance | Một hàng |
| Field persistent | Một cột |
| <code>@Id</code> | Khóa chính |
| Quan hệ giữa entity | Khóa ngoại/join table |
| Repository query | SQL được ORM tạo |

ORM là Object–Relational Mapping.
Nó giảm code chuyển đổi giữa object và hàng dữ liệu,
nhưng không loại bỏ nhu cầu hiểu SQL.

Nếu query chậm,
việc thêm annotation ngẫu nhiên thường không giải quyết được.
Ta vẫn phải hiểu:

- SQL nào thực sự được gửi.
- Query đọc bao nhiêu hàng.
- Index nào được dùng.
- Có phát sinh N+1 hay không.
- Transaction giữ lock bao lâu.

---

## 2. SQL nền tảng phải biết

### 2.1 SELECT và WHERE

Lấy hội thoại của một user:

~~~sql
SELECT id, title, turn_count, created_at, updated_at
FROM chat_conversations
WHERE owner_email = 'dat@softaibox.local'
ORDER BY updated_at DESC;
~~~

<code>WHERE owner_email = ...</code> không chỉ là filter nghiệp vụ.
Trong ứng dụng nhiều người dùng,
đây còn là một phần của kiểm soát quyền sở hữu dữ liệu.

Repository hiện tại diễn đạt rule đó ngay trong tên method:

~~~java
Optional<Conversation> findByIdAndOwnerEmail(
        Long id,
        String ownerEmail
);
~~~

So với tìm theo ID rồi kiểm tra owner ở Java,
query theo cả hai điều kiện:

- Không tải dữ liệu không thuộc user.
- Giảm nguy cơ quên kiểm tra ownership.
- Trả cùng một kết quả “không tìm thấy” cho ID không tồn tại
  và ID thuộc người khác.

### 2.2 INSERT

Tạo conversation về mặt SQL có thể giống:

~~~sql
INSERT INTO chat_conversations (
    title,
    owner_email,
    turn_count,
    created_at,
    updated_at,
    version
) VALUES (
    'Java interview',
    'dat@softaibox.local',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);
~~~

Trong code,
Hibernate sinh câu lệnh từ entity được truyền vào repository:

~~~java
repository.save(new Conversation(title.trim(), ownerEmail));
~~~

### 2.3 UPDATE

Đổi tên hội thoại có thể tạo SQL:

~~~sql
UPDATE chat_conversations
SET title = ?,
    updated_at = ?,
    version = version + 1
WHERE id = ?
  AND version = ?;
~~~

Điều kiện version là phần quan trọng của optimistic locking.
Nếu không còn hàng nào khớp version cũ,
Hibernate biết một transaction khác đã cập nhật trước.

### 2.4 DELETE

~~~sql
DELETE FROM chat_conversations
WHERE id = ?;
~~~

Code hiện tại dùng hard delete:

~~~java
repository.delete(requireOwned(id, ownerEmail));
~~~

Response DTO vẫn có field <code>deletedAt</code>,
nhưng hiện luôn là <code>null</code>.
Muốn soft delete thật,
cần thiết kế trạng thái/cột, query mặc định và index tương ứng;
chỉ thêm một field vào DTO là chưa đủ.

### 2.5 JOIN

Hiện tại <code>ChatTurn</code> giữ <code>conversationId</code> dạng <code>Long</code>,
không khai báo <code>@ManyToOne</code>.
SQL join vẫn có thể viết:

~~~sql
SELECT c.id,
       c.title,
       t.id AS turn_id,
       t.message,
       t.response
FROM chat_conversations c
JOIN chat_turns t
  ON t.conversation_id = c.id
WHERE c.owner_email = ?
ORDER BY c.updated_at DESC, t.created_at ASC;
~~~

Không có JPA relation không có nghĩa database không có quan hệ logic.
Trong production,
nên có foreign key để database ngăn turn trỏ tới conversation không tồn tại.

### 2.6 GROUP BY và HAVING

Bài toán:
lấy 10 conversation mới cập nhật nhất của user
có ít nhất ba turns.

~~~sql
SELECT c.id,
       c.title,
       c.updated_at,
       COUNT(t.id) AS number_of_turns
FROM chat_conversations c
JOIN chat_turns t
  ON t.conversation_id = c.id
WHERE c.owner_email = :ownerEmail
GROUP BY c.id, c.title, c.updated_at
HAVING COUNT(t.id) >= 3
ORDER BY c.updated_at DESC
LIMIT 10;
~~~

<code>WHERE</code> lọc hàng trước khi group.
<code>HAVING</code> lọc kết quả sau khi group.

---

## 3. Constraint — lớp bảo vệ cuối cùng

Validation trong Java tạo lỗi thân thiện sớm.
Constraint trong database bảo vệ dữ liệu
kể cả khi có bug, script hoặc service khác ghi trực tiếp.

Các constraint phổ biến:

| Constraint | Ý nghĩa |
|---|---|
| PRIMARY KEY | Mỗi hàng có định danh duy nhất |
| NOT NULL | Cột bắt buộc có giá trị |
| UNIQUE | Không cho trùng giá trị hoặc nhóm giá trị |
| FOREIGN KEY | Giá trị phải tham chiếu hàng hợp lệ |
| CHECK | Giá trị phải thỏa biểu thức |

Entity hiện có:

~~~java
@Column(nullable = false, length = 120)
private String title;
~~~

<code>nullable = false</code> giúp Hibernate sinh schema demo,
nhưng production nên quản lý schema bằng migration
như Flyway hoặc Liquibase.

### Validation và constraint không thay thế nhau

Ví dụ tên agent phải duy nhất theo owner:

1. Service kiểm tra trước để trả message đẹp.
2. Database vẫn cần unique constraint trên
   <code>(owner_email, normalized_name)</code>.
3. Khi hai request đồng thời cùng vượt qua bước kiểm tra,
   chỉ constraint mới phân xử chắc chắn.
4. Backend bắt đúng exception và trả <code>409 Conflict</code>.

Đây là mô hình:

~~~text
Validation sớm → UX tốt
Database constraint → tính đúng cuối cùng
~~~

---

## 4. Index là gì?

Không có index phù hợp,
database có thể phải quét nhiều hàng để tìm dữ liệu.
Index tạo cấu trúc phụ giúp tra cứu nhanh hơn.

Query quan trọng trong repository:

~~~text
WHERE owner_email = ?
  AND lower(title) LIKE ?
ORDER BY updated_at DESC
~~~

Một index composite có thể bắt đầu từ:

~~~sql
CREATE INDEX idx_conversation_owner_updated
ON chat_conversations(owner_email, updated_at DESC);
~~~

Index này hữu ích cho:

- Lọc theo owner.
- Sắp xếp lịch sử mới nhất của owner.

Nó không tự giải quyết tốt tìm kiếm:

~~~sql
LOWER(title) LIKE '%java%'
~~~

Wildcard ở đầu thường làm B-tree index thông thường khó được dùng.
Với dữ liệu lớn,
có thể cần functional index,
trigram/full-text index
hoặc search engine tùy database và yêu cầu.

### Cái giá của index

Mỗi index:

- Tốn dung lượng.
- Làm INSERT/UPDATE/DELETE tốn thêm công việc.
- Tăng thời gian migration.
- Có thể không được optimizer dùng.

Không thêm index chỉ vì một cột “có vẻ quan trọng”.
Hãy dựa trên query thật và <code>EXPLAIN</code>/<code>EXPLAIN ANALYZE</code>.

### Thứ tự cột trong composite index

Index:

~~~text
(owner_email, updated_at)
~~~

thường hữu ích cho query theo:

- <code>owner_email</code>.
- <code>owner_email + updated_at</code>.

Nó thường không tối ưu query chỉ theo <code>updated_at</code>.
Quy tắc “leftmost prefix” là mô hình khởi đầu tốt,
nhưng quyết định cuối cùng phải xem query plan của database cụ thể.

---

## 5. Transaction và ACID

Transaction gom nhiều thao tác thành một đơn vị công việc.

### 5.1 Atomicity

Hoặc tất cả thay đổi thành công,
hoặc tất cả rollback.

Khi gửi chat,
ứng dụng:

1. Tăng <code>turnCount</code> của conversation.
2. Lưu <code>ChatTurn</code>.

Nếu lưu turn thất bại,
turn count cũng không được tăng.

### 5.2 Consistency

Sau transaction,
dữ liệu phải thỏa constraint và invariant.

Ví dụ:

- Không có chat turn thiếu conversation.
- Turn count không âm.
- Owner không rỗng.
- Version tăng đúng khi cập nhật.

Transaction không tự phát minh invariant.
Ta phải mô tả chúng bằng domain logic và database constraint.

### 5.3 Isolation

Các transaction đồng thời được cách ly theo một mức nhất định.
Mức cách ly càng mạnh thường càng tốn lock,
giảm throughput hoặc tăng khả năng retry.

### 5.4 Durability

Sau khi commit thành công,
dữ liệu phải còn tồn tại kể cả khi process ứng dụng crash.

H2 in-memory trong bài học không cung cấp độ bền qua lần restart ứng dụng.
Nó phù hợp demo nhanh,
không phải database production.

---

## 6. Các hiện tượng concurrent transaction

### Dirty read

Transaction B đọc dữ liệu Transaction A chưa commit.
Nếu A rollback,
B đã đọc một trạng thái chưa từng tồn tại chính thức.

### Non-repeatable read

Trong cùng một transaction,
đọc cùng một hàng hai lần nhưng nhận hai giá trị khác nhau
vì transaction khác đã update và commit ở giữa.

### Phantom read

Chạy lại một query theo điều kiện
thấy thêm hoặc mất hàng do transaction khác insert/delete.

### Lost update

Hai request cùng đọc <code>turnCount = 10</code>:

~~~text
Request A đọc 10 → tính 11 ─┐
                            ├─ cùng ghi 11
Request B đọc 10 → tính 11 ─┘
~~~

Kết quả đúng phải là 12,
nhưng một update đã bị mất.

Isolation level không phải lúc nào cũng tự ngăn lost update
theo cách ứng dụng mong muốn.
Ta có thể dùng:

- Atomic SQL update.
- Optimistic locking.
- Pessimistic locking.
- Serialization ở queue.
- Thiết kế tránh shared mutable row.

---

## 7. Optimistic và pessimistic locking

### Optimistic locking

Optimistic locking giả định xung đột không xảy ra thường xuyên.
Entity có version:

~~~java
@Version
private long version;
~~~

Luồng:

1. A và B cùng đọc version 3.
2. A update với điều kiện version 3, thành công và version thành 4.
3. B update vẫn đòi version 3.
4. Không có hàng khớp.
5. Hibernate ném optimistic locking exception.

Ưu điểm:

- Không giữ database lock suốt thời gian user suy nghĩ.
- Tốt khi xung đột hiếm.

Nhược điểm:

- Caller phải quyết định retry hay báo conflict.
- Retry phải chạy lại toàn bộ logic trên dữ liệu mới.

### Pessimistic locking

Pessimistic locking khóa hàng khi đọc,
ví dụ <code>SELECT ... FOR UPDATE</code>.

Ưu điểm:

- Hữu ích khi xung đột rất có khả năng xảy ra.
- Bảo vệ critical section ở database.

Nhược điểm:

- Request khác phải chờ.
- Dễ deadlock nếu thứ tự khóa không nhất quán.
- Không nên giữ transaction mở trong lúc gọi API model chậm.

Repo hiện dùng optimistic locking cho <code>Conversation</code>.
Đây là lựa chọn hợp lý cho chỉnh sửa hội thoại thông thường.

---

## 8. JPA, Hibernate và Spring Data JPA

Ba khái niệm này liên quan nhưng không đồng nghĩa:

| Thành phần | Vai trò |
|---|---|
| JPA/Jakarta Persistence | Specification/API chuẩn cho ORM |
| Hibernate | Một implementation của JPA |
| Spring Data JPA | Abstraction tạo repository và query tiện lợi trên JPA |

Code import:

~~~java
import jakarta.persistence.Entity;
~~~

đang dùng API chuẩn JPA.

Interface:

~~~java
interface ConversationRepository
        extends JpaRepository<Conversation, Long> {
}
~~~

do Spring Data tạo implementation runtime.
Hibernate phía dưới quản lý entity và phát SQL.

### Không phải magic

Method:

~~~java
findByIdAndOwnerEmail(id, ownerEmail)
~~~

được Spring Data phân tích tên để tạo query.
Nếu tên method ngày càng dài hoặc query phức tạp,
có thể dùng:

- <code>@Query</code> với JPQL.
- Native SQL.
- Specification/Querydsl.
- Projection.
- Repository implementation tùy chỉnh.

Chọn công cụ đơn giản nhất vẫn diễn đạt rõ truy vấn.

---

## 9. Entity lifecycle

### Transient

Object mới,
chưa thuộc persistence context:

~~~java
Conversation conversation =
        new Conversation(title, ownerEmail);
~~~

### Managed

Entity đang được persistence context theo dõi.
Entity lấy từ repository trong transaction thường là managed.

~~~java
Conversation conversation = requireOwned(id, ownerEmail);
conversation.rename(title);
~~~

Không cần gọi <code>save()</code> sau <code>rename()</code>
vì dirty checking phát hiện thay đổi lúc flush.

### Detached

Entity từng managed nhưng persistence context đã đóng
hoặc entity bị detach.
Đổi field trên detached entity không tự tạo UPDATE.

### Removed

Entity đã được đánh dấu xóa.
DELETE thường xảy ra khi flush/commit.

### Vì sao constructor rỗng là protected?

JPA cần constructor không tham số để khởi tạo entity:

~~~java
protected Conversation() {
}
~~~

Để <code>protected</code> thay vì <code>public</code>
giảm khả năng business code tạo object không hợp lệ.

---

## 10. Persistence context và dirty checking

Persistence context giống một vùng làm việc cho entity managed.
Trong một context,
mỗi database identity thường tương ứng một managed instance.

Khi transaction bắt đầu:

1. Repository tải entity.
2. Hibernate giữ snapshot cần thiết.
3. Business method thay đổi entity.
4. Trước commit, Hibernate so sánh trạng thái.
5. Nếu dirty, Hibernate tạo UPDATE.
6. Transaction commit hoặc rollback.

Method rename trong repo:

~~~java
@Transactional
public ConversationView rename(
        long id,
        String title,
        String ownerEmail
) {
    Conversation conversation = requireOwned(id, ownerEmail);
    conversation.rename(title.trim());
    return ConversationView.from(conversation);
}
~~~

Không có <code>repository.save(conversation)</code>.
Đây là chủ đích,
không phải quên lưu.

### Flush không phải commit

Flush đồng bộ thay đổi trong persistence context xuống database.
Commit kết thúc transaction và xác nhận thay đổi.

~~~text
Thay đổi entity
      ↓
Persistence context
      ↓ flush
SQL gửi xuống database
      ↓ commit
Dữ liệu được xác nhận
~~~

Sau flush,
transaction vẫn có thể rollback.

### Khi nào flush xảy ra?

Thường xảy ra:

- Trước commit.
- Trước một số query cần dữ liệu đồng bộ.
- Khi gọi <code>flush()</code> rõ ràng.

Đừng gọi flush sau mọi save.
Việc đó làm mất lợi ích batching
và kéo lỗi database lên sớm hơn mà không phải lúc nào cũng cần.

---

## 11. <code>save()</code> khác <code>flush()</code> thế nào?

<code>save(entity)</code> đưa entity vào luồng persistence:

- Entity mới thường được persist.
- Entity detached thường được merge.
- SQL không nhất thiết chạy ngay tại dòng save.

<code>flush()</code> yêu cầu đồng bộ thay đổi pending thành SQL,
nhưng không commit transaction.

<code>saveAndFlush()</code> làm cả hai,
thường chỉ cần khi:

- Phải thấy constraint error ngay tại một điểm.
- Query/procedure sau đó phải nhìn dữ liệu vừa ghi.
- Test đang kiểm hành vi flush cụ thể.

Trong business flow thông thường,
để transaction commit tự flush thường rõ và hiệu quả hơn.

---

## 12. Ranh giới <code>@Transactional</code> trong repo

[ConversationService.java](../../conversation/ConversationService.java)
đặt:

~~~java
@Service
@Transactional(readOnly = true)
public class ConversationService {
}
~~~

Điều này tạo mặc định read-only cho method đọc.
Method ghi override:

~~~java
@Transactional
public ConversationView create(...) {
}
~~~

Read-only là hint và diễn đạt intent;
đừng coi nó là security boundary tuyệt đối cho mọi database.

[ChatService.java](../../chat/ChatService.java) có:

~~~java
@Transactional
public ChatView send(...) {
    Conversation conversation =
            conversations.requireForNewTurn(...);
    return ChatView.from(turns.save(new ChatTurn(...)));
}
~~~

Vì <code>ConversationService</code> là bean riêng,
lời gọi đi qua Spring proxy.
Với propagation mặc định <code>REQUIRED</code>,
method bên trong tham gia transaction đang có của <code>send</code>.

Kết quả:

~~~text
BEGIN
  SELECT conversation theo id + owner
  tăng turnCount trên managed entity
  INSERT chat_turn
  UPDATE conversation + version
COMMIT
~~~

Nếu INSERT turn vi phạm constraint,
runtime exception làm transaction rollback,
nên UPDATE conversation cũng không được commit.

### Không gọi API chậm trong transaction

Demo tạo câu trả lời ngay bằng nối String.
Trong production,
gọi LLM/HTTP có thể mất vài giây hoặc lâu hơn.

Không nên giữ transaction database mở trong lúc chờ mạng:

- Connection bị giữ lâu.
- Lock sống lâu.
- Khả năng timeout/deadlock tăng.

Thiết kế thường tách:

1. Transaction ngắn tạo job/pending turn.
2. Commit.
3. Worker gọi model.
4. Transaction ngắn ghi kết quả.

---

## 13. Query repository hiện tại

[ConversationRepository.java](../../conversation/ConversationRepository.java)
có:

~~~java
Page<Conversation>
findByOwnerEmailAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
        String ownerEmail,
        String keyword,
        Pageable pageable
);
~~~

Tên method mô tả:

- Lọc owner.
- Title chứa keyword.
- Không phân biệt hoa thường.
- Sắp xếp updatedAt giảm dần.
- Trả một Page.

Service chuẩn hóa input:

~~~java
int safePage = Math.max(1, page) - 1;
int safeLimit = Math.clamp(limit, 1, 100);
~~~

API dùng page bắt đầu từ 1,
còn Spring Data <code>PageRequest</code> bắt đầu từ 0.

Giới hạn 100 giúp tránh client yêu cầu hàng triệu bản ghi.

### Chi phí của Page

<code>Page</code> thường cần:

1. Query lấy nội dung trang.
2. Query count tổng số phần tử.

Khi count rất đắt và UI chỉ cần biết có trang tiếp hay không,
<code>Slice</code> có thể phù hợp hơn.

---

## 14. Quan hệ Conversation–ChatTurn trong thiết kế hiện tại

<code>ChatTurn</code> lưu:

~~~java
private Long conversationId;
private String ownerEmail;
~~~

Không có:

~~~java
@ManyToOne
private Conversation conversation;
~~~

### Ưu điểm của ID trực tiếp

- Entity đơn giản.
- Không vô tình lazy-load graph.
- Serialization không tạo vòng lặp.
- Query ownership rõ ràng.

### Nhược điểm

- JPA không hiểu relation.
- Không dùng fetch join/entity navigation trực tiếp.
- Nếu schema thiếu foreign key,
  có thể tạo orphan turn.
- Logic đồng bộ owner bị lặp.

Đây là trade-off,
không phải mọi relation đều bắt buộc ánh xạ bằng annotation.

Nếu thêm <code>@ManyToOne</code>,
hãy mặc định cân nhắc <code>fetch = LAZY</code>
và vẫn dùng DTO/projection ở API boundary.

---

## 15. Lazy loading và lỗi ngoài transaction

Lazy relation thường là proxy hoặc collection chưa tải.

Ví dụ giả định:

~~~java
@OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY)
private List<ChatTurn> turns;
~~~

Nếu controller gọi <code>conversation.getTurns()</code>
sau khi transaction/persistence context đã đóng,
Hibernate không còn context để tải dữ liệu
và có thể ném <code>LazyInitializationException</code>.

Repo đặt:

~~~yaml
spring:
  jpa:
    open-in-view: false
~~~

Đây là lựa chọn tốt để buộc data access diễn ra trong service,
thay vì âm thầm query khi JSON serializer chạy.

Giải pháp không phải chuyển mọi relation thành EAGER.
Hãy tải đúng shape bằng:

- Projection.
- Fetch join cho use case cụ thể.
- Entity graph.
- Query DTO.
- Batch fetching khi phù hợp.

---

## 16. N+1 query

Giả sử tải 20 conversation bằng một query,
sau đó lặp qua từng conversation và gọi lazy <code>turns</code>.

~~~text
1 query lấy 20 conversations
+ 20 query lấy turns cho từng conversation
= 21 query
~~~

Với 1.000 parent,
vấn đề trở nên rất lớn.

N+1 thường khó thấy trên dataset nhỏ,
nên cần:

- Bật SQL log trong môi trường học.
- Đếm query trong integration test.
- Quan sát APM/database metrics.
- Review endpoint trả collection.

Không fetch join mù mọi relation:

- Có thể nhân bản hàng.
- Pagination với collection fetch có thể sai hoặc đắt.
- Tải quá nhiều dữ liệu.

Projection theo đúng màn hình thường là lựa chọn sạch.

---

## 17. Timestamp và lifecycle callback

Entity dùng:

~~~java
@PrePersist
void onCreate() {
    createdAt = Instant.now();
    updatedAt = createdAt;
}

@PreUpdate
void onUpdate() {
    updatedAt = Instant.now();
}
~~~

<code>Instant</code> biểu diễn một thời điểm tuyệt đối.
Nó phù hợp lưu timestamp server-side.

Các câu hỏi production cần quyết định:

- Clock của app có đồng bộ không?
- Timestamp do app hay database tạo?
- Có cần inject <code>Clock</code> để test không?
- Khi bulk update SQL,
  lifecycle callback có chạy không?

JPA bulk update thường bỏ qua entity lifecycle và persistence context.
Đây là lý do không nên mặc định callback xử lý mọi con đường ghi.

---

## 18. Cấu hình database hiện tại

[application.yml](../../../../../../resources/application.yml) cấu hình:

~~~yaml
datasource:
  url: jdbc:h2:mem:softaibox;MODE=PostgreSQL
jpa:
  open-in-view: false
  hibernate:
    ddl-auto: create-drop
~~~

Ý nghĩa:

- H2 chạy trong memory.
- PostgreSQL mode giúp một số cú pháp gần PostgreSQL hơn.
- Dữ liệu mất khi app dừng.
- Hibernate tạo schema khi start và xóa khi stop.
- Open Session in View bị tắt.

Những điều chưa production-ready:

- H2 không thay thế test trên PostgreSQL thật.
- <code>create-drop</code> có thể phá dữ liệu.
- Chưa có migration versioned.
- Chưa cấu hình connection pool theo tải.
- Chưa có backup/restore.
- Chưa có constraint/index đầy đủ.

---

## 19. Request/data flow: gửi một chat turn

Endpoint:

~~~http
POST /api/v1/chat
Authorization: Bearer access-token
Content-Type: application/json
~~~

Body:

~~~json
{
  "message": "Giải thích dirty checking",
  "conversation_id": 1,
  "modelName": "interview-coach"
}
~~~

Luồng dữ liệu:

~~~text
HTTP JSON
  ↓ Jackson + Bean Validation
ChatController.ChatRequest
  ↓ chuyển thành command
ChatService.send() — bắt đầu transaction
  ↓
ConversationService.requireForNewTurn()
  ↓ query theo id + owner
Conversation managed — increaseTurnCount()
  ↓
ChatTurnRepository.save()
  ↓
flush: INSERT turn + UPDATE conversation/version
  ↓
COMMIT
  ↓
ChatView → ApiResponse → JSON
~~~

Ownership lấy từ <code>Authentication.getName()</code>,
không lấy từ request body.
Client không thể tự khai owner của người khác.

---

## 20. Cách chạy và quan sát

Từ thư mục gốc repository:

~~~powershell
./mvnw.cmd clean test
./mvnw.cmd spring-boot:run
~~~

Ứng dụng chạy tại:

~~~text
http://localhost:8080
~~~

H2 console:

~~~text
http://localhost:8080/h2-console
~~~

JDBC URL lấy đúng từ
[application.yml](../../../../../../resources/application.yml):

~~~text
jdbc:h2:mem:softaibox;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
~~~

Để quan sát SQL khi học,
có thể tạm bật logger SQL trong cấu hình local.
Không bật bind parameter nhạy cảm một cách vô điều kiện ở production.

Bạn cũng có thể dùng:

- [requests.http](../../../../../../../../requests.http)
- [hướng dẫn Postman](../../../../../../../../postman/README.md)

Thứ tự thực hành:

1. Login USER.
2. Tạo conversation.
3. Gửi hai chat message.
4. Đọc lịch sử turn.
5. Mở H2 console và xem hai bảng.
6. Rename conversation rồi quan sát version/timestamp.
7. Xóa conversation và kiểm tra dữ liệu liên quan.

Ở bước 7,
hãy chú ý repo hiện chưa có FK/cascade được khai báo rõ.
Đây là một câu hỏi thiết kế,
không nên đoán database sẽ tự xóa turns.

---

## 21. Những lỗi thường gặp

### Nghĩ rằng ORM thay thế SQL

ORM chỉ sinh SQL.
Query tệ vẫn là query tệ.

### Gọi <code>save()</code> sau mọi setter

Managed entity đã được dirty checking.
Save thừa làm code nhiễu
và che mất hiểu biết về lifecycle.

### Đặt transaction ở controller

Controller nên xử lý HTTP.
Use case/service là nơi ranh giới transaction thường rõ hơn.

### Giữ transaction khi gọi model hoặc API ngoài

Điều này giữ connection và lock quá lâu.
Tách thành các bước ngắn hoặc workflow async.

### Dùng EAGER để “sửa” lazy error

EAGER có thể tải dữ liệu quá mức và tạo query khó kiểm soát.
Thiết kế query theo use case.

### Không có constraint vì “đã validate ở frontend”

Frontend không phải trust boundary.
Hai request concurrent cũng có thể vượt qua check ở service.

### Dùng offset page rất sâu

Offset lớn khiến database phải bỏ qua nhiều hàng
và dữ liệu có thể dịch chuyển giữa các lần đọc.
Xem xét keyset pagination.

### Bắt optimistic lock rồi retry vô hạn

Retry phải có giới hạn,
backoff và điều kiện idempotent.
Đôi khi trả 409 để user tải dữ liệu mới là đúng hơn.

---

## 22. Bài thực hành

### Bài 1 — Viết SQL

Viết query lấy 10 conversation mới cập nhật nhất
của một user và có ít nhất ba turns.

Yêu cầu:

- Có <code>JOIN</code>.
- Có <code>GROUP BY</code>.
- Có <code>HAVING</code>.
- Giải thích thứ tự xử lý logic.

### Bài 2 — Thiết kế index

Đề xuất index cho:

- Danh sách conversation theo owner, mới nhất trước.
- Lịch sử turn theo conversation, cũ nhất trước.
- Unique agent name theo owner.

Với mỗi index,
ghi query nào được lợi và write nào bị chậm hơn.

### Bài 3 — Quan sát dirty checking

Đặt breakpoint trong <code>ConversationService.rename</code>.

Quan sát:

1. Entity được load.
2. <code>rename()</code> đổi field.
3. Không có <code>save()</code>.
4. SQL UPDATE xuất hiện khi flush/commit.

### Bài 4 — Gây optimistic conflict

Viết integration test dùng hai transaction
cùng đọc một conversation,
sau đó cùng update.

Kỳ vọng:

- Một transaction commit.
- Transaction còn lại nhận optimistic locking exception.

### Bài 5 — Tạo và sửa N+1

Trên một branch học tập:

1. Ánh xạ Conversation–ChatTurn.
2. Tạo 20 conversations, mỗi conversation 3 turns.
3. Truy cập turns trong loop.
4. Đếm query.
5. Viết projection hoặc query phù hợp.
6. So sánh số query và dữ liệu được tải.

### Bài 6 — Transaction rollback

Chủ động làm INSERT ChatTurn thất bại
sau khi đã tăng turn count.

Xác nhận:

- Không có turn mới.
- Turn count không tăng.
- Exception đi ra đúng boundary.

### Bài 7 — Migration

Viết migration giả định cho:

- Hai bảng.
- Primary key.
- Foreign key.
- NOT NULL.
- Index theo access pattern.
- Version mặc định.

Không cần áp dụng vào code chính nếu chưa học Flyway;
mục tiêu là tập thiết kế schema rõ ràng.

---

## 23. Câu hỏi phỏng vấn và đáp án ngắn

### JPA khác Hibernate thế nào?

JPA là specification API cho persistence.
Hibernate là một implementation phổ biến của specification đó.

### Spring Data JPA làm gì?

Nó tạo abstraction repository,
sinh implementation/query từ interface
và tích hợp JPA với Spring.

### Entity managed là gì?

Là entity đang được persistence context theo dõi.
Thay đổi của nó có thể được dirty checking và flush thành SQL.

### <code>save()</code> khác <code>flush()</code>?

Save đưa entity vào persistence flow.
Flush đồng bộ thay đổi pending xuống database,
nhưng chưa chắc commit transaction.

### Dirty checking là gì?

Hibernate theo dõi entity managed
và tự sinh UPDATE khi trạng thái thay đổi trước flush.

### Vì sao lazy loading lỗi ngoài transaction?

Proxy/collection cần persistence context để query dữ liệu.
Khi context đã đóng,
Hibernate không thể tải phần chưa khởi tạo.

### N+1 là gì?

Một query tải N parent,
sau đó phát thêm một query cho mỗi parent,
tạo tổng cộng 1 + N query.

### <code>@Version</code> làm gì?

Nó thêm kiểm tra version vào UPDATE/DELETE
để phát hiện dữ liệu đã bị transaction khác thay đổi.

### Optimistic khác pessimistic locking?

Optimistic không giữ lock dài,
phát hiện conflict lúc ghi.
Pessimistic khóa dữ liệu sớm
và khiến transaction khác phải chờ.

### ACID là gì?

Atomicity, Consistency, Isolation, Durability:
nguyên tử, nhất quán, cách ly và bền vững.

### Index có luôn làm nhanh hơn không?

Không.
Index tăng tốc một số read
nhưng tốn storage và làm write/migration đắt hơn.

### Vì sao cần constraint khi đã validation?

Constraint bảo vệ tại nơi dữ liệu được lưu,
kể cả khi có request concurrent hoặc đường ghi khác bỏ qua validation.

### Vì sao không mở transaction quanh cuộc gọi LLM?

Network call có latency khó đoán,
giữ connection/lock lâu
và làm giảm khả năng chịu tải.

---

## 24. Liên hệ với SoftAIBox

Màn hình lịch sử chat cần:

- Query theo user hiện tại.
- Search title.
- Sort theo cập nhật mới nhất.
- Pagination có giới hạn.

Luồng gửi chat cần:

- Kiểm ownership conversation.
- Tạo turn.
- Cập nhật metadata conversation.
- Commit nhất quán.

Luồng agent cần:

- Lưu agent theo owner.
- Lưu execution bền vững ở production.
- Query trạng thái theo agent và user.
- Index cho polling thường xuyên.

Các quyết định hôm nay nối trực tiếp tới frontend:

~~~text
TanStack Query
  ↓ gọi REST
Controller
  ↓
Transactional service
  ↓
Repository/JPA
  ↓
SQL + constraint + index
~~~

Frontend cache không thể sửa dữ liệu backend mất nhất quán.
Tính đúng phải được bảo vệ ở transaction và database.

---

## 25. Cách học trong 4–6 giờ

### 60 phút đầu

- Đọc SQL, constraint và index.
- Viết tay schema hai bảng.
- Tự viết ba query SELECT/JOIN/GROUP.

### 60–90 phút tiếp

- Đọc hai entity và repository.
- Vẽ entity lifecycle.
- Giải thích vì sao rename không gọi save.

### 60–90 phút tiếp

- Đọc transaction flow của <code>ChatService.send</code>.
- Đặt breakpoint.
- Quan sát SQL và rollback.

### 60 phút tiếp

- Học isolation, lost update và <code>@Version</code>.
- Mô phỏng hai request concurrent trên giấy.

### 45–60 phút cuối

- Làm bài tập index/N+1.
- Trả lời câu hỏi phỏng vấn thành tiếng.
- Ghi ba rủi ro production của cấu hình H2 hiện tại.

---

## Definition of Done

Bạn hoàn thành ngày 10 khi có thể:

- Viết được SELECT, JOIN, GROUP BY, HAVING cho domain chat.
- Giải thích primary key, foreign key, unique và check constraint.
- Đề xuất index dựa trên query, không dựa trên cảm tính.
- Nói rõ bốn thuộc tính ACID.
- Phân biệt các anomaly và lost update.
- Phân biệt JPA, Hibernate và Spring Data JPA.
- Mô tả transient, managed, detached và removed.
- Giải thích dirty checking, flush và commit.
- Lần theo transaction của <code>ChatService.send</code>.
- Giải thích tác dụng của <code>@Version</code>.
- Nhận diện lazy loading và N+1.
- Chỉ ra ít nhất năm điểm database demo chưa production-ready.
- Chạy app, tạo chat turn và quan sát dữ liệu trong H2.
- Trả lời bộ câu hỏi phỏng vấn mà không nhìn tài liệu.
