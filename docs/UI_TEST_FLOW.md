# Luong kiem tra end-to-end tren UI

## Chuan bi

1. Chay Oracle local va backend profile `local` tai `http://localhost:8080`.
2. Chay frontend tai `http://localhost:5173`.
3. Neu chua co du lieu E2E, chay repeatable seed theo `TEST_DATA_OVERVIEW.md`.
4. Mo DevTools > Network va loc `api/v1` de xac nhan UI dang goi BE that.

Mat khau chung cua cac tai khoan local: `Password@123`.

## A. Login va dashboard theo role

### Super Admin

1. Login `e2e.superadmin@fap.local`.
2. Expected: vao `/home`; thay tong so training session, trainee, trainer, active class va recent audit.
3. Icon chuong mo `/training-calendar/notifications` va tai notification tu BE.

### Class Admin

1. Login `e2e.classadmin01@fap.local`.
2. Expected: dashboard hien assigned classes, participants, trainers, upcoming sessions cua admin nay.

### Trainer

1. Login `e2e.trainer01@fap.local`.
2. Expected: dashboard hien lich day, session hoan thanh va attendance dang cho.

### Trainee

1. Login `e2e.trainee01@fap.local`.
2. Expected: dashboard hien registration, upcoming session, attendance summary va notification.
3. Login `e2e.trainee12@fap.local`; expected `401` vi tai khoan Inactive.

## B. User va profile

1. Login Super Admin, vao **User List**.
2. Search `e2e`, page size 5; expected co it nhat 3 trang.
3. Filter status `Inactive`, email `e2e.trainee12@fap.local`; expected mot record.
4. Tao user:
   - Full name: `Nguyen Kiem Thu UI`
   - Email: `ui.trainee.<suffix>@fap.local`
   - Phone: `0904000001`
   - Password: `Password@123`
   - Date of birth: `2001-06-15`
   - Gender: `Male`
   - Role: `Trainee`
5. Save; expected toast thanh cong va doc lai duoc record tu list.
6. Vao **Profile**, upload `D:\AI\Antigravity\Fap_local\Fap\src\assets\hero.png`; expected request `POST /users/me/avatar` tra `204` va anh hien lai sau refresh.
7. Vao **Account & Security**, doi mat khau bang current/new password; sau khi xac nhan, doi lai `Password@123` de tiep tuc bo test.

## C. Syllabus va material

1. Vao **Syllabus**, search `E2E-JAVA-CORE`.
2. Filter level `Beginner`, status `Active`; mo `[E2E] Java Core Programming`.
3. Expected:
   - General: 30 attendees, 40 hours.
   - Output standards: H4SD, K6SD.
   - Assessment: Quiz 15%, Assignment 25%, Final 60%.
   - Outline co 2 day, unit va topic.
4. Tao syllabus:
   - Name: `UI Java Backend <suffix>`
   - Code: `UI-JAVA-<suffix>`
   - Technical requirements: `JDK 21, IntelliJ IDEA, Oracle XE`
5. Dien General Info, Outline, Others; luu full payload va doc lai bang `GET /syllabuses/{id}/full`.
6. Upload `test-data/files/sample-material.txt` trong man material; download lai va so sanh noi dung.

## D. Training Program va Class

1. Vao **Training Program**, tao:
   - Name: `UI Java Backend Program <suffix>`
   - Code: `UI-JAVA-PROG-<suffix>`
   - Start date: ngay mai
   - Duration: 30 days
2. Gan syllabus vua tao, luu va doc lai detail.
3. Vao **Class**, tao:
   - Name: `UI Java Backend Class <suffix>`
   - Code: `UI-JAVA-CLASS-<suffix>`
   - Program: program vua tao
   - Capacity: 30
   - Location: `Lab A-201`
4. Gan `e2e.classadmin01@fap.local` va `e2e.trainer01@fap.local`.
5. Chuyen status theo nut UI; expected transition khong hop le tra `409`, khong phai `500`.

## E. Training Calendar, registration va attendance

1. Vao **Training Calendar > Create Training**.
2. Nhap:
   - Title: `UI Spring Boot Workshop <suffix>`
   - Class: class vua tao
   - Trainer: `e2e.trainer01@fap.local`
   - Date: ngay mai
   - Time: `09:00-11:00`
   - Type: `Offline`
   - Room: `Lab A-201`
   - Capacity: 20
