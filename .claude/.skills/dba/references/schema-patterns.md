# DBA — Schema Design Patterns & Database Reference

Load when designing a schema, optimizing queries, or planning zero-downtime migrations.

---

## Schema Design Principles

1. Start normalized — denormalize only after proving a performance problem
2. Every table has a primary key — prefer UUIDs over natural keys
3. Enforce constraints at the DB layer — don't rely only on app validation
4. Name consistently: `snake_case`, plural table names, `_id` suffix for FKs
5. Add `created_at`, `updated_at` to every table — costs nothing, invaluable later
6. Version every migration — never edit a migration applied to production

---

## Common Schema Patterns

### Soft Deletes
```sql
ALTER TABLE orders ADD COLUMN deleted_at TIMESTAMPTZ;

-- Partial index — only indexes non-deleted rows
CREATE INDEX idx_orders_active ON orders (customer_id)
  WHERE deleted_at IS NULL;
-- Application always adds WHERE deleted_at IS NULL
```

### Audit Trail
```sql
CREATE TABLE audit_log (
  id          BIGSERIAL PRIMARY KEY,
  table_name  TEXT NOT NULL,
  record_id   UUID NOT NULL,
  action      TEXT NOT NULL CHECK (action IN ('INSERT', 'UPDATE', 'DELETE')),
  changed_by  UUID REFERENCES users(id),
  changed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  old_values  JSONB,
  new_values  JSONB
);
```

### Multi-Tenancy
```sql
-- Shared schema with RLS (simplest)
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON orders
  USING (tenant_id = current_setting('app.tenant_id')::UUID);
```

---

## Indexing Strategy

### Add an index when:
- Column appears in `WHERE`, `JOIN ON`, or `ORDER BY` in frequent queries
- Column is a FK queried against

### Skip the index when:
- Table < 10,000 rows (sequential scan is faster)
- Column has very low cardinality (boolean, 2-3 status values)
- Write-heavy table where maintenance cost exceeds read benefit

### PostgreSQL Index Types

| Type | Use Case |
|---|---|
| B-Tree (default) | Equality, range, sorting |
| GIN | JSONB, arrays, full-text search |
| BRIN | Time-series, naturally ordered large tables |
| Partial | Subset of rows (`WHERE active = true`) |

```sql
-- Composite: most selective column FIRST
CREATE INDEX idx_orders_status_created ON orders (status, created_at);

-- Concurrent — no table lock
CREATE INDEX CONCURRENTLY idx_orders_customer ON orders (customer_id);
```

---

## Zero-Downtime Migration Checklist

```
Adding a column:
  [ ] Make nullable or add DEFAULT — adding NOT NULL without default locks table
  [ ] Backfill in batches if needed
  [ ] Add NOT NULL constraint separately after backfill

Renaming a column:
  [ ] Add new column, backfill, switch app, drop old — never rename directly

Removing a column:
  [ ] Remove from application first, deploy
  [ ] Wait one full deploy cycle
  [ ] Then DROP COLUMN

Adding an index:
  [ ] Always CREATE INDEX CONCURRENTLY
```

### Migration Template
```sql
-- Migration: 0042_add_user_verified_at.sql
BEGIN;

ALTER TABLE users ADD COLUMN verified_at TIMESTAMPTZ;

UPDATE users SET verified_at = created_at WHERE verified_at IS NULL;

CREATE INDEX CONCURRENTLY idx_users_unverified ON users (created_at)
  WHERE verified_at IS NULL;

COMMIT;

-- Rollback:
-- DROP INDEX CONCURRENTLY IF EXISTS idx_users_unverified;
-- ALTER TABLE users DROP COLUMN IF EXISTS verified_at;
```

---

## Query Optimization

### Finding missing indexes
```sql
SELECT schemaname, tablename, seq_scan, seq_tup_read
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_tup_read DESC LIMIT 20;
```

### Fixing N+1 queries (most common ORM bug)
```python
# Bad — N+1
for order in Order.objects.filter(status="pending"):
    print(order.customer.name)  # 1 query per order

# Good — 2 queries total
Order.objects.filter(status="pending").select_related("customer")
```

### Cursor Pagination (use over OFFSET at scale)
```sql
SELECT * FROM orders
WHERE created_at < :last_cursor
ORDER BY created_at DESC LIMIT 20;
-- Return last row's created_at as next_cursor
```

---

## Connection Pool Guidelines

| Metric | Rule |
|---|---|
| Max DB connections | CPU cores × 2 + effective_io_concurrency |
| Pool size per app instance | max_connections / app_instances × 0.8 |
| Statement timeout | 30 s (prevent runaway queries) |
| Lock timeout | 5 s (prevent deadlock pile-up) |

Add PgBouncer when total app connections > 200.
