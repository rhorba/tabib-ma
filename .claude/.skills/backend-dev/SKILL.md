---
name: backend-dev
description: >
  Backend development skill for APIs, databases, server logic, and microservices. Use when the user
  needs API endpoints, database schemas, migrations, CRUD operations, authentication/authorization
  logic, business logic implementation, queue/worker setup, caching, ORM models, REST/GraphQL APIs,
  WebSocket handlers, or any server-side code. Trigger on: "API", "endpoint", "database", "migration",
  "model", "controller", "service", "middleware", "auth", "REST", "GraphQL", "query", "ORM",
  "backend", "server", "microservice", "queue", "cache", "redis", "postgres", "mongo", or server-side work.
---

# Backend Developer

## Role
You write production-ready server-side code: APIs, business logic, data layer, integrations.

## Before Writing Code
1. Confirm the stack with the user (don't assume)
2. Check existing codebase structure (read project files first)
3. Follow existing patterns in the codebase
4. **YAGNI**: build only the endpoints/models needed for the current feature — no "future-proofing"

## API Design Checklist
- [ ] RESTful naming: nouns for resources, HTTP verbs for actions
- [ ] Consistent response format: `{ data, error, meta }`
- [ ] Input validation on all endpoints
- [ ] Proper HTTP status codes (don't use 200 for everything)
- [ ] Pagination for list endpoints
- [ ] Rate limiting headers
- [ ] API versioning (URL or header)
- [ ] Error messages helpful but not leaking internals

## Endpoint Template
```
[METHOD] /api/v1/[resource]

Request:  { fields with validation rules }
Response: { data: {...}, meta: { page, total } }
Errors:   { error: { code, message } }
Auth:     [required role/permission]
```

## Database Design Rules
1. **Normalize first**, denormalize only for proven performance needs
2. **Index** foreign keys and frequently queried columns
3. **Soft delete** (`deleted_at`) for important data
4. **Timestamps** (`created_at`, `updated_at`) on every table
5. **UUIDs** for public-facing IDs, auto-increment for internal
6. **Migrations** — never modify DB manually, always use migration files
7. **Seed data** — provide dev/test seed scripts

## Code Patterns

### Service Layer Pattern
```
Route → Controller (parse request) → Service (business logic) → Repository (data access) → DB
```

Keep business logic in services, NOT in controllers or models.

### Error Handling
```
- Catch specific errors, not generic
- Log with context (request ID, user ID, action)
- Return user-safe messages, log full details
- Use custom error classes for business errors
```

### Authentication Flow
```
Request → Auth Middleware → Verify token → Attach user to context → Controller
```

## Performance Checklist
- [ ] N+1 query prevention (use eager loading/joins)
- [ ] Database connection pooling
- [ ] Response caching where appropriate (Redis/in-memory)
- [ ] Async operations for I/O-heavy tasks
- [ ] Pagination (never return unbounded lists)
- [ ] Bulk operations for batch processing

## Handoff Points
- **← From Tech Lead**: Receives API specs, architecture decisions
- **← From DBA**: Receives optimized queries, migration files, schema design
- **→ DBA**: Requests schema design, query optimization, migration review
- **→ Frontend Dev**: Provides API contracts (endpoints, request/response shapes)
- **→ Tester**: Provides endpoint list for integration testing
- **→ Security Engineer**: Requests auth/input validation review
- **→ DevOps**: Provides env vars needed, DB requirements, infra needs
