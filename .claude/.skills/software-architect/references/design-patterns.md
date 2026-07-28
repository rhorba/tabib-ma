# Software Architect — Design Patterns & Architecture Reference

Load when designing module boundaries, selecting patterns, or reviewing architecture fitness.

---

## Architecture Styles — YAGNI-Ordered

| Style | When to Use |
|---|---|
| Monolith (modular) | < 10 engineers, single domain |
| Modular monolith | Growing team, clear domain boundaries |
| Strangler Fig | Migrating legacy incrementally |
| Microservices | Independent scaling proven, ≥ 3 teams |
| Event-driven | Async workflows, audit trails, fan-out |
| CQRS | Read/write models diverge significantly |
| Event Sourcing | Full audit trail required, temporal queries |

---

## Clean Architecture (Dependency Rule)

```
[ Frameworks & Drivers ]  ← React, Express, Postgres driver
       ↓  ↑
[ Interface Adapters ]    ← Controllers, Presenters, Gateways
       ↓  ↑
[ Application Layer ]     ← Use Cases
       ↓  ↑
[ Domain Layer ]          ← Entities, Value Objects (zero framework imports)
```

Inner layers never import outer layers.

---

## DDD Building Blocks

### Entity — identity persists over time
```python
class Order:
    def __init__(self, id: OrderId, customer: CustomerId):
        self.id = id
        self.items: list[OrderItem] = []
```

### Value Object — immutable, identity = attributes
```python
@dataclass(frozen=True)
class Money:
    amount: Decimal
    currency: str

    def add(self, other: "Money") -> "Money":
        assert self.currency == other.currency
        return Money(self.amount + other.amount, self.currency)
```

### Repository — abstracts data access from domain
```python
class OrderRepository(Protocol):
    def find_by_id(self, id: OrderId) -> Order | None: ...
    def save(self, order: Order) -> None: ...
```

### Aggregate Root — owns invariants, single transaction boundary
- External code only holds a reference to the root
- All state changes go through root methods

---

## Hexagonal Architecture (Ports & Adapters)

```
[ HTTP Controller ]  [ CLI ]
         |
[ Application Service ]
    /              \
[Port: OrderRepo]  [Port: EmailSender]
    |                    |
[Postgres]           [Sendgrid]
```

Port = interface defined by the application.
Adapter = infrastructure implementation of the port.
Swap adapters without touching business logic.

---

## GoF Quick Reference

| Pattern | Category | Problem | Solution |
|---|---|---|---|
| Factory Method | Creational | Create objects without knowing type | Subclass decides |
| Builder | Creational | Complex object step-by-step | Separate construction |
| Adapter | Structural | Incompatible interfaces | Wrapper translates calls |
| Decorator | Structural | Add behavior without subclassing | Wrap + extend |
| Facade | Structural | Simplify complex subsystem | Single unified interface |
| Strategy | Behavioral | Interchangeable algorithms | Inject algorithm |
| Observer | Behavioral | Notify dependents | Subscribe/publish |
| Command | Behavioral | Encapsulate operations | Enable undo/queue |
| Chain of Responsibility | Behavioral | Pass request along handlers | Middleware pipelines |

---

## SOLID Quick Reference

| Principle | Violation Sign | Fix |
|---|---|---|
| Single Responsibility | "God class" 500+ lines | Extract focused classes |
| Open/Closed | `if type == X` chains | Strategy or polymorphism |
| Liskov Substitution | Subclass throws unexpectedly | Fix the inheritance |
| Interface Segregation | Clients implement unused methods | Split interfaces |
| Dependency Inversion | Business logic `new`s infrastructure | Inject dependencies |

---

## Module Boundary Rules

1. High cohesion within a module — related concepts together
2. Low coupling between modules — explicit interfaces only
3. Stable dependencies — unstable → stable, never reverse
4. No circular imports between modules
5. Package by feature (`orders/`), not by layer (`controllers/`)

---

## Architectural Fitness Functions

Automated rules enforced in CI to prevent architectural drift:

```python
# import-linter (Python) — domain must not import infrastructure
[forbidden]
source_modules = myapp.domain.*
forbidden_modules = myapp.infrastructure.*
```

Run fitness functions in every CI build.
