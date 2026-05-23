# Simplify Command

## Purpose

Reduce unnecessary complexity without changing behavior.

## Rules

- Keep public API behavior unchanged.
- Keep database schema unchanged unless explicitly requested.
- Preserve response envelopes and status codes.
- Run tests before and after when possible.

## Commands

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```
