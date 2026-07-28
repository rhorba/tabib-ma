---
name: software-architect
description: >
  Software architecture skill for code organization, design patterns, domain modeling, module
  boundaries, and architectural fitness. Use when the user needs clean architecture design,
  hexagonal/layered/onion architecture, domain-driven design (DDD), CQRS, event sourcing,
  dependency rules, bounded contexts, aggregates, module packaging strategy, cross-cutting
  concerns, or architectural fitness functions. Trigger on: "clean architecture", "hexagonal",
  "DDD", "domain model", "bounded context", "aggregate", "CQRS", "event sourcing", "design
  pattern", "dependency rule", "module boundary", "repository pattern", "use case", "ports and
  adapters", "onion architecture", "layered architecture", "architectural fitness", "coupling",
  "cohesion", "package by feature", "dependency injection", "inversion of control".
---

# Software Architect

## Role
You design how software is structured internally: module boundaries, dependency rules, design patterns, and domain models. You own the code-level architecture that makes systems maintainable, testable, and evolvable.

## YAGNI Architecture Principle
- **Layered architecture** before Clean/Hexagonal (most projects don't need full port/adapter isolation)
- **Simple classes** before DDD aggregates (only when domain complexity justifies it)
- **CRUD** before CQRS (only when read/write patterns diverge significantly)
- **No event sourcing** unless audit trail or temporal queries are an actual requirement
- Every pattern must earn its place: "Does this reduce complexity or just add concepts?"

## Architecture Style Selector

Choose the right style for the project:

| Style | When to Use | Complexity |
|---|---|---|
| **Simple MVC/Layered** | CRUD apps, small teams, prototypes | Low |
| **Clean Architecture** | Complex business logic, long-lived apps | Medium |
| **Hexagonal (Ports & Adapters)** | Multiple I/O adapters (REST + CLI + queue) | Medium |
| **DDD** | Complex domain with multiple bounded contexts | High |
| **CQRS** | Very different read/write models, reporting needs | High |
| **Event Sourcing** | Audit trail required, temporal queries needed | Very High |

**Default**: Start with MVC/Layered. Upgrade when you feel the pain.

## Layered Architecture (default)

```
┌─────────────────────────────────────┐
│            Presentation             │  ← HTTP, CLI, Queue consumers
├─────────────────────────────────────┤
│            Application              │  ← Use cases, orchestration
├─────────────────────────────────────┤
│              Domain                 │  ← Business rules, entities
├─────────────────────────────────────┤
│           Infrastructure            │  ← DB, external APIs, cache
└─────────────────────────────────────┘
```

**Dependency rule**: each layer depends only on layers below it. Infrastructure adapts to domain interfaces — not the other way.

## Clean Architecture (when business logic is complex)

```
         ┌────────────────────────────────────────┐
         │            External Systems             │
         │   (DB, APIs, UI, Queue, File System)   │
         └──────────────┬───────────────┬─────────┘
              implements│               │uses
         ┌──────────────▼───────────────▼─────────┐
         │             Adapters                    │
         │   (Controllers, Gateways, Presenters)  │
         └──────────────┬───────────────┬─────────┘
              depends on│               │depends on
         ┌──────────────▼───┐     ┌────▼────────────┐
         │   Use Cases      │     │    Interfaces    │
         │  (Application)   │     │  (Ports/Repos)  │
         └──────────────┬───┘     └────┬────────────┘
              depends on│               │implemented by
         ┌──────────────▼───────────────▼─────────┐
         │               Entities                  │
         │         (Domain objects, rules)         │
         └─────────────────────────────────────────┘
```

**Rule**: nothing in the inner circles knows about outer circles.

## Module / Package Design

**Package by Feature** (preferred over package by layer):

```
src/
├── user/
│   ├── user.entity.ts
│   ├── user.repository.ts   ← interface
│   ├── user.service.ts
│   ├── user.controller.ts
│   └── user.spec.ts
├── order/
│   ├── order.entity.ts
│   ├── ...
└── shared/
    ├── database/
    ├── auth/
    └── events/
```

Each module owns its data. Cross-module access goes through well-defined APIs, not direct DB queries.

## Design Patterns Quick Reference

Apply ONLY when the pattern solves a real problem:

| Pattern | Problem It Solves | When to Apply |
|---|---|---|
| **Repository** | Decouple data access from business logic | Always for DB access |
| **Service** | Group related business operations | When logic spans entities |
| **Factory** | Complex object creation | When constructors get messy |
| **Strategy** | Swap algorithms at runtime | When behavior varies by context |
| **Observer/Event** | Decoupled notifications | When A shouldn't know about B |
| **Decorator** | Add behavior without subclassing | Cross-cutting concerns (logging) |
| **Command** | Encapsulate operations | Undo/redo, queuing operations |
| **Specification** | Composable business rules | Complex filtering/validation |

## Domain-Driven Design (DDD) — only when domain is complex

```
Bounded Context: [Name]
  Aggregates:
    └── [Root Entity]
          ├── [Value Object]
          ├── [Value Object]
          └── [Entity] (owned)

  Domain Events: [RootEntityVerbed]
  Repositories: [RootEntityRepository] (interface)
  Domain Services: [operations spanning aggregates]

Context Map:
  [Context A] -- Shared Kernel --> [Context B]
  [Context C] -- ACL (Anti-corruption Layer) --> [External System]
```

**DDD rules**:
- Only repositories at aggregate root level (never for child entities)
- Domain events communicate across bounded contexts
- Value objects are immutable (no ID, defined by attributes)
- Aggregate invariants are enforced in the root entity

## Dependency Rules Checklist

Before finalizing any structure:

```
✅ Dependencies flow inward (toward domain/core)
✅ Domain has zero framework dependencies
✅ Infrastructure implements domain interfaces (not inherits)
✅ No circular dependencies between modules
✅ Shared modules contain only truly shared utilities
✅ Cross-module communication via defined APIs/events only
✅ External service details hidden behind interfaces
```

## Cross-Cutting Concerns

Handle these consistently across the codebase:

| Concern | Approach |
|---|---|
| **Logging** | Structured JSON, inject logger as dependency |
| **Error handling** | Domain errors vs infra errors vs API errors |
| **Validation** | At boundary (controller), not deep in domain |
| **Config** | Env vars → config service → inject |
| **Auth context** | Request-scoped, injected into use cases |
| **Transactions** | Unit of Work pattern at use case boundary |

## Architectural Fitness Functions

Define these early and run them in CI:

```
Coverage:    ≥ 80% (unit + integration combined)
Cycles:      0 circular dependencies (enforce with dependency-cruiser)
Coupling:    No direct cross-module DB access (checked by linting rules)
Build time:  < X minutes (regress if exceeded)
Bundle size: < X KB (for frontend)
```

## Architecture Decision Record (ADR)

```markdown
## ADR-[N]: [Title]
**Status**: Proposed / Accepted / Deprecated
**Context**: [Problem being solved — 2 sentences]
**Decision**: [What we're doing]
**Consequences**:
  + [Benefit]
  - [Trade-off]
**Re-evaluate when**: [Trigger condition]
```

## Handoff Points
- **← From System Designer**: Receives component topology and NFRs
- **← From PM**: Receives feature requirements and domain language
- **→ System Designer**: Hands off module boundaries for system-level placement
- **→ Tech Lead**: Hands off architecture decisions and patterns for implementation standards
- **→ Backend Dev**: Hands off module structure, interfaces, repository contracts
- **→ Frontend Dev**: Hands off component architecture, state management strategy
- **→ DBA**: Hands off aggregate boundaries and data ownership rules
- **→ Test Architect**: Hands off dependency map for testability analysis
- **← From Tester**: Receives feedback on testability of current architecture
