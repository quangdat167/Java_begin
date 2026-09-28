# Test API bằng Postman

## Import

Trong Postman chọn **Import** và import hai file:

1. `SoftAIBox_Java_Lab.postman_collection.json`
2. `SoftAIBox_Local.postman_environment.json`

Chọn environment **SoftAIBox Java Lab - Local** ở góc trên bên phải.

## Chạy backend

Từ thư mục `Java_begin`:

```powershell
.\mvnw.cmd spring-boot:run
```

Chờ log có dòng `Started JavaBeginApplication`, sau đó mở Collection Runner và chạy
theo thứ tự folder từ `01` đến `06`.

## Thứ tự quan trọng

- Login USER tự lưu access/refresh token.
- Create conversation tự lưu `conversationId`; phải chạy trước các request Chat.
- Create agent tự lưu `agentId`; Execute agent tự lưu `executionId`.
- Request Delete conversation nằm cuối folder Chat nên Collection Runner có thể chạy
  toàn bộ collection theo đúng thứ tự mà không để lại dữ liệu conversation.
- Folder RBAC phải chạy khi token hiện tại vẫn là USER. Request Login ADMIN sau đó sẽ
  ghi đè token để kiểm tra endpoint admin trả 200.
- Folder Validation dùng token hiện tại; test “title rỗng” vẫn hợp lệ với ADMIN vì mọi
  user đã xác thực đều được tạo conversation.

Các biến động được lưu ở **Collection variables**, không phải Environment. Mở Collection
-> Variables để xem `accessToken`, `refreshToken`, `conversationId`, `agentId` và
`executionId` sau khi chạy.
