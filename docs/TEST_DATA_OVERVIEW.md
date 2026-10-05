# Tong quan du lieu E2E

## Pham vi

Dataset nam tai `src/main/resources/db/seed/R__seed_e2e_test_data.sql`.
Thu muc `db/seed` chi duoc Flyway nap khi chay profile `local`; profile `prod` khong quet thu muc nay.

## So luong dataset E2E

| Du lieu | So luong | Ghi chu |
|---|---:|---|
| User | 25 | 1 Super Admin, 2 Class Admin, 3 Trainer, 19 Trainee |
| Syllabus | 5 | General info, outline va output standards |
| Training Program | 5 | 2 Active, 2 Planning, 1 Inactive |
| Class | 6 | Active, Planning, Closed |
| Training Session | 10 | Qua khu, gan hien tai va tuong lai |
| Registration | 14 | Registered, Waitlist, Completed, Cancelled |
| Attendance | 5 | Present, Late, Absent |
| Feedback | 3 | Gan voi session Completed |
| Material metadata | 2 | Link ngoai; upload file that tao qua API |
| Question | 6 | Single/multiple, Easy/Medium/Hard |
| Quiz | 3 | Draft, Published, Closed |
| Quiz Attempt/Result | 3 | 2 Submitted, 1 InProgress |
| Notification | 3 | Read va unread |

Database local co the con dataset V22, vi vay tong record thuc te co the lon hon bang tren.

## Trang thai da phu

- User: `Active`, `Inactive`.
- Syllabus: `Drafting`, `Pending`, `Active`, `Inactive`.
- Topic: `Active`, `Inactive`.
- Program: `Planning`, `Active`, `Inactive`.
- Class: `Planning`, `Active`, `Closed`.
- Session: `Upcoming`, `Completed`, `Canceled`.
- Registration: `Registered`, `Waitlist`, `Completed`, `Cancelled`.
- Attendance: `Present`, `Late`, `Absent`; method `Manual`, `QR`.
- Quiz: `Draft`, `Published`, `Closed`.
- Attempt: `InProgress`, `Submitted`.

## Du lieu cho search/filter/pagination

- `GET /users?keyword=e2e&limit=10` co it nhat 25 account, du ba trang.
- Syllabus co level `Beginner`, `Intermediate`, `Advanced`, `All levels`.
- Session co ba status va nhieu ngay de loc `fromDate`, `toDate`, `status`.
- Co ten ngan, ten dai, tieng Viet, tieng Anh va optional field null.
- Tim dataset bang email/code bat dau `e2e`, `E2E` hoac ten bat dau `[E2E]`.

## Cach seed

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:JAVA_HOME="C:\Users\nguye\.jdks\ms-21.0.11"
.\mvnw.cmd spring-boot:run
```

Flyway chay V28 de dong bo sequence, sau do chay repeatable seed. Seed dung `MERGE`/natural key nen co the chay lap lai ma khong tao duplicate.

## Cach reset DEV

1. Dung backend local de khong co request dang ghi DB.
2. Chay:

```powershell
sqlplus fap/<local-password>@localhost:1521/XEPDB1 '@scripts/reset-dev-data.sql'
```

3. Khoi dong lai backend profile `local`; Flyway se tao lai dataset.

Script chi chay khi co marker `DEV_SEED/dataset_version=e2e-v1`. No chi xoa dataset E2E va cac record tao tu flow co prefix `FLOW-`, `API Flow`, `UI-` hoac email `flow.*`/`ui.trainee.*`; khong reset sequence va khong xoa dataset V22.

## Ba cach chay thu

- Tu dong: khoi dong backend local, sau do chay:

```powershell
.\scripts\verify-e2e-flow.ps1 -BaseUrl "http://localhost:8080/api/v1"
```

Script tu tao ID theo thoi gian, khong hard-code primary key, va kiem tra full flow cung cac response `400`, `401`, `403`, `404`, `409`, `422`.
- API: chay `docs/api-test-flow.http` hoac cac step trong `API_CENTER_TEST_FLOW.md`.
- UI: chay `UI_TEST_FLOW.md`. Cac man Program, Class, Calendar, Quiz, Settings, Dashboard va Profile da noi API that; chi cac muc duoc ghi ro preview/disabled la chua co BE.
