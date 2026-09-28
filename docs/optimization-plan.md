# Kế hoạch tối ưu hệ thống

Branch: `feature/optimization-phase-0-1` (tách từ `feature/production-readiness-round3`), gồm giai đoạn 0–4.
Nguồn: audit ngày 2026-09-28 (DB/JPA, cấu trúc code, cấu hình/test/build). Không thêm scope ngoài
`docs/07_scope_freeze.md`.

## Trạng thái

- ✅ xong và đã kiểm chứng
- ⬜ chưa làm

| Giai đoạn | Nội dung | Trạng thái |
|---|---|---|
| 0 | Lưới an toàn: integration test trên Oracle thật, JaCoCo, đo baseline | ✅ |
| 1 | Quick wins: batching, index, sửa query lỗi/nặng, config, 2 lỗi bảo mật nhỏ | ✅ |
| 2 | Hot path mỗi request: cache principal/permission, audit cùng transaction, mail sau commit | ✅ |
| 3 | Khử N+1 ở service nặng (CourseResult, MyLearning, ClassEnrollment, dashboard), pooled sequence | ✅ |
| 4 | File: stream upload/download, clone BLOB phía DB (giữ BLOB trong Oracle) | ✅ |
| 5 | Tái cấu trúc: tách god service, gom logic trùng, i18n, code chết | ⬜ |
| 6 | Build/CI/docs: loại `db/seed` khỏi jar prod, build-info, dọn `docs/.claude` | ⬜ |

## Quyết định đã chốt (2026-09-28)

1. **Audit** ghi cùng transaction với nghiệp vụ: chỉ log thao tác đã commit; thao tác bị rollback
   không còn log (trước đây có, do `REQUIRES_NEW`).
2. **File** giữ BLOB trong Oracle, tối ưu streaming. Không thêm object storage.
3. **Pooled sequence** chấp nhận id nhảy cóc.
4. **Một instance**: cache Caffeine trong process, TTL ngắn + xoá ngay khi đổi dữ liệu. Nếu sau này
   chạy nhiều instance, thay đổi quyền có hiệu lực chậm tối đa bằng TTL ở instance khác.

## Giai đoạn 0 — đã giao

| Hạng mục | File |
|---|---|
| Failsafe chạy `*IT` trong `verify`; JaCoCo report unit + merged | `pom.xml` |
| Hạ tầng IT: Oracle qua `FAP_IT_DB_URL` hoặc Testcontainers `gvenzl/oracle-xe`, rollback mỗi test, đếm statement/entity bằng Hibernate Statistics | `src/test/java/com/fap/support/*`, `src/test/resources/application-it.yaml` |
| Flyway + `ddl-auto=validate` trên Oracle thật | `DatabaseSchemaIT` |
| Đo baseline các read path nặng → `target/query-baseline.txt` | `QueryBaselineIT` |
| CI: bắt buộc chạy IT (`FAP_IT_REQUIRE_DB=true`), timeout 30 phút, upload report | `.github/workflows/backend-ci.yml` |
| `HIBERNATE_STATS=true` để soi N+1 khi chạy local | `application-local.yaml` |

### Lỗi thật mà Giai đoạn 0 phát hiện

| Lỗi | Ảnh hưởng | Sửa |
|---|---|---|
| Regex che secret trong `logback-spring.xml` chứa `}`, bị logback cắt → `PatternSyntaxException` | **Mọi profile ngoài `local`/`test` (kể cả prod) không khởi động được** | Dùng `\x7D`; thêm `LogbackConfigurationTest` nạp cấu hình như prod |
| `QuizRepository.searchAssignedToUser` / `findAssignedToUserByClass`: `DISTINCT` trên entity có CLOB → `ORA-00932` | API quiz được giao cho trainee trả 500 trên Oracle | Đổi join + DISTINCT thành `EXISTS` |
| `MaterialFileRepository.searchAssignedToUser` / `findAssignedToUserByClass`: như trên (Syllabus có CLOB) | API tài liệu / My Learning của trainee trả 500 | Đổi join + DISTINCT thành `EXISTS` |

