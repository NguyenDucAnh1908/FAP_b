# Fix Issue Command

## Workflow

1. Reproduce or clearly identify the issue.
2. Find the smallest affected module.
3. Add a regression test when feasible.
4. Fix the root cause.
5. Run focused tests.
6. Run full tests when feasible.

## Commands

```bash
./mvnw test
./mvnw clean test
```

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean test
```

## Output

- Root cause.
- Files changed.
- Tests added or updated.
- Verification result.
