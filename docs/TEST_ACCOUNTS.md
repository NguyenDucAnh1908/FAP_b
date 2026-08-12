# Tài khoản kiểm thử local

Tất cả tài khoản dưới đây chỉ được tạo khi backend chạy profile `local`. Mật khẩu chung là
`Password@123`; không dùng mật khẩu này ở môi trường thật.

| Role thực tế | Email | Trạng thái | Mục đích chính |
|---|---|---|---|
| Super Admin | `e2e.superadmin@fap.local` | Active | Toàn quyền, API Center, tạo dữ liệu |
| Class Admin | `e2e.classadmin01@fap.local` | Active | Quản lý lớp Java và lịch học |
| Class Admin | `e2e.classadmin02@fap.local` | Active | Quản lý lớp Full Stack và kiểm tra phạm vi dữ liệu |
| Trainer | `e2e.trainer01@fap.local` | Active | Java Core, điểm danh, kết quả quiz |
| Trainer | `e2e.trainer02@fap.local` | Active | Spring REST và Full Stack |
| Trainer | `e2e.trainer03@fap.local` | Active | Lớp cuối tuần/online |
| Trainee | `e2e.trainee01@fap.local` | Active | Đăng ký, tài liệu, dashboard, thông báo |
| Trainee | `e2e.trainee02@fap.local` | Active | Có kết quả quiz đạt và điểm danh Late |
| Trainee | `e2e.trainee03@fap.local` | Active | Có attempt Submitted và InProgress |
| Trainee | `e2e.trainee04@fap.local` | Active | Waitlist |
| Trainee | `e2e.trainee05@fap.local` | Active | Waitlist thứ hai để kiểm tra thứ tự |
| Trainee | `e2e.trainee06@fap.local` | Active | Registration Cancelled |
| Trainee | `e2e.trainee07@fap.local` | Active | Lớp Full Stack, không được tải tài liệu lớp Java |
| Trainee | `e2e.trainee08@fap.local` | Active | Lớp Full Stack |
| Trainee | `e2e.trainee09@fap.local` | Active | Lịch đã hoàn thành trong quá khứ |
| Trainee | `e2e.trainee10@fap.local` | Active | Lịch đã hoàn thành trong quá khứ |
| Trainee | `e2e.trainee11@fap.local` | Active | Tài khoản trống quan hệ để nhập flow mới |
| Trainee | `e2e.trainee12@fap.local` | Inactive | Login phải trả `401`; kiểm tra filter Inactive |
| Trainee | `e2e.trainee13@fap.local` | Active | Tên ngắn; pagination/search |
| Trainee | `e2e.trainee14@fap.local` | Active | Tên tiếng Việt; pagination/search |
| Trainee | `e2e.trainee15@fap.local` | Active | Tên tiếng Anh; pagination/search |
| Trainee | `e2e.trainee16@fap.local` | Active | Tên tiếng Việt có dấu |
| Trainee | `e2e.trainee17@fap.local` | Active | Số điện thoại để trống |
| Trainee | `e2e.trainee18@fap.local` | Active | Tên dài để kiểm tra hiển thị |
| Trainee | `e2e.trainee19@fap.local` | Active | Bản ghi thứ 25 cho pagination |

## Kết quả đăng nhập mong đợi

- Bốn role Active đăng nhập được qua `POST /api/v1/auth/login`.
- `e2e.trainee12@fap.local` không đăng nhập được và nhận `401`.
- Trainee gọi `GET /api/v1/users` nhận `403 FORBIDDEN`.
- Access token sống 15 phút theo cấu hình hiện tại; REST file tự lấy token mới.

## Lưu ý

- Role của dự án là `Super Admin`, `Class Admin`, `Trainer`, `Trainee`. Không có role tên chung
  `Admin`, `Student` hoặc `Teacher`.
- Hash BCrypt được tạo theo encoder hiện tại của Spring Security.
- Seed tìm user theo email và dùng `MERGE`, nên khởi động lại không tạo tài khoản trùng.