## Giai đoạn 1 — đã giao

| Hạng mục | Trước | Sau |
|---|---|---|
| User search (`UserRepository.search`) | Phân trang trong RAM (HHH90003004): trang 10 user tải 27 entity (mọi user khớp) | Trang id trong SQL rồi fetch role: ≤ 14 entity, ≤ 4 statement (`UserSearchQueryIT`) |
| Quiz result summary | Tải mọi attempt + CLOB answers (8 entity với seed) | 1 câu aggregate, chỉ tải quiz (1 entity, ≤ 2 statement) (`QuizQueriesIT`) |
| Login / check trùng email, class code, syllabus code | `UPPER(col) = UPPER(?)` full scan | Function index — plan: `INDEX RANGE SCAN IDX_USERS_EMAIL_UPPER` |
| Index (V32) | 8 FK hay dùng không index; conflict lịch chỉ có index 1 cột | FK index, composite `(class_id/trainer_id, start_time, end_time)`, room, session_date, audit created_at, attempt status/submitted_at |
| JPA | Không batch | `jdbc.batch_size=50`, `order_inserts/updates`, `default_batch_fetch_size=50`, `fetch_size=100` |
| Hikari keepalive | 0 (tắt) | 120 s |
| Nén response | Tắt | gzip JSON ≥ 2 KB |
| Audit IP | Tin `X-Forwarded-For` của client → giả mạo được | Chỉ tin khi `app.rate-limit.trust-forward-headers=true` (`AuditLogServiceTest`) |
| OTP khi mail tắt | Ghi OTP ra log ở mọi profile | Chỉ profile `local` (`PasswordResetMailServiceTest`) |

## Giai đoạn 2 — đã giao

| Hạng mục | Trước | Sau |
|---|---|---|
| Principal của JWT | `loadUserByUsername` (user + roles) mỗi request | `AuthorizationCache` Caffeine, TTL `AUTHORIZATION_CACHE_TTL` (60 s), xoá sau commit khi user đổi |
| Permission | `findByRoleIdIn` mỗi `@PreAuthorize` | Cache theo role, xoá sau commit khi đổi ma trận quyền |
| JWT filter | Parse token 2 lần; user bị khoá vẫn dùng token cũ tới hết hạn | Parse 1 lần; kiểm tra `isEnabled()` (`JwtAuthenticationFilterTest`) |
| Audit | `REQUIRES_NEW`: giữ 2 kết nối/request, log cả thao tác rollback | Cùng transaction (`REQUIRED`) |
| Mail OTP | Gửi SMTP trong transaction; email tồn tại trả lời chậm hơn (lộ tài khoản qua timing) | `@Async` sau commit, dùng `AsyncConfig` vốn có sẵn |
| Notification | `findById(user)` mỗi người nhận | `getReferenceById` |

Password login vẫn đọc DB (không cache), nên đổi mật khẩu có hiệu lực ngay; bản cache không chứa hash.

## Giai đoạn 3 — đã giao

| Read path (seed local) | Statements trước | Sau | Cách |
|---|---:|---:|---|
| `adminDashboard.getDashboard` | 27 | 16 (0 khi cache nóng) | `group by status` + cache `DASHBOARD_CACHE_TTL` (30 s); cache hit không mượn kết nối |
| `courseResult.list` (5 kết quả) | 12 (2N+2) | 4 (hằng số) | Snapshot quiz và adjustment đọc theo lớp |
| `courseResult.calculate` | ~6 + 2 × số quiz bắt buộc, mỗi học viên (+ `list` 2N+2) | 13 cho cả lớp (seed: 3 kết quả), kể cả ghi và `list`; không tăng theo số học viên | Đọc kết quả, đăng ký, điểm danh, bài làm tốt nhất (projection, không CLOB) một lần; bulk delete snapshot; ghi theo batch |
| `myLearning.learningContent` | 23 | 11 | Thống kê bài làm + số câu hỏi theo nhóm |
| `myLearning.progress` | 15 | 9 | Như trên + điểm danh `group by status` |
| Danh sách tài liệu | `existsById` mỗi tài liệu | 0 | Cờ `contentStored` là `@Formula` EXISTS theo khoá chính |
| Đồng bộ đăng ký auto-enroll | 1 lookup mỗi học viên / mỗi buổi | 1 lookup cho cả buổi / cả học viên | Prefetch + `saveAll` |

