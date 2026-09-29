# Ngày 13 — Spring Security, JWT, refresh token, RBAC và CORS

Ngày 13 học cách backend xác định “bạn là ai” và “bạn được làm gì”. Hãy phân biệt rõ code demo phục vụ học tập với hệ thống production: JWT secret, user store và refresh session hiện đều tối giản.

Code đọc theo thứ tự:

1. [SecurityConfig.java](../../security/SecurityConfig.java): filter chain, JWT encoder/decoder, RBAC và CORS.
2. [DemoUserService.java](../../auth/DemoUserService.java): user store và BCrypt.
3. [TokenService.java](../../auth/TokenService.java): access JWT, opaque refresh token và rotation.
4. [AuthController.java](../../auth/AuthController.java): login, refresh và profile.
5. [Role.java](../../auth/Role.java) và [UserProfile.java](../../auth/UserProfile.java): role/profile contract.
6. [AdminDemoController.java](../../security/AdminDemoController.java): method authorization.

---

## Mục tiêu

Sau ngày 13, bạn cần:

- Phân biệt authentication và authorization.
- Giải thích Spring Security filter chain chạy trước controller.
- Hiểu password hashing và vì sao không lưu raw password.
- Mô tả cấu trúc JWT và khác biệt giữa ký với mã hóa.
- Theo được access-token flow trong code.
- Phân biệt access token và refresh token.
- Giải thích refresh rotation/revocation/reuse detection.
- Hiểu cách claim role thành authority <code>ROLE_ADMIN</code>.
- Phân biệt URL security và method security.
- Giải thích 401 với 403.
- Hiểu CORS không phải authentication.
- Phân tích CSRF, XSS và vị trí lưu token theo threat model.
- Chạy login, refresh, profile và RBAC bằng Postman.

---

## 1. Authentication và authorization

Authentication trả lời: **request đến từ ai?** Authorization trả lời: **identity đó được phép làm gì?**

Ví dụ:

~~~text
Email + password đúng
  → authentication thành công
  → identity = dat@softaibox.local

Identity gọi /api/v1/admin/demo
  → authorization kiểm role
  → USER bị 403, ADMIN được 200
~~~

Frontend <code>PrivateRoute</code> hoặc ẩn nút Admin chỉ cải thiện UX. Người dùng có thể gọi API trực tiếp, nên backend mới là security boundary.

---

## 2. Principal, credential, authority và role

- Principal: identity, trong repo là email trong JWT subject.
- Credential: bằng chứng xác thực như password hoặc token.
- Authority: quyền Spring dùng để quyết định truy cập.
- Role: nhóm quyền cấp cao; Spring convention thường thêm prefix <code>ROLE_</code>.

Token chứa claim:

~~~json
{
  "sub": "admin@softaibox.local",
  "role": "ADMIN",
  "userId": 2
}
~~~

Converter tạo:

~~~java
new SimpleGrantedAuthority("ROLE_" + role)
~~~

Vì vậy <code>ADMIN</code> thành authority <code>ROLE_ADMIN</code>, khớp:

~~~java
@PreAuthorize("hasRole('ADMIN')")
~~~

Nếu dùng <code>hasAuthority</code>, thường phải ghi đúng chuỗi <code>ROLE_ADMIN</code>.

---

## 3. Security filter chain chạy trước controller

Luồng protected request:

~~~text
HTTP request
  ↓
CORS/security filters
  ↓ resolve Bearer token
JwtDecoder: signature + token checks
  ↓
JwtAuthenticationConverter
  ↓
SecurityContext có Authentication
  ↓
URL authorization
  ↓
DispatcherServlet/controller
  ↓
@PreAuthorize nếu method có rule
~~~

Nếu token thiếu hoặc không hợp lệ, request có thể kết thúc với 401 trước khi controller được gọi. Nếu identity hợp lệ nhưng thiếu quyền, kết quả là 403.

---

## 4. Security rules trong repository

