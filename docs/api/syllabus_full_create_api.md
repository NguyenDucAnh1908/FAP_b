# Syllabus Full Create API

Creates a complete draft syllabus in one transaction.

```http
POST /api/v1/syllabuses/full
Authorization: Bearer <accessToken>
Accept-Language: vi
Content-Type: application/json
```

Updates/replaces a full editable syllabus in one transaction.

```http
PUT /api/v1/syllabuses/{id}/full
Authorization: Bearer <accessToken>
Accept-Language: vi
Content-Type: application/json
```

Loads a full syllabus for view/edit screens.

```http
GET /api/v1/syllabuses/{id}/full
Authorization: Bearer <accessToken>
Accept-Language: vi
```

Creates a new editable version from an `Active` or `Inactive` syllabus.

```http
POST /api/v1/syllabuses/{id}/clone
Authorization: Bearer <accessToken>
Accept-Language: vi
Content-Type: application/json

{
  "name": "IS SOFTWARE",
  "code": "HIS_SW_V2",
  "version": "v2.0"
}
```

Rules:
- Requires `syllabus:create`.
- Creates syllabus in `Drafting`.
- `POST /full` creates syllabus in `Drafting`.
- `PUT /{id}/full` replaces general info, output standards, days, units, topics, and material links for an editable syllabus.
- `GET /{id}/full` returns general info, output standards, days, units, topics, and topic material links.
- If any nested item fails validation or duplicate sort rules, the whole request rolls back.
- Every topic `outputStandard` must exist in root `outputStandards`.
- `days`, `units`, and `topics` may be empty arrays while saving a draft tab by tab.
- Time allocation total must be `100`.
- Assessment total must be `100`.
- `POST /{id}/clone` requires `syllabus:create` and accepts only an `Active` or `Inactive` source.
- The cloned syllabus always starts in `Drafting`; the source syllabus is unchanged.
- General info, output standards, outline, material metadata, external links, and available BLOB content are copied.
- If an old internal material no longer has BLOB content, the cloned material is marked unavailable so an administrator can upload it again.

Example:

```json
{
  "name": "ASP.NET MVC",
  "code": "NET_MVC",
  "version": "v1.0",
  "levelName": "Beginner",
  "attendees": 30,
  "duration": "3 days",
  "technicalRequirements": "SQL Server 2019, Git, Docker",
  "courseObjectives": "Build ASP.NET MVC applications with layered architecture.",
  "rules": "Attend all sessions and complete assignments.",
  "timeAllocAssignmentLab": 50,
  "timeAllocConceptLecture": 30,
  "timeAllocGuideReview": 10,
  "timeAllocTestQuiz": 10,
  "assessQuizPct": 15,
  "assessAssignmentPct": 15,
  "assessFinalPct": 70,
  "assessmentText": "Quiz 15%, Assignment 15%, Final 70%",
  "outputStandards": ["H4SD", "K6SD"],
  "days": [
    {
      "dayNumber": 1,
      "sortOrder": 1,
      "units": [
        {
          "name": "MVC Fundamentals",
          "sortOrder": 1,
          "topics": [
            {
              "name": "Routing and Controllers",
              "outputStandard": "H4SD",
              "online": true,
              "durationMinutes": 90,
              "status": "Active",
              "sortOrder": 1,
              "materials": [
                {
                  "fileName": "Routing slides",
                  "fileUrl": "https://drive.google.com/file/d/example/view",
                  "fileSize": 1024,
                  "contentType": "link"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```
