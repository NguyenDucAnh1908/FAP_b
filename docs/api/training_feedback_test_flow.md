# Training Feedback End-to-End Test Flow

Use this checklist to create enough data to test training feedback from Swagger or Postman.

Base assumptions:
- App is running on `http://localhost:8080`.
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`.
- Super Admin login:

```json
{
  "email": "admin@fap.local",
  "password": "password"
}
```

After login, use:

```text
Authorization: Bearer <accessToken>
Accept-Language: vi
```

## 1. Find Role IDs

Call:

```http
GET /api/v1/roles
```

Record these IDs:

```text
CLASS_ADMIN_ROLE_ID=<id of Class Admin>
TRAINER_ROLE_ID=<id of Trainer>
TRAINEE_ROLE_ID=<id of Trainee>
```

Do not hard-code role IDs unless you know your local seed values.

## 2. Create Test Users

Create Class Admin:

```http
POST /api/v1/users
```

```json
{
  "fullName": "Feedback Class Admin",
  "email": "feedback.classadmin@fap.local",
  "phone": "+84900000001",
  "password": "Password@123",
  "dateOfBirth": "1995-01-01",
  "gender": "Male",
  "avatarUrl": null,
  "roleIds": [CLASS_ADMIN_ROLE_ID]
}
```

Create Trainer:

```json
{
  "fullName": "Feedback Trainer",
  "email": "feedback.trainer@fap.local",
  "phone": "+84900000002",
  "password": "Password@123",
  "dateOfBirth": "1993-01-01",
  "gender": "Female",
  "avatarUrl": null,
  "roleIds": [TRAINER_ROLE_ID]
}
```

Create Trainee:

```json
{
  "fullName": "Feedback Trainee",
  "email": "feedback.trainee@fap.local",
  "phone": "+84900000003",
  "password": "Password@123",
  "dateOfBirth": "2000-01-01",
  "gender": "Male",
  "avatarUrl": null,
  "roleIds": [TRAINEE_ROLE_ID]
}
```

Record:

```text
CLASS_ADMIN_USER_ID=<created id>
TRAINER_USER_ID=<created id>
TRAINEE_USER_ID=<created id>
```

## 3. Create and Publish a Syllabus

Create syllabus:

```http
POST /api/v1/syllabuses
```

```json
{
  "name": "Feedback Test Syllabus",
  "code": "FB-SYL-001",
  "version": "1.0",
  "levelName": "Beginner",
  "attendees": 30,
  "duration": "1 day",
  "technicalRequirements": "Laptop",
  "courseObjectives": "Complete a feedback test flow.",
  "rules": "Join on time.",
  "timeAllocAssignmentLab": 40,
  "timeAllocConceptLecture": 40,
  "timeAllocGuideReview": 10,
  "timeAllocTestQuiz": 10,
  "assessQuizPct": 20,
  "assessAssignmentPct": 30,
  "assessFinalPct": 50,
  "assessmentText": "Feedback test assessment"
}
```

Record:

```text
SYLLABUS_ID=<created id>
```

Add output standards:

```http
PUT /api/v1/syllabuses/{SYLLABUS_ID}/output-standards
```

```json
{
  "standards": ["H4SD"]
}
```

Create day:

```http
POST /api/v1/syllabuses/{SYLLABUS_ID}/days
```

```json
{
  "dayNumber": 1,
  "sortOrder": 1
}
```

Record:

```text
DAY_ID=<created id>
```

Create unit:

```http
POST /api/v1/syllabuses/{SYLLABUS_ID}/days/{DAY_ID}/units
```

```json
{
  "name": "Feedback Unit",
  "sortOrder": 1
}
```

Record:

```text
UNIT_ID=<created id>
```

Create topic:

```http
POST /api/v1/syllabuses/{SYLLABUS_ID}/units/{UNIT_ID}/topics
```

```json
{
  "name": "Feedback Topic",
  "outputStandard": "H4SD",
  "online": false,
  "durationMinutes": 180,
  "status": "Active",
  "sortOrder": 1
}
```

Submit syllabus:

```http
PATCH /api/v1/syllabuses/{SYLLABUS_ID}/status
```

```json
{
  "status": "Pending"
}
```

Publish syllabus:

```json
{
  "status": "Active"
}
```

## 4. Create and Publish a Training Program

Create program:

```http
POST /api/v1/training-programs
```

```json
{
  "name": "Feedback Test Program",
  "duration": "1 day",
  "totalHours": 3,
  "version": "1.0"
}
```

Record:

```text
PROGRAM_ID=<created id>
```

Attach syllabus:

```http
PUT /api/v1/training-programs/{PROGRAM_ID}/syllabuses
```

```json
{
  "syllabuses": [
    {
      "syllabusId": SYLLABUS_ID,
      "sortOrder": 1
    }
  ]
}
```

Publish program:

```http
PATCH /api/v1/training-programs/{PROGRAM_ID}/status
```

```json
{
  "status": "Active"
}
```

## 5. Create and Activate a Class

Create class:

```http
POST /api/v1/classes
```

```json
{
  "name": "Feedback Test Class",
  "classCode": "FB-CLASS-001",
  "trainingProgramId": PROGRAM_ID,
  "location": "HCM",
  "locationDetail": "Room F01",
  "fsu": "FAP",
  "classTime": "08:30-11:30",
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "duration": "1 month"
}
```

Record:

```text
CLASS_ID=<created id>
```

Assign class admin:

```http
PUT /api/v1/classes/{CLASS_ID}/admins
```

```json
{
  "userIds": [CLASS_ADMIN_USER_ID]
}
```

Assign trainer:

```http
PUT /api/v1/classes/{CLASS_ID}/trainers
```

```json
{
  "trainers": [
    {
      "userId": TRAINER_USER_ID,
      "syllabusId": SYLLABUS_ID
    }
  ]
}
```

Activate class:

```http
PATCH /api/v1/classes/{CLASS_ID}/status
```

```json
{
  "status": "Active"
}
```

## 6. Create Training Session

Create session:

```http
POST /api/v1/training-sessions
```

```json
{
  "classId": CLASS_ID,
  "title": "Feedback Test Session",
  "description": "Session used to test feedback flow.",
  "trainerId": TRAINER_USER_ID,
  "room": "Room F01",
  "sessionDate": "2026-06-02",
  "startTime": "2026-06-02T08:30:00",
  "endTime": "2026-06-02T11:30:00",
  "sessionType": "Offline",
  "meetingLink": null,
  "capacity": 30
}
```

Record:

```text
SESSION_ID=<created id>
```

## 7. Register Trainee

Login as trainee:

```json
{
  "email": "feedback.trainee@fap.local",
  "password": "Password@123"
}
```

Use trainee token and call:

```http
POST /api/v1/training-sessions/{SESSION_ID}/registrations
```

Expected registration status:

```text
Registered
```

## 8. Add Attendance and Complete Session

Switch back to Super Admin, Trainer, or assigned Class Admin token.

Upsert attendance:

```http
PUT /api/v1/training-sessions/{SESSION_ID}/attendance
```

```json
{
  "records": [
    {
      "userId": TRAINEE_USER_ID,
      "status": "Present",
      "checkedInAt": "2026-06-02T08:35:00",
      "checkInMethod": "Manual",
      "correctionReason": null
    }
  ]
}
```

Complete session:

```http
PATCH /api/v1/training-sessions/{SESSION_ID}/status
```

```json
{
  "status": "Completed"
}
```

## 9. Submit Feedback

Login as trainee again and call:

```http
POST /api/v1/training-sessions/{SESSION_ID}/feedback
```

```json
{
  "ratingContent": 5,
  "ratingTrainer": 5,
  "ratingOrganization": 4,
  "comment": "Session was practical and easy to follow."
}
```

Expected:

```text
201 Created
```

Calling the same endpoint again should return:

```text
409 FEEDBACK_ALREADY_SUBMITTED
```

## 10. Verify Feedback Views

Trainee self-history:

```http
GET /api/v1/me/feedback?page=1&limit=20
```

Trainer/Class Admin/Super Admin summary:

```http
GET /api/v1/training-sessions/{SESSION_ID}/feedback-summary
```

Expected summary:

```json
{
  "success": true,
  "data": {
    "trainingSessionId": SESSION_ID,
    "feedbackCount": 1,
    "averageContentRating": 5.0,
    "averageTrainerRating": 5.0,
    "averageOrganizationRating": 4.0,
    "overallAverageRating": 4.666666666666667
  },
  "message": null
}
```

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `401` | Missing or expired Bearer token. |
| `403` | Logged-in user does not have permission or ownership scope for the endpoint. |
| `409 FEEDBACK_SESSION_NOT_COMPLETED` | Session must be marked `Completed` first. |
| `409 TRAINING_SESSION_ATTENDANCE_REQUIRED` | Complete session requires attendance for all registered trainees. |
| `409 FEEDBACK_REGISTRATION_REQUIRED` | Trainee did not register for the session. |
| `409 FEEDBACK_ALREADY_SUBMITTED` | User already submitted feedback for that session. |
