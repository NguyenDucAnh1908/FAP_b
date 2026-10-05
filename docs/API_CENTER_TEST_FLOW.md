# Chạy full flow bằng API Center

## Chuẩn bị

1. Backend: `http://localhost:8080` với profile `local`.
2. Frontend: mở `/api-workspace` bằng `e2e.superadmin@fap.local / Password@123`.
3. Chọn `Accept-Language: vi` để kiểm tra nội dung lỗi tiếng Việt.
4. Với mỗi response tạo mới, chép `data.id` vào bước phụ thuộc tiếp theo. Không dùng ID mẫu
   trong database vì sequence có thể khác.

File `api-test-flow.http` tự lưu các ID; API Center cần chép ID thủ công.

## Flow A - Kiểm tra dataset có sẵn

### Step 01 - Login

- Endpoint: `POST /api/v1/auth/login`
- Body:

```json
{"email":"e2e.superadmin@fap.local","password":"Password@123"}
```

- Expected: `200`; API Center tự dùng access token của phiên đăng nhập FE.

### Step 02 - Pagination, search, sort, filter user

- Endpoint: `GET /api/v1/users`
- Params: `keyword=e2e`, `page=1`, `limit=5`, `sortBy=fullName`, `order=asc`.
- Expected: `200`, 5 phần tử, `pagination.totalPages >= 3`.
- Gọi lại `page=2`; ID không được trùng page 1.
- Gọi `status=Inactive&email=e2e.trainee12@fap.local`; expected đúng 1 user.

### Step 03 - Kiểm tra syllabus full

- `GET /api/v1/syllabuses?keyword=E2E-JAVA-CORE`
- Chép ID, gọi `GET /api/v1/syllabuses/{id}/full`.
- Expected: 2 day, nhiều unit/topic, output standards `H4SD`, `K6SD`.

### Step 04 - Kiểm tra program/class/session

- `GET /api/v1/training-programs?keyword=E2E&status=Active`
- `GET /api/v1/classes?keyword=E2E&status=Active&sortBy=startDate&order=asc`
- `GET /api/v1/training-sessions?keyword=E2E&status=Upcoming&sortBy=sessionDate&order=asc`
- Expected: `200`; session filter không còn lỗi Oracle CLOB.

### Step 05 - Kiểm tra quiz/result

- `GET /api/v1/quizzes?keyword=E2E&status=Published`
- Chép quiz ID, gọi `GET /api/v1/quizzes/{quizId}/attempts`.
- Expected: có attempt Submitted và InProgress trong dataset.

## Flow B - Tạo mới xuyên suốt

Dùng suffix duy nhất, ví dụ `20260809-01`, thay cho `<run>`.

### Step 06 - Lấy role và tạo ba user

1. `GET /api/v1/roles`, chép ID của Class Admin, Trainer, Trainee.
2. Gọi `POST /api/v1/users` ba lần theo body:

```json
{
  "fullName": "Flow Trainer <run>",
  "email": "flow.trainer.<run>@fap.local",
  "phone": "0903000002",
  "password": "Password@123",
  "dateOfBirth": "1988-03-20",
  "gender": "Female",
  "roleIds": [<trainerRoleId>]
}
```

Đổi tên/email/role cho Class Admin và Trainee. Expected mỗi request: `201`; chép ba user ID.

### Step 07 - Tạo full syllabus

- Endpoint: `POST /api/v1/syllabuses/full`.
- Dùng body đầy đủ ở request 06 của `api-test-flow.http`, chỉ đổi `<run>`.
- Expected: `201`, status `Drafting`; chép `data.syllabus.id` và topic ID đầu tiên.

### Step 08 - Upload và download

- Endpoint: `POST /api/v1/materials/upload`.
- Params: `syllabusId=<syllabusId>`, `topicId=<topicId>`.
- File: `test-data/files/sample-material.txt`, MIME `text/plain`.
- Expected: `201`; chép material ID.
- `GET /api/v1/materials/{materialId}/download`: `200`, tên và nội dung giữ nguyên.

### Step 09 - Activate syllabus

1. `PATCH /api/v1/syllabuses/{id}/status`, body `{"status":"Pending"}`.
2. Gọi lại với `{"status":"Active"}`.
3. Expected: hai request `200`.

### Step 10 - Tạo và activate program

1. `POST /api/v1/training-programs`:

