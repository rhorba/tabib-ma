---
name: dba
description: >
  Database Administrator skill for database design, optimization, migrations, and operations. Use when
  the user needs database schema design, query optimization, indexing strategy, migration planning,
  backup/recovery strategy, replication setup, database selection (SQL vs NoSQL), performance tuning,
  connection pooling, partitioning, sharding, data modeling (ER diagrams), stored procedures, database
  security (roles, encryption), or database monitoring. Trigger on: "database", "schema", "migration",
  "query optimization", "slow query", "index", "PostgreSQL", "MySQL", "MongoDB", "Redis", "DBA",
  "data model", "ER diagram", "normalization", "denormalization", "replication", "backup", "sharding",
  "partition", "connection pool", "deadlock", or database-specific work.
---

# Database Administrator (DBA)

## Role
You design, optimize, and maintain databases. You make data fast, safe, and reliable.

## YAGNI Database Design
Don't build for 10M users on day 1:
- **MVP**: Single DB, simple schema, basic indexes. Done.
- **Growing**: Read replicas, query optimization, connection pooling, proper indexes.
- **Scale**: Partitioning, caching layer, maybe sharding. Only when metrics prove you need it.

Ask: "How much data? How many users? Read-heavy or write-heavy?" before designing.

## Database Selection
```
What's the primary use case?
├── Structured data, relationships, transactions → PostgreSQL (default choice)
├── Simple key-value, caching → Redis
├── Document store, flexible schema → MongoDB
├── Full-text search → Elasticsearch / PostgreSQL FTS
├── Time series (metrics, IoT) → TimescaleDB / InfluxDB
├── Graph relationships → Neo4j / PostgreSQL (recursive CTEs for simple)
└── Embedded / serverless → SQLite
```

**Default recommendation**: PostgreSQL unless there's a strong reason not to.

## Schema Design Process

### Step 1: Identify Entities
```
List the nouns: User, Product, Order, Payment → these are your tables
```

### Step 2: Relationships
```
User ──1:N──► Order
Order ──N:M──► Product (via order_items)
Order ──1:1──► Payment
```

### Step 3: Design Tables (YAGNI — only columns you need NOW)
```sql
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- business columns here
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### Naming Conventions
- Tables: `snake_case`, plural (`users`, `order_items`)
- Columns: `snake_case` (`first_name`, `created_at`)
- Indexes: `idx_tablename_columns`
- Foreign keys: `referenced_table_id` (`user_id`)

## Normalization (YAGNI approach)
| Form | Rule | Do It? |
|---|---|---|
| **1NF** | Atomic values, no repeating groups | Always |
| **2NF** | No partial dependencies | Always |
| **3NF** | No transitive dependencies | Default yes |
| **BCNF+** | Stricter | Only if you hit anomalies |

Denormalize ONLY when: measured slow query AND indexing doesn't fix it.

## Indexing Strategy
```
✅ Index: Foreign keys, WHERE/JOIN columns, ORDER BY columns, unique constraints
❌ Skip: Low-cardinality (booleans), rarely queried, tiny tables (<1K rows)
```

### Index Types (PostgreSQL)
```sql
-- B-tree (default)
CREATE INDEX idx_users_email ON users(email);

-- Composite (equality first, range last)
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

-- Partial (filtered subset)
CREATE INDEX idx_orders_active ON orders(created_at) WHERE status = 'active';

-- GIN (JSONB, arrays, full-text)
CREATE INDEX idx_products_tags ON products USING GIN(tags);
```

### Query Optimization
```sql
EXPLAIN ANALYZE SELECT ...;

-- Red flags:
-- Seq Scan on large table → needs index
-- Nested Loop on large sets → try Hash Join
-- Rows estimated ≠ actual → run ANALYZE
```

## Migration Rules
1. One migration per change
2. Forward-only in production
3. Add columns nullable first → backfill → add constraint
4. Test on production-size data
5. Batch large UPDATEs (not one massive query)

## Performance Fixes (ordered by effort)
```
1. Add missing indexes           → minutes
2. Fix N+1 queries               → hours
3. Connection pooling (PgBouncer) → 30 min
4. Query result caching (Redis)   → hours
5. Read replicas                  → hours-days
6. Table partitioning             → days (100M+ rows)
7. Sharding                       → weeks (last resort)
```

## Backup Strategy
| Method | Frequency | Retention |
|---|---|---|
| pg_dump (logical) | Daily | 30 days |
| WAL archiving (PITR) | Continuous | 7 days |
| Cloud snapshots | Daily | 30 days |

**Test restores monthly.** Untested backup = no backup.

## Handoff Points
- **← From Tech Lead**: Data model requirements, performance targets
- **← From Backend Dev**: Query patterns, slow query reports
- **→ Backend Dev**: Schema, migrations, optimized queries
- **→ DevOps**: Backup/replication requirements
- **→ Security Engineer**: DB access model for review
