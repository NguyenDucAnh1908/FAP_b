# Doi chieu mock va API that o frontend

Frontend duoc doi chieu tai `D:\AI\Antigravity\Fap_local\Fap`.

| Module/man hinh | Trang thai FE | Backend API | Hanh dong/ghi chu |
|---|---|---|---|
| Login, refresh, logout | API that | `/auth/login`, `/auth/refresh`, `/auth/logout` | Token duoc gan tu dong vao request |
| Forgot/reset/change password | API that | `/auth/forgot-password`, `/auth/reset-password`, `/auth/change-password` | Change password co UI trong Account Security; forgot/reset phu thuoc mail local |
| Dashboard theo role | API that | `/me/training-dashboard`, `/me/trainer-dashboard`, `/me/class-admin-dashboard` | Home mo cho ca 4 role; Super Admin tong hop tu API list va audit |
| User List CRUD/status | API that | `/users`, `/roles` | Co search, filter, sort va pagination |
| Profile/avatar | API that mot phan | `/users/{id}`, `/users/me/avatar`, `/users/{id}/avatar` | Avatar that cho moi role; sua profile chi mo cho role co `user:modify` vi BE chua co API self-update |
| Role Permission | API that | `/roles/permissions` GET/PUT | Ma tran quyen duoc doc va luu tu BE |
| Syllabus list/detail/full | API that | `/syllabuses/*` | General, Outline, Others va full payload deu goi BE |
| Syllabus materials | API that | `/materials/*`, topic materials | Upload/download va lien ket topic goi BE |
| Training Program | API that | `/training-programs/*` | List, detail, create, update, status va syllabus assignment |
| Class | API that | `/classes/*`, admins, trainers | List, detail, CRUD, status va phan cong nguoi dung |
| Training Calendar/Session | API that | `/training-sessions/*` | List, calendar, detail, create/update va status |
| Registration | API that | `/training-sessions/{id}/registrations`, `/me/training-registrations` | Dang ky, huy va waitlist dung BE |
| Participants | API that | `/training-sessions/{id}/participants` | Chon buoi hoc roi tai registered/waitlist |
| Attendance | API that | `/training-sessions/{id}/attendance`, `/me/attendance` | Doc va luu attendance that |
| Feedback | API that | `/training-sessions/{id}/feedback`, `/feedback-summary`, `/me/feedback` | Trainee gui/xem feedback va Admin/Trainer xem tong hop ngay trong Training Detail |
| Quiz/Question | API that | `/questions`, `/quizzes`, quiz assignments | CRUD, publish va quan ly assignment theo class/session goi BE |
| Quiz Attempt/Result | API that | `/quiz-attempts`, `/attempts`, `/attempt-summary` | BE la noi cham diem duy nhat |
| Notification | API that | `/notifications` | Danh sach va mark-read; mo duoc tu icon chuong |
| Settings | API that | `/settings` GET/PUT | General, Training, Notifications va Integrations luu vao BE |
| Audit Log | API that | `/audit-logs` | Khong con `setTimeout` gia |
| API Center | API that theo OpenAPI | Snapshot `public/openapi.json` | Dung cho cac API chua co UI chuyen biet |
| Training Reports | Mock co gan nhan preview | Chua co API bao cao tong hop | Giu preview, khong dung de xac nhan DB |
| Backup/restore | Vo hieu hoa | Chua co API | Nut disabled, khong hien toast thanh cong gia |
| Two-factor authentication | Vo hieu hoa | Chua co API | Switch disabled va co tooltip |
| Tao role moi | Vo hieu hoa | Chua co endpoint create role | Chi ma tran permission co the cap nhat |
| Certificate | Chua co UI/API | Chua co API certificate | Khong hien nut download gia |
| Training detail - materials tab | API that theo quyen | `/me/materials`, `/materials/{id}/download` | Trainee xem/tai material da duoc gan; Admin/Trainer duoc dieu huong sang syllabus vi BE khong co quan he material-session |
| Global training analytics | Mock | Chua co endpoint | Chi man Reports Preview con so lieu demo |

## File mock cu

- `QuizData.js` va `CalendarData.js` van duoc giu de tranh xoa file ngoai pham vi, nhung khong con duoc cac man hinh nghiep vu chinh import.
- `SettingsData.js` chi con duoc `ThemeContext` dung cho tuy chon giao dien local; setting nghiep vu da chuyen sang `/settings`.

## Quy tac kiem tra

- Chi tinh la API that khi DevTools Network co request toi `/api/v1/...` va thay doi co the doc lai tu BE.
- Du lieu chi thay doi trong localStorage khong duoc dung lam bang chung database.
- Trang Reports Preview khong duoc dung de doi chieu thong ke.
- API chua co UI rieng van co the chay that trong `/api-workspace`.