[SecurityConfig.java](../../security/SecurityConfig.java):

~~~java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers(
        "/api/v1/auth/email/login",
        "/api/v1/auth/refresh",
        "/h2-console/**"
    )
    .permitAll()
    .anyRequest()
    .authenticated()
)
~~~

Ý nghĩa:

- Login public để có thể lấy token.
- Refresh public ở filter level vì bearer tại đó là opaque refresh token, không phải JWT.
- H2 console public chỉ phục vụ local learning; không mở như vậy ở production.
- Mọi endpoint còn lại cần authentication.

<code>permitAll</code> không có nghĩa endpoint refresh chấp nhận mọi token. Nó chỉ cho request tới controller; controller/service vẫn kiểm refresh session.

---

## 5. Stateless session

Config:

~~~java
.sessionManagement(session ->
    session.sessionCreationPolicy(
        SessionCreationPolicy.STATELESS
    )
)
~~~

Backend không dựa vào HTTP session để nhớ user giữa request. Mỗi protected request phải cung cấp credential, ở đây là bearer access JWT.

Stateless access token giúp scale request validation, nhưng toàn hệ thống chưa hoàn toàn stateless vì refresh sessions đang được lưu trong memory của server.

---

## 6. Password không được lưu plaintext

[DemoUserService.java](../../auth/DemoUserService.java) tạo hash:

~~~java
passwordEncoder.encode("java123")
~~~

và kiểm:

~~~java
passwordEncoder.matches(rawPassword, passwordHash)
~~~

Không so sánh bằng cách encode lại rồi dùng <code>equals</code>; thuật toán adaptive hash dùng salt nên hai hash của cùng password có thể khác.

### Hash khác encryption

- Hash password là một chiều; verify bằng hàm matches.
- Encryption có key và có thể giải mã.
- Password storage cần slow adaptive hash như BCrypt, Argon2 hoặc scrypt.
- SHA-256 đơn thuần quá nhanh, dễ brute-force bằng GPU.

BCrypt cost phải được chọn theo hiệu năng production và có kế hoạch nâng cost.

---

## 7. Tránh user enumeration

Service dùng cùng message:

~~~text
Email or password is incorrect
~~~

cho email không tồn tại và password sai. Điều này giảm việc lộ danh sách account qua message.

Production còn cân nhắc:

- Timing gần tương đương.
- Rate limiting theo account/IP/device.
- Lockout có kiểm soát để tránh denial-of-service.
- Audit login thất bại nhưng không log raw password.
- MFA cho rủi ro cao.

Repo map lỗi login thành 400 do <code>IllegalArgumentException</code>. Một API khác có thể chọn 401; điều quan trọng là contract nhất quán và không lộ nguyên nhân chi tiết.

---

## 8. JWT gồm những gì?

JWT compact thường có ba phần:

~~~text
base64url(header).base64url(payload).base64url(signature)
~~~

Header mô tả thuật toán, payload chứa claims, signature bảo vệ tính toàn vẹn/xác thực nguồn.

Quan trọng:

> JWT được ký thường không được mã hóa.

Bất kỳ ai có token đều có thể base64url-decode header/payload. Không đặt password, secret hoặc PII nhạy cảm trong claim.

Signature cho biết payload chưa bị sửa nếu key/thuật toán đúng; nó không che nội dung.

---

## 9. Claims trong access token

[TokenService.java](../../auth/TokenService.java) đặt:

~~~java
JwtClaimsSet.builder()
    .issuer("softaibox-java-lab")
    .issuedAt(now)
    .expiresAt(expiresAt)
    .subject(user.email())
    .claim("role", user.role().name())
    .claim("userId", user.id())
    .build();
~~~

Claims:

| Claim | Ý nghĩa |
|---|---|
| <code>iss</code> | Issuer phát token |
| <code>iat</code> | Thời điểm phát |
| <code>exp</code> | Hết hạn |
| <code>sub</code> | Principal/email |
| <code>role</code> | Role demo |
| <code>userId</code> | ID user |

