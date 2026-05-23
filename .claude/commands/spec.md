---
name: spec
description: Create a backend feature specification before implementation
---

# /spec

## Purpose

Define backend requirements before code changes.

## Discovery Questions

- Which frozen module owns this behavior?
- Which endpoint or workflow is affected?
- Which roles may perform the action?
- What ownership checks apply?
- What status transitions are allowed?
- Is a database migration required?
- What tests prove the behavior?

## Output Template

```markdown
# Feature: [Name]

## Objective

## Scope Reference
- `docs/07_scope_freeze.md`
- `docs/06_business_logic_review.md`

## API Contract

## Business Rules

## Security

## Database Impact

## Testing Strategy

## Out Of Scope
```
