# My Learning APIs

Self-service endpoints for trainee learning overview. All endpoints require Bearer token authentication.

## List My Classes

```http
GET /api/v1/me/classes?page=1&limit=20&keyword=java
```

Rules:
- Returns classes where the current user has at least one `Registered` or `Completed` training registration.
- `keyword` searches class name, class code, location, and training program name.

Response shape:

```json
{
  "success": true,
  "data": [
    {
      "id": 1001,
      "name": "Java Backend Foundation",
      "classCode": "JAVA-BE-01",
      "trainingProgramId": 501,
      "trainingProgramName": "Backend Foundation Program",
      "status": "Active",
      "location": "Online",
      "locationDetail": "Google Meet",
      "fsu": "FAP",
      "classTime": "Mon/Wed/Fri 19:00",
      "startDate": "2026-06-01",
      "endDate": "2026-07-15",
      "duration": "6 weeks",
      "createdBy": 1,
      "updatedBy": 1,
      "createdAt": "2026-05-24T10:00:00",
      "updatedAt": "2026-05-24T10:00:00"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 1
  }
}
```

## Get My Class Detail

```http
GET /api/v1/me/classes/{classId}
```

Rules:
- Returns `404` if the class is not assigned to the current user through an eligible registration.
- Includes ordered syllabuses from the class training program.

Response data:

```json
{
  "classInfo": {
    "id": 1001,
    "name": "Java Backend Foundation",
    "classCode": "JAVA-BE-01",
    "trainingProgramId": 501,
    "trainingProgramName": "Backend Foundation Program",
    "status": "Active"
  },
  "syllabuses": [
    {
      "syllabusId": 2001,
      "name": "Java Core",
      "code": "JAVA-CORE",
      "version": "v1.0",
      "status": "Active",
      "levelName": "Beginner",
      "duration": "3 days",
      "sortOrder": 1
    }
  ]
}
```

## Get My Class Learning Content

```http
GET /api/v1/me/classes/{classId}/learning-content?keyword=oop
```

Rules:
- Returns the class, ordered syllabuses, the current user's sessions in that class, assigned material links, and currently available published quizzes.
- Material search uses `keyword` across file name, link, content type, topic, syllabus name, and syllabus code.
- Quiz visibility follows current assignment rules: assigned to the class or to one of the user's registered/completed sessions in that class.

Response data:

```json
{
  "classInfo": {},
  "syllabuses": [],
  "sessions": [],
  "materials": [
    {
      "id": 3001,
      "syllabusId": 2001,
      "syllabusName": "Java Core",
      "syllabusCode": "JAVA-CORE",
      "topicId": 4001,
      "topicName": "OOP Basics",
      "fileName": "OOP Slides",
      "fileUrl": "https://drive.google.com/file/d/example/view",
      "fileSize": null,
      "contentType": "link",
      "uploadedBy": 1,
      "uploadedAt": "2026-05-24T10:00:00"
    }
  ],
  "quizzes": [
    {
      "id": 5001,
      "title": "Java OOP Quiz",
      "description": "Basic OOP quiz",
      "durationMinutes": 30,
      "passingScore": 70,
      "maxAttempts": 3,
      "category": "Java",
      "openDate": "2026-06-01",
      "closeDate": "2026-06-30",
      "questionCount": 10,
      "attemptCount": 1,
      "remainingAttempts": 2,
      "latestAttemptId": 6001,
      "latestAttemptStatus": "Submitted",
      "latestScore": 80,
      "latestPassed": true
    }
  ]
}
```

## Get My Class Progress

```http
GET /api/v1/me/classes/{classId}/progress
```

Rules:
- Returns `404` if the class is not assigned to the current user through a `Registered` or `Completed` registration.
- Session progress is calculated from the current user's eligible registrations in the class.
- Attendance progress is calculated from attendance records for the current user in the class.
- Material progress counts assigned material links from syllabuses attached to the class training program.
- Quiz progress counts currently available published quizzes assigned to the class or to one of the user's eligible sessions in that class.

Response data:

```json
{
  "classInfo": {
    "id": 1001,
    "name": "Java Backend Foundation",
    "classCode": "JAVA-BE-01",
    "trainingProgramId": 501,
    "trainingProgramName": "Backend Foundation Program",
    "status": "Active"
  },
  "sessions": {
    "total": 10,
    "completed": 4,
    "upcoming": 5,
    "canceled": 1
  },
  "attendance": {
    "present": 3,
    "late": 1,
    "absent": 0
  },
  "materials": {
    "total": 12
  },
  "quizzes": {
    "assigned": 5,
    "attempted": 3,
    "passed": 2,
    "remaining": 2,
    "latestAttemptId": 6001,
    "latestScore": 80,
    "latestPassed": true
  }
}
```