Production cần xác định và validate issuer, audience, time skew, algorithm. Decoder hiện cấu hình HS256 secret và dựa vào validation mặc định cho các claim tiêu chuẩn; repo chưa cấu hình validator issuer/audience tường minh.

---

## 10. Ký HS256 và quản lý secret

Repo tạo cùng một symmetric key cho encoder và decoder:

~~~java
new SecretKeySpec(
    secret.getBytes(StandardCharsets.UTF_8),
    "HmacSHA256"
)
~~~

Với HS256, bên verify có secret cũng có khả năng ký token. Nó phù hợp service đơn/demo, nhưng khi nhiều service chỉ nên verify, asymmetric algorithm có thể giảm việc phân phối signing key.

Config chặn secret ngắn hơn 32 bytes. Đây là guard cơ bản, không thay thế:

- Random secret đủ entropy.
- Secret manager.
- Rotation và key ID.
- Không commit secret.
- Giới hạn quyền truy cập.
- Audit.

[application.yml](../../../../../../resources/application.yml) ghi rõ secret local chỉ để học.

---

## 11. Access token flow

Login:

~~~text
POST /auth/email/login
  ↓ validate email/password shape
DemoUserService.authenticate()
  ↓ BCrypt matches
TokenService.issue()
  ↓ tạo signed JWT + refresh UUID
AuthController.toResponse()
  ↓ token, refreshToken, tokenExpires, user
~~~

Request protected:

~~~text
Authorization: Bearer access-jwt
  ↓
JwtDecoder verify
  ↓
subject + ROLE_USER/ROLE_ADMIN
  ↓
Controller nhận Authentication hoặc Jwt
~~~

Endpoint profile:

~~~java
ApiResponse<UserProfile> me(
        @AuthenticationPrincipal Jwt jwt
) {
    return ApiResponse.success(
        UserProfile.from(
            users.findByEmail(jwt.getSubject())
        )
    );
}
~~~

Identity lấy từ token, không lấy email tùy ý trong query/body.

---

## 12. Access token và refresh token

| Access token | Refresh token |
|---|---|
| TTL ngắn | TTL dài hơn |
| Gửi tới resource API | Chỉ gửi tới refresh endpoint |
| Chứa claims trong JWT demo | UUID opaque trong demo |
| Verify không cần DB | Cần tra server-side session |
| Lộ thì cửa sổ ngắn hơn | Lộ nguy hiểm hơn |

Config hiện tại:

~~~yaml
app:
  jwt:
    access-ttl: PT15M
    refresh-ttl: P7D
~~~

Access token hết sau 15 phút, refresh token sau 7 ngày.

---

## 13. Refresh rotation

Refresh flow:

~~~text
POST /api/v1/auth/refresh
Authorization: Bearer refresh-uuid
  ↓
remove refresh session cũ
  ↓ invalid/expired? → 400
  ↓
issue access token mới + refresh token mới
~~~

Code dùng:

~~~java
RefreshSession session =
        refreshSessions.remove(refreshToken);
~~~

Việc remove trước khi issue làm token cũ chỉ dùng thành công một lần. Hai request refresh concurrent: chỉ một request lấy được session, request còn lại thất bại. Đó là rotation cơ bản.

---

## 14. Vì sao refresh store hiện tại chỉ là demo?

<code>ConcurrentHashMap</code> có các hạn chế:

- Mất toàn bộ session khi restart.
- Mỗi app replica có map khác.
- Token được lưu plaintext.
- Không có device/session metadata.
- Không có logout/revoke-all.
- Không có audit.
- Không phát hiện token family reuse đầy đủ.
- Không cleanup session hết hạn chủ động.

Production thường lưu hash của refresh token, user/session/device, expiry, revokedAt, replacedBy/token family. Nếu token cũ đã rotate bị dùng lại, revoke cả family và yêu cầu login.

