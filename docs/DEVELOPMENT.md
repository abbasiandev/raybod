# 🛠️ Development Guide

This guide provides coding standards and contribution guidelines for Hybrid Cloud Sentinel.

---

## Code Style

### Kotlin (Android)

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `ThreatRepository` |
| Functions | camelCase | `scanApplication()` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Variables | camelCase | `userPreferences` |

### Python (Backend)

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `ThreatAnalyzer` |
| Functions | snake_case | `analyze_permissions()` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_BATCH_SIZE` |

---

## Git Workflow

### Branch Naming

```
feature/  - New features
bugfix/   - Bug fixes
hotfix/   - Critical fixes
docs/     - Documentation
```

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(agent): add permission analyzer
fix(data): handle network timeout
docs(api): update endpoint reference
```

---

## Clean Architecture Rules

1. **Dependency Rule**: Dependencies point inward only
2. **Domain Independence**: No Android deps in `:domain`
3. **Repository Pattern**: Data access through interfaces

---

## Pull Request Checklist

- [ ] Tests pass (`./gradlew testDebugUnitTest`)
- [ ] Code follows style guide
- [ ] Documentation updated
- [ ] No breaking changes