Các ngân sách trên là assertion trong `QueryBaselineIT`, `CourseResultCalculationIT`, `AdminDashboardIT`.
Truy vấn theo lô lọc theo lớp bằng subquery (không dùng IN-list id) để tránh giới hạn 1000 phần tử
của Oracle. Thứ tự "bài làm tốt nhất" giữ nguyên ngữ nghĩa cũ (`score DESC` của Oracle xếp NULL trước).

**Pooled sequence (V33):** 29 sequence `INCREMENT BY 50 CACHE 20`, entity `allocationSize = 50`.
`hibernate.query.mutation_strategy.global_temporary.create_tables=false`: với pooled id, Hibernate 6
chuẩn bị bảng tạm `HT_*` cho HQL `INSERT ... SELECT` và sẽ tạo chúng lúc khởi động (DDL ngoài Flyway,
và treo 30 s/entity khi không có DB). Ứng dụng không dùng HQL insert.

> Sau V33, code cũ (`allocationSize = 1`) không khởi động được trên schema đã migrate (Hibernate từ
> chối khi increment lệch). Deploy migration và code cùng lúc.

## Giai đoạn 4 — đã giao

| Hạng mục | Trước | Sau |
|---|---|---|
| Upload tài liệu | `file.getBytes()` (tới 20 MB trên heap) | `setBinaryStream` từ file tạm của multipart vào BLOB |
| Download tài liệu | `byte[]` + `ByteArrayResource`, kết nối DB giữ trong lúc đọc | Stream BLOB ra file tạm trong transaction, trả `InputStreamResource`, file tự xoá khi đóng stream |
| Clone phiên bản syllabus | Tải mọi BLOB rồi `Arrays.copyOf` (2 bản trên heap) | `INSERT ... SELECT` trong Oracle, không byte nào qua JVM |
| Update full syllabus | 2 bản mỗi BLOB | 1 bản (bỏ `copyOf`); vẫn giữ trong RAM vì dòng cũ bị xoá trước khi dòng mới tồn tại |
| Giới hạn kích thước | `FileValidator` hardcode 20 MB | Lấy từ `spring.servlet.multipart.max-file-size` (`MAX_UPLOAD_SIZE`) |

Avatar (≤ 2 MB) giữ nguyên `byte[]`. Round-trip byte chính xác được kiểm chứng trên Oracle bởi
`MaterialContentStorageIT` (upload → download, clone → download).

## Còn lại

- Giai đoạn 5 (tái cấu trúc) và 6 (build/CI/docs) như bảng trạng thái.
- Update full syllabus vẫn giữ BLOB của tài liệu được giữ lại trong RAM; bỏ hẳn cần đổi cách xoá/tạo
  lại cây outline (giữ nguyên material thay vì xoá rồi tạo lại).

## Kiểm chứng

- `./mvnw test`: 311 test, 0 lỗi.
- `./mvnw verify` với `FAP_IT_DB_URL` trỏ Oracle XE 21 local: 21 IT, 0 lỗi; không có bảng `HT_*`
  nào được tạo; V32, V33 đã áp.
- Nhánh Testcontainers (Docker) chưa chạy trên máy dev (không có Docker); CI sẽ chạy nhánh này.