Không lưu raw refresh token trong log hoặc database nếu hash đủ cho lookup/verify.

---

## 15. BearerTokenResolver đặc biệt cho refresh

Spring Resource Server mặc định thấy header Bearer và cố decode nó như JWT. Nhưng refresh token demo chỉ là UUID.

Repo cấu hình:

~~~java
return request ->
    "/api/v1/auth/refresh"
        .equals(request.getServletPath())
            ? null
            : delegate.resolve(request);
~~~

Với refresh endpoint, resource-server filter bỏ qua bearer; controller tự đọc header và gửi opaque token tới <code>TokenService.rotate</code>.

Đây là chi tiết quan trọng: endpoint được <code>permitAll</code> ở URL layer nhưng vẫn có application-level credential validation.

---

## 16. RBAC

Role-Based Access Control gán quyền dựa trên role.

[AdminDemoController.java](../../security/AdminDemoController.java):

~~~java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/demo")
ApiResponse<?> adminOnly() {
}
~~~

<code>@EnableMethodSecurity</code> bật method authorization.

Flow:

~~~text
JWT role ADMIN
  → converter tạo ROLE_ADMIN
  → @PreAuthorize hasRole ADMIN
  → cho phép

JWT role USER
  → ROLE_USER
  → thiếu ROLE_ADMIN
  → 403
~~~

Role đơn giản phù hợp demo. Production có thể cần permission nhỏ hơn như <code>agent:read</code>, <code>agent:execute</code>, tenant scope và ownership rule.

---

## 17. URL security và method security

URL security trong filter chain phù hợp rule tổng quát:

