# Backend Dev — API Design Patterns & Best Practices

Load when designing or reviewing REST APIs, error handling, or server-side patterns.

---

## REST API Conventions

### URL Structure
```
GET    /users            → list users
POST   /users            → create user
GET    /users/:id        → get one user
PUT    /users/:id        → replace user
PATCH  /users/:id        → partial update
DELETE /users/:id        → delete user

GET    /users/:id/orders → nested resource (orders belonging to a user)
```

**Rules:**
- Nouns, not verbs: `/orders`, not `/getOrders`
- Plural resource names: `/users`, not `/user`
- Lowercase, hyphen-separated: `/order-items`, not `/orderItems`
- Never expose internal IDs when UUIDs can be used

---

## HTTP Status Codes

| Code | Meaning | When |
|---|---|---|
| 200 | OK | Successful GET / PUT / PATCH |
| 201 | Created | Successful POST that created a resource |
| 204 | No Content | Successful DELETE or action with no body |
| 400 | Bad Request | Validation failure (client error) |
| 401 | Unauthorized | Missing or invalid auth token |
| 403 | Forbidden | Valid token but no permission |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate creation, optimistic lock conflict |
| 422 | Unprocessable Entity | Business rule violation |
| 429 | Too Many Requests | Rate limit hit |
| 500 | Internal Server Error | Unexpected server failure |
| 503 | Service Unavailable | Downstream dependency down |

---

## Error Response Format (consistent across all endpoints)

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "The request contains invalid fields.",
    "details": [
      { "field": "email", "message": "Must be a valid email address" },
      { "field": "age",   "message": "Must be a positive integer" }
    ],
    "request_id": "req_01HXZ..."
  }
}
```

**Rules:**
- Always return JSON — never plain text error strings
- Always include a `request_id` for tracing
- Never expose stack traces or internal paths in production
- Error `code` is a machine-readable string (not an HTTP status code)

---

## Pagination

### Cursor-based (preferred for large datasets)
```json
GET /orders?limit=20&cursor=eyJpZCI6MTAwfQ==

{
  "data": [...],
  "pagination": {
    "next_cursor": "eyJpZCI6MTIwfQ==",
    "has_more": true
  }
}
```

### Offset-based (simpler, degrades at large offsets)
```json
GET /orders?page=3&per_page=20

{
  "data": [...],
  "pagination": {
    "page": 3,
    "per_page": 20,
    "total": 847,
    "total_pages": 43
  }
}
```

Use cursor for > 10K records. Use offset only for small, admin-facing lists.

---

## API Versioning

```
URL versioning (simplest):  /v1/users  /v2/users
Header versioning:          Accept: application/vnd.api+json;version=2
```

**Rules:**
- Never break a published API without bumping the version
- Deprecation: set `Sunset` header, give 6 months notice minimum
- v1 and v2 can coexist — sunset v1 only after migration confirmed

---

## Rate Limiting

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1719878400
Retry-After: 60  (on 429 response)
```

**Strategy:** Token bucket per API key. Unauthenticated endpoints use IP-based limiting.

---

## Idempotency (for mutations)

```
POST /payments
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000

Server stores key → result mapping for 24 hours.
Duplicate request with same key → returns original result, no side effects.
```

Required for: payments, order creation, email sends, any non-idempotent POST.

---

## Input Validation Checklist

```
[ ] Required fields present
[ ] Types match schema (string, integer, UUID, date)
[ ] String lengths within bounds
[ ] Numeric values within allowed range
[ ] Enum values in allowed set
[ ] Email / URL / phone validated with regex
[ ] File uploads: type check (not just extension), size limit
[ ] No HTML / script injection possible in text fields
```

Always validate at the API boundary — never trust client data.

---

## Auth Patterns Quick Reference

| Pattern | Token Location | Pros | Cons |
|---|---|---|---|
| JWT (short-lived) | Authorization: Bearer | Stateless | Cannot revoke before expiry |
| JWT + refresh token | Refresh in HttpOnly cookie | Balance of stateless + revocable | More complex |
| Opaque session token | HttpOnly cookie | Instantly revocable | Requires shared session store |
| API key | X-API-Key header | Simple for machine-to-machine | No expiry by default |
