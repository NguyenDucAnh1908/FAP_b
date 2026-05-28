# Dashboard APIs

This document covers the personal dashboard endpoints under `/api/v1/me`.

All endpoints require a valid JWT access token.

Swagger test flow:

1. Login with `POST /api/v1/auth/login`.
2. Copy `accessToken` from the response.
3. In Swagger UI, click `Authorize`.
4. Enter `Bearer <accessToken>`.
5. Optional: set `Accept-Language` to `vi` or `en`.
6. Call the dashboard endpoint for the current user's role.

Postman header example:

```text
Authorization: Bearer <accessToken>
Accept-Language: vi
```

## Endpoint Summary

| Role / Context | Endpoint | Purpose |
|---|---|---|
| Trainee / any user with registrations | `GET /api/v1/me/training-dashboard` | View own learning registration and attendance summary. |
| Trainer | `GET /api/v1/me/trainer-dashboard` | View own teaching workload and pending attendance work. |
| Class Admin | `GET /api/v1/me/class-admin-dashboard` | View classes managed by the current class admin. |

Important scope rule:
- These are personal dashboard endpoints.
- The client must not send `userId`, `trainerId`, or `adminId`.
- The backend always uses the authenticated user's principal id.

## `GET /api/v1/me/training-dashboard`

Use this endpoint for a trainee or any user who has registered for training sessions.

Data prerequisites:
- User has a valid account and can login.
- User has at least one row in `training_registrations`.
- To populate attendance summary, user has rows in `attendance_records`.

Sample response:

```json
{
  "success": true,
  "data": {
    "registeredSessions": 2,
    "upcomingSessions": 1,
    "completedSessions": 1,
    "waitlistedSessions": 0,
    "attendanceSummary": {
      "present": 1,
      "late": 0,
      "absent": 0
    },
    "nextSessions": [
      {
        "trainingSessionId": 1001,
        "title": "Java Foundation - Day 1",
        "description": "Introduction and setup",
        "status": "Upcoming",
        "sessionType": "Offline",
        "sessionDate": "2026-06-01",
        "startTime": "2026-06-01T08:30:00",
        "endTime": "2026-06-01T11:30:00",
        "room": "Room A101",
        "meetingLink": null,
        "capacity": 30,
        "enrolledCount": 18,
        "classId": 1001,
        "className": "Java Backend K01",
        "classCode": "JAVA-BE-K01",
        "trainerId": 1004,
        "trainerFullName": "Trainer User",
        "trainerEmail": "trainer@fap.local",
        "registrationId": 1001,
        "registrationStatus": "Registered",
        "registeredAt": "2026-05-28T09:00:00",
        "cancelledAt": null,
        "completedAt": null
      }
    ],
    "recentAttendance": [
      {
        "attendanceId": 1001,
        "attendanceStatus": "Present",
        "checkedInAt": "2026-05-20T08:35:00",
        "checkInMethod": "Manual",
        "correctionReason": null,
        "createdAt": "2026-05-20T08:35:00",
        "updatedAt": "2026-05-20T08:35:00",
        "trainingSessionId": 1000,
        "trainingSessionTitle": "Java Foundation - Orientation",
        "trainingSessionStatus": "Completed",
        "sessionType": "Offline",
        "sessionDate": "2026-05-20",
        "startTime": "2026-05-20T08:30:00",
        "endTime": "2026-05-20T11:30:00",
        "room": "Room A101",
        "meetingLink": null,
        "classId": 1001,
        "className": "Java Backend K01",
        "classCode": "JAVA-BE-K01",
        "trainerId": 1004,
        "trainerFullName": "Trainer User",
        "trainerEmail": "trainer@fap.local"
      }
    ]
  },
  "message": null
}
```

## `GET /api/v1/me/trainer-dashboard`

Use this endpoint for the logged-in trainer.

Data prerequisites:
- User has role `Trainer`.
- User is assigned in `class_trainers`.
- User is assigned as `trainer` on one or more `training_sessions`.
- Pending attendance appears when an upcoming session on or before today has registered participants without complete attendance records.

Sample response:

```json
{
  "success": true,
  "data": {
    "assignedClasses": 2,
    "upcomingSessions": 3,
    "completedSessions": 4,
    "pendingAttendanceSessions": 1,
    "nextSessions": [
      {
        "id": 1001,
        "classId": 1001,
        "className": "Java Backend K01",
        "classCode": "JAVA-BE-K01",
        "title": "Java Foundation - Day 1",
        "description": "Introduction and setup",
        "trainerId": 1004,
        "trainerFullName": "Trainer User",
        "trainerEmail": "trainer@fap.local",
        "room": "Room A101",
        "sessionDate": "2026-06-01",
        "startTime": "2026-06-01T08:30:00",
        "endTime": "2026-06-01T11:30:00",
        "sessionType": "Offline",
        "meetingLink": null,
        "capacity": 30,
        "enrolledCount": 18,
        "status": "Upcoming",
        "createdAt": "2026-05-28T08:00:00",
        "updatedAt": "2026-05-28T08:00:00"
      }
    ],
    "recentCompletedSessions": []
  },
  "message": null
}
```

## `GET /api/v1/me/class-admin-dashboard`

Use this endpoint for the logged-in class admin.

Data prerequisites:
- User has role `Class Admin`.
- User is assigned in `class_admins`.
- Assigned classes have trainers, sessions, and registrations if you want all counters to be non-zero.

Sample response:

```json
{
  "success": true,
  "data": {
    "assignedClasses": 2,
    "activeClasses": 1,
    "planningClasses": 1,
    "upcomingSessions": 3,
    "pendingAttendanceSessions": 1,
    "totalTrainers": 2,
    "totalParticipants": 18,
    "classesStartingSoon": [
      {
        "id": 1001,
        "name": "Java Backend K01",
        "classCode": "JAVA-BE-K01",
        "trainingProgramId": 1001,
        "trainingProgramName": "Java Backend Foundation",
        "status": "Active",
        "location": "HCM",
        "locationDetail": "Room A101",
        "fsu": "FHN",
        "classTime": "Mon-Wed-Fri 08:30",
        "startDate": "2026-06-01",
        "endDate": "2026-07-01",
        "duration": "1 month",
        "createdBy": 1000,
        "updatedBy": 1000,
        "createdAt": "2026-05-28T08:00:00",
        "updatedAt": "2026-05-28T08:00:00"
      }
    ],
    "recentSessions": []
  },
  "message": null
}
```

## Minimal Data Setup for Manual Testing

Create or reuse this data in order:

1. Login as Super Admin.
2. Create active syllabus and active training program.
3. Create a planning class from the active training program.
4. Assign at least one Class Admin and one Trainer to the class.
5. Activate the class.
6. Create one or more upcoming training sessions for the assigned trainer.
7. Login as a trainee user and register for a session.
8. Upsert attendance for completed or current sessions.
9. Login as each role and call the matching dashboard endpoint.

Expected quick checks:
- Trainee dashboard should show own registrations and attendance only.
- Trainer dashboard should show only sessions where the logged-in user is trainer.
- Class Admin dashboard should show only classes where the logged-in user is assigned as class admin.