~~~text
/api/v1/admin/** cần ADMIN
mọi endpoint còn lại cần authenticated
~~~

Method security đặt rule gần use case:

~~~java
@PreAuthorize("hasRole('ADMIN')")
~~~

Defense in depth có thể dùng cả hai, nhưng tránh rule mâu thuẫn khó hiểu. Ownership theo dữ liệu thường vẫn phải kiểm ở service/repository:

~~~java
findByIdAndOwnerEmail(id, authentication.getName())
~~~

Role đúng không tự động cho phép đọc resource của user khác.

---

## 18. 401 và 403

### 401

- Không có token.
- Token malformed.
- Signature sai.
- Token hết hạn.

### 403

- JWT hợp lệ.
- User đã authenticated.
- Nhưng thiếu authority yêu cầu.

Test bắt buộc:

1. Không token gọi profile → 401.
2. USER gọi admin → 403.
3. ADMIN gọi admin → 200.

Không trả 200 kèm <code>code = "UNAUTHORIZED"</code>; HTTP status phải đúng để client interceptor hoạt động.

---

## 19. CORS là gì?

Origin gồm scheme + host + port:

~~~text
http://localhost:5173
http://localhost:8080
~~~

Hai origin khác nhau. Browser áp Same-Origin Policy và dùng CORS response headers để quyết định JavaScript frontend có được đọc response cross-origin hay không.

Repo cho phép:

~~~java
configuration.setAllowedOrigins(
    List.of("http://localhost:5173")
);
configuration.setAllowedMethods(
    List.of("GET", "POST", "PUT",
            "PATCH", "DELETE", "OPTIONS")
);
configuration.setAllowedHeaders(
    List.of("Authorization", "Content-Type")
);
configuration.setAllowCredentials(true);
~~~

CORS là browser policy, không phải authentication. Curl/Postman không bị browser CORS chặn nhưng vẫn phải có token.

---

## 20. Preflight request

Browser có thể gửi:

~~~http
OPTIONS /api/v1/chat
Origin: http://localhost:5173
Access-Control-Request-Method: POST
Access-Control-Request-Headers: authorization,content-type
~~~

Server trả CORS headers phù hợp trước khi browser gửi POST thật.

Nếu preflight fail, controller POST chưa chạy. Debug CORS cần kiểm network tab và OPTIONS, không chỉ business endpoint.

Khi <code>allowCredentials=true</code>, không dùng wildcard origin tùy tiện. Allowlist chính xác theo môi trường.

---

## 21. CSRF và token storage

CSRF lợi dụng việc browser tự gắn credential, điển hình cookie, vào request từ site độc hại. Repo disable CSRF:

~~~java
.csrf(csrf -> csrf.disable())
~~~

Điều này thường được cân nhắc cho stateless bearer token gửi explicit trong Authorization header. Nhưng nếu chuyển access/refresh token sang cookie tự động gửi, phải đánh giá lại CSRF protection.

### localStorage

Ưu điểm: dễ dùng với Authorization header. Nhược điểm: JavaScript đọc được; XSS có thể lấy token.

### HttpOnly cookie

JavaScript không đọc được, giảm nguy cơ token theft qua XSS. Nhưng browser tự gửi cookie nên cần SameSite/CSRF design, secure flag, domain/path đúng.

Không có lựa chọn tuyệt đối cho mọi hệ thống. Phải dựa trên threat model, kiến trúc frontend/backend và yêu cầu UX.

XSS vẫn nguy hiểm với cookie HttpOnly vì script độc có thể thực hiện action trong browser dù không đọc token.

---

## 22. Logout và revocation

Access JWT tự chứa thông tin nên khó revoke ngay nếu không có state bổ sung. Các chiến lược:

- Access TTL ngắn.
- Revoke refresh session để không cấp access mới.
- Token version trên user/session.
- Denylist theo <code>jti</code> cho trường hợp đặc biệt.
- Rotate signing key trong sự cố lớn.

Logout production thường revoke refresh session/family và xóa credential phía client. Access token đã phát có thể còn hợp lệ tới expiry nếu không có denylist.

Trade-off là giữa stateless validation, khả năng revoke tức thì, độ phức tạp và tải storage.

---

## 23. Những gì cần verify trên JWT

Production checklist:

- Chỉ chấp nhận algorithm mong đợi.
- Signature hợp lệ.
- <code>exp</code> chưa qua.
- <code>nbf</code> nếu dùng.
- <code>iss</code> đúng.
- <code>aud</code> chứa API mong đợi.
- Clock skew nhỏ, có chủ đích.
- Required claim tồn tại và đúng kiểu.
- Key rotation/kid.

Không tin claim chỉ vì decode được base64. Phải verify signature và validation policy.

---

## 24. Request flow: login

~~~http
POST /api/v1/auth/email/login
Content-Type: application/json

{
  "email": "dat@softaibox.local",
  "password": "java123"
}
~~~

~~~text
JSON + @Valid
  ↓
DemoUserService.findByEmail
  ↓
BCrypt matches
  ↓
TokenService.issue
  ├─ signed access JWT
  └─ refresh UUID + in-memory session
  ↓
TokenResponse
~~~

Response fields tương thích frontend: <code>token</code>, <code>refreshToken</code>, <code>tokenExpires</code>, <code>user</code>.

---

## 25. Request flow: protected API

~~~http
GET /api/v1/auth/me
Authorization: Bearer access-jwt
~~~

~~~text
Bearer resolver
  ↓
JwtDecoder HS256
  ↓
JwtAuthenticationConverter
  ↓ subject + authority
  ↓
AuthController.me(@AuthenticationPrincipal Jwt)
  ↓
UserProfile
~~~

Controller không tự parse token string. Security layer đã xác thực và cung cấp principal.

---

## 26. Request flow: refresh

~~~http
POST /api/v1/auth/refresh
Authorization: Bearer refresh-uuid
~~~

~~~text
Custom resolver bỏ qua resource-server JWT decode
  ↓
AuthController đọc Authorization
  ↓
remove prefix Bearer
  ↓
TokenService.rotate
  ↓ token cũ bị remove
  ↓
access + refresh mới
~~~

Client phải thay cả hai token sau refresh. Nếu chỉ thay access token nhưng giữ refresh cũ, lần refresh tiếp theo thất bại.

---

## 27. Cách chạy bằng Postman

Start app:

~~~powershell
./mvnw.cmd spring-boot:run
~~~

Import theo [postman/README.md](../../../../../../../../postman/README.md), chọn environment local và chạy:

1. Login USER; script tự lưu access/refresh token.
2. Get profile.
3. Refresh; script thay token cũ.
4. USER gọi Admin API, kỳ vọng 403.
5. Login ADMIN, ghi đè token.
6. ADMIN gọi Admin API, kỳ vọng 200.
7. Không token gọi profile, kỳ vọng 401.

Tài khoản demo:

| Role | Email | Password |
|---|---|---|
| USER | <code>dat@softaibox.local</code> | <code>java123</code> |
| ADMIN | <code>admin@softaibox.local</code> | <code>admin123</code> |

Chỉ dùng local learning.

---

## 28. Cách đọc JWT an toàn khi học

Bạn có thể decode header/payload để quan sát claims, nhưng đừng paste token production vào website không tin cậy.

Thực hành:

1. Login lấy access token.
2. Decode local.
3. Tìm <code>sub</code>, <code>role</code>, <code>iat</code>, <code>exp</code>.
4. Sửa một ký tự payload.
5. Gọi API và quan sát signature verification thất bại.
6. Không commit token vào repository.

Decode không phải verify.

---

## 29. Những lỗi thường gặp

### Đặt password trong JWT

JWT payload dễ đọc. Không bao giờ đặt credential/secret trong claim.

### Dùng SHA-256 trực tiếp cho password

Quá nhanh và không adaptive. Dùng BCrypt/Argon2/scrypt với thư viện chuẩn.

### JWT TTL rất dài

Token bị lộ có cửa sổ khai thác dài. Dùng access ngắn + refresh lifecycle.

### Lưu refresh token plaintext vĩnh viễn

Database leak biến token thành credential dùng được. Lưu hash và metadata.

### Chỉ chặn role ở frontend

Ẩn UI không ngăn gọi API. Backend phải authorize.

### Nhầm CORS với security

CORS không chặn curl/attacker server. Authentication và authorization vẫn bắt buộc.

### Trả 403 cho token hết hạn

Token không còn identity hợp lệ nên thường là 401.

### Tin role từ request body

Role phải đến từ trusted identity store/token đã verify, không từ client payload.

### Log Authorization header

Log có thể trở thành nơi rò token. Redact credential.

### Không validate issuer/audience

Token hợp lệ về chữ ký nhưng dành cho hệ thống khác có thể bị chấp nhận.

---

## 30. Bài thực hành

### Bài 1 — Ma trận 401/403

Test thiếu token, malformed token, expired token, USER vào admin, ADMIN vào admin. Ghi layer quyết định từng status.

### Bài 2 — TTL ngắn

Trong cấu hình học tập, giảm access TTL còn 10 giây. Login, gọi profile thành công, chờ hết hạn, nhận 401, refresh và gọi lại. Khôi phục config sau bài.

### Bài 3 — Refresh reuse

Login, refresh một lần, dùng lại refresh token cũ. Giải thích vì sao <code>remove</code> làm lần hai thất bại và production nên phát hiện token family reuse ra sao.

### Bài 4 — Thiết kế refresh entity

Đề xuất fields: ID, userId, tokenHash, familyId, device, createdAt, expiresAt, rotatedAt, revokedAt, replacedBy. Viết index và cleanup strategy.

### Bài 5 — Permission

Thêm thiết kế permission <code>AGENT_EXECUTE</code> thay vì chỉ ADMIN/USER. Mô tả claim, converter và <code>@PreAuthorize</code>; chưa cần sửa code chính.

### Bài 6 — Threat model token storage

So sánh localStorage với secure HttpOnly cookie cho SoftAIBox. Phân tích XSS, CSRF, cross-origin, refresh flow và logout.

### Bài 7 — CORS

Gửi preflight OPTIONS từ origin được phép và origin khác. Quan sát header và phân biệt lỗi browser với lỗi backend business.

---

## 31. Câu hỏi phỏng vấn và đáp án ngắn

### Authentication khác authorization?

Authentication xác định identity; authorization quyết định identity được làm gì.

### Hash khác encryption?

Hash là một chiều để verify; encryption có thể giải mã bằng key.

### JWT có mã hóa payload không?

JWT ký thông thường không mã hóa; payload có thể đọc, signature chống sửa.

### Access khác refresh token?

Access sống ngắn và gọi API; refresh sống dài hơn, chỉ dùng để cấp cặp token mới và cần quản lý/revoke.

### Vì sao rotate refresh token?

Giảm khả năng token bị đánh cắp được tái sử dụng và hỗ trợ phát hiện reuse.

### 401 khác 403?

401 là chưa xác thực hợp lệ; 403 là đã xác thực nhưng thiếu quyền.

### <code>hasRole("ADMIN")</code> kiểm gì?

Theo convention Spring, nó kiểm authority <code>ROLE_ADMIN</code>.

### CORS có bảo vệ API khỏi Postman không?

Không. CORS là chính sách do browser thực thi.

### Khi nào CSRF quan trọng?

Khi browser tự động gửi credential như cookie; phải phân tích cách token được vận chuyển.

### OAuth2 khác JWT?

OAuth2 là authorization framework/protocol flows; JWT là token format. OAuth2 có thể dùng token JWT hoặc opaque.

### URL security khác method security?

URL security chặn ở filter chain theo request; method security bảo vệ lời gọi method/use case gần code nghiệp vụ.

### JWT revoke thế nào?

Thường access TTL ngắn, revoke refresh session; nếu cần tức thì dùng denylist/token version hoặc state khác với trade-off.

---

## 32. Liên hệ SoftAIBox

Frontend cần:

~~~text
login
  → lưu credential theo chiến lược
  → gắn access token vào request
  → nhận 401
  → refresh đúng một lần
  → thay cả access + refresh
  → retry request
~~~

Phải tránh nhiều request 401 cùng lúc tạo refresh storm. Frontend thường cần single-flight refresh và queue request chờ.

Backend cần:

- Subject/tenant đáng tin.
- Ownership query.
- Role/permission.
- Refresh persistence/rotation.
- Audit và rate limit.
- CORS allowlist.
- Không log credential.

Frontend route guard và backend authorization bổ sung nhau, nhưng chỉ backend bảo vệ dữ liệu.

---

## 33. Kế hoạch học

1. 60 phút: authentication, authorization, filter chain và 401/403.
2. 60 phút: password hashing, login flow.
3. 60–90 phút: JWT claims, signature, access token flow.
4. 60 phút: refresh rotation và production design.
5. 45 phút: RBAC, URL/method/ownership authorization.
6. 45 phút: CORS, CSRF, XSS và token storage.
7. 45 phút: chạy Postman và trả lời phỏng vấn.

---

## Definition of Done

Bạn hoàn thành ngày 13 khi có thể:

- Phân biệt authentication/authorization.
- Vẽ security filter flow trước controller.
- Giải thích BCrypt và password hashing.
- Mô tả ba phần JWT và ký khác mã hóa.
- Nêu claims cần verify.
- Theo login → access token → protected API.
- Theo refresh rotation và dùng token mới.
- Giải thích hạn chế của in-memory refresh store.
- Map role claim thành Spring authority.
- Chứng minh USER nhận 403, ADMIN nhận 200.
- Phân biệt URL, method và ownership authorization.
- Giải thích CORS, preflight, CSRF và XSS.
- Đánh giá localStorage/cookie theo threat model.
- Chỉ ra ít nhất tám điểm demo chưa production-ready.
- Chạy trọn folder Authentication, RBAC và security error trong Postman.
- Trả lời câu hỏi phỏng vấn không nhìn tài liệu.