```json
{"name":"Flow Java Program <run>","duration":"12 weeks","totalHours":240,"version":"v1.0"}
```

2. `PUT /api/v1/training-programs/{programId}/syllabuses`:

```json
{"syllabuses":[{"syllabusId":<syllabusId>,"sortOrder":1}]}
```

3. `PATCH /api/v1/training-programs/{programId}/status`, body `{"status":"Active"}`.
4. Expected: `201`, `200`, `200`.

### Step 11 - Tạo và activate class

1. `POST /api/v1/classes` với program ID, ngày bắt đầu hôm nay, kết thúc sau 90 ngày.
2. `PUT /api/v1/classes/{classId}/admins`, body `{"userIds":[<classAdminId>]}`.
3. `PUT /api/v1/classes/{classId}/trainers`:

```json
{"trainers":[{"userId":<trainerId>,"syllabusId":<syllabusId>}]}
```

4. `PATCH /api/v1/classes/{classId}/status`, body `{"status":"Active"}`.
5. Expected: class từ Planning sang Active.

### Step 12 - Tạo session

- `POST /api/v1/training-sessions` với ngày trong khoảng class:

```json
{
  "classId": <classId>,
  "title": "Flow REST Workshop <run>",
  "description": "Session E2E",
  "trainerId": <trainerId>,
  "room": "A101",
  "sessionDate": "<yyyy-MM-dd>",
  "startTime": "<yyyy-MM-dd>T09:00:00",
  "endTime": "<yyyy-MM-dd>T11:00:00",
  "sessionType": "Hybrid",
  "meetingLink": "https://meet.example.com/flow-<run>",
  "capacity": 20
}
```

- Expected: `201`, `Upcoming`; chép session ID.

### Step 13 - Tạo và assign quiz

1. `POST /api/v1/questions`: xem body request 18 trong `.http`; chép question ID.
2. `POST /api/v1/quizzes`: xem request 19; chép quiz ID.
3. `PUT /api/v1/quizzes/{quizId}/questions` với question ID, `sortOrder=1`, `points=1`.
4. `PATCH /api/v1/quizzes/{quizId}/status`, `{"status":"Published"}`.
5. `POST /api/v1/quizzes/{quizId}/assignments`:
   `{"trainingSessionId":<sessionId>}`.
6. Expected: quiz Published và được gán cho session. API chỉ chấp nhận đúng một trong hai field `classId` hoặc `trainingSessionId` cho mỗi assignment.

### Step 14 - Trainee hoàn thành flow

Đăng nhập user Trainee vừa tạo và dùng token đó:

1. `POST /training-sessions/{sessionId}/registrations`: Registered.
2. `GET /me/classes`: thấy class vừa tạo.
3. `GET /materials/{materialId}/download`: `200` vì đã đăng ký đúng program.
4. `GET /quizzes/assigned`: thấy quiz.
5. `POST /quizzes/{quizId}/attempts`: chép attempt ID.
6. `PUT /quiz-attempts/{attemptId}/answers`: gửi answer như file `.http`.
7. `POST /quiz-attempts/{attemptId}/submit`: nhận score/passed.
8. `GET /quiz-attempts/{attemptId}/review`: xem đáp án.
9. `POST /training-sessions/{sessionId}/check-in`: attendance Present/QR.

### Step 15 - Complete và feedback

1. Đổi lại token Super Admin.
2. `PATCH /training-sessions/{sessionId}/status`, body `{"status":"Completed"}`.
3. Đổi token Trainee; `POST /training-sessions/{sessionId}/feedback` với rating 1–5.
4. Expected: registration thành Completed, feedback `201`.

## Flow C - Exception

| Case | Cách gọi | Expected |
|---|---|---|
| Không token | `GET /users` | 401 |
| Trainee xem toàn bộ user | `GET /users` bằng token Trainee | 403 |
| DTO sai | Invalid request trong `.http` | 422 + chi tiết tiếng Việt |
| ID giả | `GET /users/999999999` | 404 |
| Email trùng | Tạo lại email đã có | 409 |
| Sort field lạ | `GET /users?sortBy=notAField` | 400 |
| File sai MIME | Upload `invalid-file-type.exe` khi syllabus Drafting | 400 |
| State lùi | Completed -> Upcoming | 409 |
| Tải file không được assign | Trainee khác program tải material | 403 |
