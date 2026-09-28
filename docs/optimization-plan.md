# Kế hoạch tối ưu hệ thống

Branch: `feature/optimization-phase-0-1` (tách từ `feature/production-readiness-round3`).
Nguồn: audit ngày 2026-09-28 (DB/JPA, cấu trúc code, cấu hình/test/build). Không thêm scope ngoài
`docs/07_scope_freeze.md`.

## Trạng thái

- ✅ xong và đã kiểm chứng
- ⬜ chưa làm
- ❓ chờ quyết định

| Giai đoạn | Nội dung | Trạng thái |
|---|---|---|
| 0 | Lưới an toàn: integration test trên Oracle thật, JaCoCo, đo baseline | ✅ |
| 1 | Quick wins: batching, index, sửa query lỗi/nặng, config, 2 lỗi bảo mật nhỏ | ✅ |
| 2 | Hot path mỗi request: cache permission, audit/notification/mail sau commit | ⬜ ❓ |
| 3 | Khử N+1 ở service nặng (CourseResult, MyLearning, ClassEnrollment, dashboard), pooled sequence | ⬜ ❓ |
| 4 | File: stream download, clone BLOB phía DB, StorageService | ⬜ ❓ |
| 5 | Tái cấu trúc: tách god service, gom logic trùng, i18n, code chết | ⬜ |
| 6 | Build/CI/docs: loại `db/seed` khỏi jar prod, build-info, dọn `docs/.claude` | ⬜ |

## Quyết định cần chốt trước Giai đoạn 2–4

1. **Audit khi transaction rollback**: hiện `REQUIRES_NEW` nên thao tác thất bại vẫn có log. Chuyển
   sang ghi sau commit thì mất log thao tác thất bại.
2. **Lưu file**: giữ BLOB trong Oracle (chỉ tối ưu streaming) hay chuyển object storage.
3. **Pooled sequence** (`INCREMENT BY 50`): id sẽ nhảy cóc.
4. **Chạy nhiều instance?** Nếu có, cache permission và rate limit cần Redis hoặc TTL ngắn.

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

## Baseline còn lại cho Giai đoạn 3

Đo bằng `QueryBaselineIT` trên dữ liệu seed local (statements | entities):

| Read path | Statements | Entities |
|---|---:|---:|
| `adminDashboard.getDashboard` | 27 | 5 |
| `courseResult.list` (5 kết quả) | 12 | 17 |
| `myLearning.learningContent` | 23 | 51 |
| `myLearning.progress` | 15 | 47 |

Batch fetch không làm giảm các số này vì N+1 ở đây là lời gọi repository trong vòng lặp, không phải
lazy loading. Khi tối ưu path nào, chuyển số của path đó thành assertion trong `*IT` của module.

## Kiểm chứng

- `./mvnw test`: 306 test, 0 lỗi.
- `./mvnw verify` với `FAP_IT_DB_URL` trỏ Oracle XE 21 local: 14 IT, 0 lỗi.
- Nhánh Testcontainers (Docker) chưa chạy trên máy dev (không có Docker); CI sẽ chạy nhánh này.