3. Save; expected session xuat hien trong list va calendar.
4. Vao **Participant Management**, chon session; expected registered/waitlist tai tu BE.
5. Login Trainee, vao **Student Registration**, dang ky session.
6. Mo **Training Detail**; Trainee xem/tai material duoc gan trong tab **Learning Materials**. Admin/Trainer xem danh sach participant that.
7. Login Trainer, vao session > **Mark Attendance**, danh dau `Present` va luu.
8. Chuyen session sang `Completed`, login Trainee va gui rating trong tab **Feedback**.
9. Login Admin/Trainer mo lai tab **Feedback**; expected thay so response va diem trung binh tu BE.
10. Login Trainee, vao **My Calendar** va dashboard; expected thay session va attendance cap nhat.

## F. Quiz end-to-end

1. Login Class Admin/Super Admin/Trainer, vao **Question Bank**.
2. Tao it nhat hai cau hoi co option va correct answer hop le.
3. Vao **Quiz Management**, tao quiz va chon it nhat mot question.
4. Publish quiz.
5. Mo menu cua quiz > **Manage Assignments**.
6. Chon dung mot scope: **Class** hoac **Session**, chon target va bam **Assign**. Khong gui dong thoi `classId` va `trainingSessionId`.
7. Login Trainee duoc assign, vao **My Quizzes**, start attempt, luu answer va submit.
8. Expected: diem do BE tra ve; refresh van giu result, khong phu thuoc localStorage.
9. Login Admin/Trainer, vao result summary; expected thay attempt da submit.

## G. Course Result & Completion

1. Login Super Admin/Class Admin, mo `/class/{id}/gradebook` cua lop `Active`.
2. Cau hinh chuyen can toi thieu va quiz da assign truc tiep vao class; thu quiz khong assign, expected `400`.
3. De mot session `Upcoming`, bam **Close class**; expected `409 CLASS_SESSIONS_INCOMPLETE`.
4. Hoan tat/huy toan bo session, dong quiz bat buoc, bam **Calculate**; expected moi hoc vien co `Passed`, `Failed` hoac `Withdrawn`.
5. Dieu chinh mot ket qua `Passed/Failed`, bat buoc nhap ly do; expected co lich su va ket qua tro lai chua publish.
6. Dong lop; expected khong tu dong doi tat ca enrollment thanh `Completed`.
7. Bam **Publish results** hai lan; expected lan hai khong tao notification trung.
8. Login Trainer; expected Gradebook chi doc, khong co nut policy/calculate/close/publish/adjust.
9. Login Trainee, mo **My Learning**; truoc publish khong thay ket qua, sau publish chi thay ket qua cua ban than.

## H. Settings, permission va audit

1. Login Super Admin, vao **Settings**.
2. General/Training/Notifications/Integrations: thay doi mot gia tri, save, refresh va kiem tra gia tri van con.
3. Role Permission: doi mot permission phu hop, save va doc lai; sau test phuc hoi gia tri cu.
4. Audit: expected bang goi `/audit-logs`; Backup/Restore va 2FA disabled vi BE chua co API.

## I. API Center

1. Login Super Admin va mo `/api-workspace`.
2. Chay cac step trong `API_CENTER_TEST_FLOW.md` theo thu tu.
3. Sau moi create, lay `data.id` cho request tiep theo.
4. Dung API Center cho my-learning progress va cac endpoint chua co UI rieng. Feedback da co trong Training Detail.

## J. Validation va exception

1. Chuyen ngon ngu sang `VI`.
2. Tao user voi email `not-an-email`, password `weak`, ngay sinh tuong lai.
3. Expected: `422`, message tong va details field bang tieng Viet.
4. Test duplicate email/code: expected `409`.
5. Test ID khong ton tai: expected `404`.
6. Test token thieu/het han: expected `401`; role khong du quyen: expected `403`.
7. Upload sai MIME/qua 20 MB material hoac qua 2 MB avatar: expected `400`.

## Phan chua the xac nhan bang UI that

- **Training Reports** dung API tong hop that; Super Admin xem toan he thong, Class Admin chi xem cac lop duoc giao.
- BE chua co API certificate, backup/restore, 2FA va create role.
- Training detail khong co quan he material truc tiep theo session; Trainee van xem duoc material duoc gan qua `/me/materials`, con Admin/Trainer quan ly tai syllabus/topic.
- Cac phan tren phai duoc bo qua khi danh gia du lieu DB, khong tinh mock la ket qua E2E.

## Checklist

- [ ] Login 4 role Active; Inactive nhan 401.
- [ ] Dashboard dung du lieu theo tung role.
- [ ] User search/filter/sort/pagination va avatar.
- [ ] Syllabus full/outline/Others/material.
- [ ] Program -> Class -> Session dung dependency.
- [ ] Registration -> Participants -> Materials -> Attendance -> Feedback.
- [ ] Question -> Quiz -> Assignment -> Attempt -> Result.
- [ ] Notification, Settings, Permission, Audit goi API that.
- [ ] Validation hien dung ngon ngu.
- [ ] Khong co response 500/stack trace trong full flow.
