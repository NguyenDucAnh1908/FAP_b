# Learning Material APIs

Learning Material APIs manage metadata/link materials attached to syllabus topics and expose assigned materials to trainees.

All endpoints require a valid JWT access token.

## Endpoint Summary

| Method | Endpoint | Permission | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/materials` | `learning_material:view` | List/search material library. |
| `POST` | `/api/v1/materials` | `learning_material:modify` | Create material metadata under a syllabus topic. |
| `GET` | `/api/v1/materials/{id}` | `learning_material:view` | Get material detail. |
| `PUT` | `/api/v1/materials/{id}` | `learning_material:modify` | Update material metadata/link. |
| `DELETE` | `/api/v1/materials/{id}` | `learning_material:modify` | Delete material metadata. |
| `GET` | `/api/v1/me/materials` | `learning_material:view` | Current user's assigned materials. |
| `GET` | `/api/v1/syllabuses/{id}/topics/{topicId}/materials` | `syllabus:view` | List materials under a topic. |
| `POST` | `/api/v1/syllabuses/{id}/topics/{topicId}/materials` | `syllabus:modify` | Create material under a topic. |
| `DELETE` | `/api/v1/syllabuses/{id}/topics/{topicId}/materials/{materialId}` | `syllabus:modify` | Delete material under a topic. |

## Rules

- Material is stored as metadata/link in `material_files`; file upload/S3 is not implemented in this step.
- A material must belong to a syllabus topic.
- Active/Inactive syllabuses cannot have materials created, updated, or deleted.
- `/api/v1/me/materials` returns materials from syllabuses linked to training programs of classes/sessions where the current user has `Registered` or `Completed` registration.

## Create Material

```http
POST /api/v1/materials
Authorization: Bearer <accessToken>
Content-Type: application/json
Accept-Language: vi
```

```json
{
  "syllabusId": 1000,
  "topicId": 1000,
  "fileName": "Java OOP Slides",
  "fileUrl": "https://example.com/materials/java-oop-slides.pdf",
  "fileSize": 524288,
  "contentType": "application/pdf"
}
```

Success response:

```json
{
  "success": true,
  "data": {
    "id": 1000,
    "topicId": 1000,
    "fileName": "Java OOP Slides",
    "fileUrl": "https://example.com/materials/java-oop-slides.pdf",
    "fileSize": 524288,
    "contentType": "application/pdf",
    "uploadedBy": 1000,
    "uploadedAt": "2026-05-29T13:30:00"
  }
}
```

## List Material Library

```http
GET /api/v1/materials?syllabusId=1000&topicId=1000&keyword=java&page=1&limit=20
Authorization: Bearer <accessToken>
```

## Update Material

```http
PUT /api/v1/materials/1000
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "fileName": "Java OOP Slides v2",
  "fileUrl": "https://example.com/materials/java-oop-slides-v2.pdf",
  "fileSize": 600000,
  "contentType": "application/pdf"
}
```

## My Assigned Materials

```http
GET /api/v1/me/materials?keyword=java&page=1&limit=20
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "data": [
    {
      "id": 1000,
      "syllabusId": 1000,
      "syllabusName": "Java Foundation",
      "syllabusCode": "JAVA-FOUNDATION",
      "topicId": 1000,
      "topicName": "Object-Oriented Programming",
      "fileName": "Java OOP Slides",
      "fileUrl": "https://example.com/materials/java-oop-slides.pdf",
      "fileSize": 524288,
      "contentType": "application/pdf",
      "uploadedBy": 1000,
      "uploadedAt": "2026-05-29T13:30:00"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

Common errors:

| HTTP | Code | Meaning |
|---|---|---|
| `404` | `NOT_FOUND` | Syllabus, topic, or material was not found. |
| `409` | `SYLLABUS_NOT_EDITABLE` | Active/Inactive syllabus material cannot be changed. |
