# Tester — Test Pattern Library & Coverage Reference

Load when writing complex tests or debugging flaky / incomplete test suites.

---

## Core Test Patterns

### Arrange-Act-Assert (AAA)
```python
def test_order_total_with_discount():
    # Arrange
    order = Order()
    order.add_item(Product(price=100), quantity=2)
    discount = PercentageDiscount(rate=0.10)

    # Act
    total = order.calculate_total(discount)

    # Assert
    assert total == Money(180, "USD")
```

### Object Mother — centralizes test data creation
```python
class OrderMother:
    @staticmethod
    def a_new_order() -> Order:
        return Order(customer_id=CustomerId("cust-1"))

    @staticmethod
    def an_order_with_items(count: int = 2) -> Order:
        order = OrderMother.a_new_order()
        for _ in range(count):
            order.add_item(ProductMother.a_standard_product(), quantity=1)
        return order
```

### Builder — fluent, readable test setup
```python
class OrderBuilder:
    def __init__(self):
        self._customer = "default"
        self._items = []

    def with_customer(self, cid: str) -> "OrderBuilder":
        self._customer = cid
        return self

    def with_item(self, product, qty: int) -> "OrderBuilder":
        self._items.append((product, qty))
        return self

    def build(self) -> Order:
        o = Order(self._customer)
        for product, qty in self._items:
            o.add_item(product, qty)
        return o
```

---

## Test Double Cheat Sheet

| Double | Definition | When |
|---|---|---|
| Stub | Returns hardcoded value | Control a specific return path |
| Spy | Records calls + real impl | Verify interactions happened |
| Mock | Pre-programmed expectations | Strict interaction testing |
| Fake | Working impl, shortcuts | In-memory DB, local email |
| Dummy | Placeholder, never called | Fill required parameters |

### Fake (In-Memory Repository)
```python
class InMemoryOrderRepository:
    def __init__(self): self._store = {}
    def save(self, order): self._store[str(order.id)] = order
    def find_by_id(self, id): return self._store.get(str(id))
```

---

## Coverage Strategy

**MUST cover (every line):**
- Business logic: calculations, validation, state transitions
- Error handling: what happens when dependencies fail
- Auth / authorization checks

**CAN skip (justified):**
- Trivial getters/setters with zero logic
- Framework boilerplate (ORM models, route registration)
- Generated code

**Anti-pattern — testing the framework, not your code:**
```python
# Bad: tests Django ORM, not your logic
def test_save(): user.save(); assert User.objects.count() == 1

# Good: tests your validation rule
def test_user_requires_email():
    with pytest.raises(ValidationError): User.create(name="Alice", email=None)
```

---

## Parametrized Tests (boundary conditions)

```python
@pytest.mark.parametrize("amount,currency,expected", [
    (100, "USD", "USD 100.00"),
    (0,   "EUR", "EUR 0.00"),
    (-1,  "USD", ValueError),
])
def test_money_formatting(amount, currency, expected):
    if expected is ValueError:
        with pytest.raises(ValueError): Money(amount, currency)
    else:
        assert str(Money(amount, currency)) == expected
```

---

## Integration Test Checklist (per endpoint)

- [ ] Happy path: valid input → expected response + DB state
- [ ] Missing required field → 422 with field name
- [ ] Invalid field type → 422
- [ ] Unauthorized → 401
- [ ] Wrong user (forbidden) → 403
- [ ] Not found → 404
- [ ] Duplicate creation → 409

---

## Flaky Test Diagnosis

| Symptom | Likely Cause | Fix |
|---|---|---|
| Fails only in CI | Timing dependency | Remove `sleep()`, use polling |
| Fails only with others | Shared state | Reset DB / mocks in setUp |
| Fails ~10% of the time | Race condition | Explicit synchronization |
| Fails on different dates | Hardcoded date | Use test clock |

**Rule**: A flaky test is worse than no test. Quarantine max 1 sprint, then fix or delete.

---

## Coverage Commands by Stack

```bash
# Node / TypeScript
npm run test -- --coverage

# Python
pytest --cov=src --cov-report=term-missing

# Go
go test ./... -coverprofile=coverage.out && go tool cover -func=coverage.out

# Java
mvn test jacoco:report
```
