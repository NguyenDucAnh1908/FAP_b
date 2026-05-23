# Project Structure

## Root Layout

```text
project-root/
  .claude/
  docs/
  src/
    main/
      java/com/fap/
      resources/
    test/
      java/com/fap/
  pom.xml
  mvnw
  mvnw.cmd
  README.md
```

## Java Package Layout

Use feature-first packages with shared infrastructure in `common`.

```text
com.fap/
  FapApplication.java
  common/
    api/
    config/
    exception/
    security/
    audit/
    util/
  auth/
  user/
  role/
  syllabus/
  program/
  clazz/
  quiz/
  calendar/
  notification/
  settings/
  storage/
```

Use `clazz` because `class` is a Java keyword.

## Per-Feature Layout

For implemented modules, use:

```text
feature/
  controller/
  dto/
  entity/
  mapper/
  repository/
  service/
  enums/
```

## Layer Rules

Allowed direction:

```text
controller -> service -> repository -> database
             service -> mapper
all layers -> common
```

Forbidden:

- Controller calling repository directly.
- Controller containing business rules.
- Repository containing status transition rules.
- Entity exposed directly as API response.
- Cross-feature writes that bypass the owning service.
- Raw SQL in service classes.

Raw SQL is allowed only in Flyway migrations or repository-level custom queries after review.

## Resource Layout

```text
src/main/resources/
  application.yaml
  application-local.yaml
  application-test.yaml
  db/migration/V1__create_fap_schema.sql
```
