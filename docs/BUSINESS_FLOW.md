# Luồng nghiệp vụ thực tế

## Module phát hiện trong code

Authentication; User/Avatar; Role/Permission; Audit Log; Settings; Syllabus/Outline/Output
Standard/Import; Learning Material/Upload/Download; Training Program; Class/Admin/Trainer;
Training Session; Registration/Waitlist; Attendance; Feedback; Notification; My Training/My
Learning; Question Bank; Quiz/Assignment/Attempt/Result.

Không có entity/API riêng cho `Course`, `Lesson`, `Exam` hoặc `Report`. Nội dung học nằm trong
Syllabus `day -> unit -> topic`; kết quả hiện tại là Quiz Attempt.

## Dependency chính

```text
Role -> User
Syllabus -> Day -> Unit -> Topic -> Material
Syllabus -> Training Program -> Class -> Session -> Registration
Class -> Class Admin
Class + Syllabus -> Trainer assignment
Registration -> Attendance -> Session Completed -> Feedback
Question -> Quiz -> Quiz Question -> Quiz Assignment -> Attempt -> Result
Notification/Audit Log được sinh từ các thao tác trên
```

## Bảng master flow

| Step | Actor | Module | Action | Dependency | API | Expected |
|---:|---|---|---|---|---|---|
| 1 | Super Admin | Auth | Đăng nhập | User Active | `POST /auth/login` | 200 + token |
| 2 | Super Admin | Role/User | Lấy role và tạo user | Role có sẵn | `GET /roles`, `POST /users` | 200/201 |
| 3 | Super Admin | Syllabus | Tạo full syllabus | Output standard hợp lệ | `POST /syllabuses/full` | Drafting, 201 |
| 4 | Super Admin | Material | Upload khi còn editable | Syllabus Drafting/Pending + topic | `POST /materials/upload` | 201 |
| 5 | Super Admin | Syllabus | Drafting -> Pending -> Active | Có topic + output standard | `PATCH /syllabuses/{id}/status` | 200 |
| 6 | Super Admin | Program | Tạo, gắn syllabus, activate | Syllabus Active | `POST /training-programs`, `PUT .../syllabuses`, `PATCH .../status` | 201/200 |
| 7 | Super Admin | Class | Tạo lớp | Program Active | `POST /classes` | Planning, 201 |
| 8 | Super Admin | Class | Gán admin/trainer | User đúng role; trainer có syllabus | `PUT /classes/{id}/admins`, `PUT .../trainers` | 200 |
| 9 | Super Admin | Class | Activate lớp | Có ngày, admin và trainer | `PATCH /classes/{id}/status` | 200 |
| 10 | Admin/Trainer | Session | Tạo lịch | Class Active; trainer đã gán; ngày trong lớp | `POST /training-sessions` | Upcoming, 201 |
| 11 | Trainee | Registration | Đăng ký | Session Upcoming; user Active | `POST .../registrations` | Registered/Waitlist |
| 12 | Trainee | Learning | Xem lớp/tài liệu | Registration Registered/Completed | `GET /me/classes`, `GET /me/materials` | 200 |
| 13 | Admin/Trainer | Quiz | Tạo question + quiz | Question hợp lệ | `POST /questions`, `POST /quizzes` | Draft, 201 |
| 14 | Admin/Trainer | Quiz | Thêm câu hỏi, publish, assign | Quiz có câu hỏi | `PUT .../questions`, `PATCH .../status`, `POST .../assignments` | 200/201 |
| 15 | Trainee | Attempt | Làm và nộp quiz | Published, còn hạn, được assign | `POST .../attempts`, `PUT .../answers`, `POST .../submit` | Score/result |
| 16 | Trainee/Admin | Attendance | Check-in hoặc nhập điểm danh | Registration Registered | `POST .../check-in`, `PUT .../attendance` | 200 |
| 17 | Admin/Trainer | Session | Complete | Mọi Registered đã có attendance | `PATCH .../status` | Completed |
| 18 | Trainee | Feedback | Gửi đánh giá | Session Completed; chưa feedback | `POST .../feedback` | 201 |
| 19 | Admin/Trainer | Result | Xem kết quả | Quiz/attempt tồn tại | `GET /quizzes/{id}/attempts` | 200 |
| 20 | Super Admin | Audit | Kiểm tra log | Các thao tác đã phát sinh | `GET /audit-logs` | 200 |

## Quy tắc trạng thái quan trọng

- Syllabus: `Drafting -> Pending -> Active`; Active chỉ có thể sang Inactive.
- Program: Planning chỉ activate khi đã gắn ít nhất một syllabus Active.
- Class: Planning chỉ activate khi có ngày, Class Admin và Trainer; sau đó có thể Closed.
- Session: Upcoming sang Completed/Canceled; Completed yêu cầu attendance đủ cho Registered.
- Quiz: Draft chỉ publish khi có câu hỏi; Published có thể Closed.
- Registration tự thành Waitlist khi `enrolledCount >= capacity`.
- Trainee chỉ tải file khi có registration hợp lệ trong program chứa syllabus của file.

## Flow lỗi chuẩn

| Trường hợp | Expected |
|---|---|
| Không token | 401 |
| Đúng token nhưng thiếu quyền | 403 `FORBIDDEN` |
| Validation DTO | 422 `VALIDATION_ERROR`, hỗ trợ `Accept-Language: vi` |
| ID không tồn tại | 404 |
| Sort field không cho phép/multipart sai | 400 |
| Email/code trùng, transition sai, dependency chưa đủ | 409 |
