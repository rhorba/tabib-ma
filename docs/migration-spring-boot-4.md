# Spring Boot 3.5 → 4.1 Migration Plan — tabib-ma backend

**Status**: Approved 2026-11-18 (plan + option A); ADR-5 finalised by Story 11.1 spike · **Author**: Tech Lead · **Date**: 2026-11-18
**Hard deadline**: 2027-01-17 — the `.trivyignore` entries for spring-webmvc CVE-2026-47884 / CVE-2026-47890 expire then and CI turns red.

## 1. Why
Spring Boot 3.5.16 is the last OSS release of the 3.5 line (Spring Framework 6.2). Two CRITICAL spring-webmvc CVEs are fixed **only** in Spring Framework 7.0.9+. There is no patch path on 3.5 — the only real fix is Boot 4.

## 2. Target versions (checked against Maven Central, 2026-11-18)
| Component | Now | Target (Boot 4.1.2 BOM) |
|---|---|---|
| Spring Boot | 3.5.16 | **4.1.2** |
| Spring Framework | 6.2.19 | 7.0.10 (≥ 7.0.9 fixes both CVEs) |
| Spring Security | 6.5.x | 7.1.2 |
| Hibernate ORM | 6.6.x | 7.4.13.Final |
| Jackson | 2.21.7 (`com.fasterxml.jackson`) | 3.1.7 (`tools.jackson`) |
| Tomcat | 10.1.62 (pinned) | 11.0.27 (BOM default, no pin needed) |
| springdoc-openapi | 2.8.9 | 3.x (3.2.0 latest — confirm Boot 4.1 compatibility in spike) |
| Testcontainers | 1.20.1 | 2.0.x (artifact + package renames) |
| ArchUnit | 1.3.0 | 1.5.x (Java 21 + newer bytecode support) |
| Java / Gradle | 21 / 9.6.1 | unchanged — both supported |

Why 4.1.2 rather than 4.0.8: both include Spring 7.0.9+, but 4.1.x is the newer supported line, so it buys a longer OSS support window before the next forced upgrade.

## 3. Impact inventory (measured in this codebase)
| Area | Usage found | Expected change | Effort |
|---|---|---|---|
| Jackson 3 package rename | 2 main files (`JsonAccessDeniedHandler`, `JsonAuthenticationEntryPoint`), 21 test files — only `ObjectMapper` and `JsonNode` | `com.fasterxml.jackson.databind` → `tools.jackson.databind`; Jackson 3 exceptions are unchecked (remove `throws JsonProcessingException` where present) | S, mechanical |
| jjwt serializer | `jjwt-jackson` 0.12.6 is built on Jackson **2** | Decide in spike: keep (pulls Jackson 2 back onto the classpath, still CVE-scanned) or switch to `jjwt-gson` and drop Jackson 2 entirely | S |
| Modular starters | 8 starters in `build.gradle` | Boot 4 splits auto-config into modules. **Flyway** needs `spring-boot-starter-flyway`, not only `flyway-core`, or migrations silently stop running; MockMvc test auto-config moves to its own test module | S, but silent-failure risk |
| Spring Security 7 | 1 `SecurityFilterChain` config, JWT filter, 2 JSON error handlers | Deprecated DSL methods removed; check `authorizeHttpRequests` matchers and the custom JWT filter's ordering | S–M |
| Hibernate 7 | No `org.hibernate` imports; JPA only + 16 Flyway migrations, Postgres `EXCLUDE USING gist` constraints (ADR-4) | Stricter HQL/JPQL parsing and type mapping; the exclusion constraint lives in SQL, so it's unaffected, but the double-booking tests must stay green | M, mostly test-driven |
| Testcontainers 2 | 3 test files | `org.testcontainers:postgresql` → `testcontainers-postgresql`; `PostgreSQLContainer` package moves | S |
| Redis cache | 1 `@Cacheable` site (doctor search, with `maxFeeMad` key) | Cache serializer defaults may change with Jackson 3 → flush the dev cache after the upgrade; check search still caches | S |
| Events / scheduling | 3 `@TransactionalEventListener`, 2 `@Scheduled` | Not expected to change; covered by existing tests | — |
| Not used (no work) | `@MockBean`/`@SpyBean` (removed in Boot 4), `TestRestTemplate`, `RestTemplate`, `javax.*`, `@ConfigurationProperties` | — | — |
| Build hygiene | `dependencyManagement` netty pins, `ext['jackson-bom.version']`, `ext['tomcat.version']` | Delete all three — Boot 4.1.2's BOM is newer than every pin; regenerate `gradle.lockfile` | S |

**Overall**: a small surface. Most of the work is mechanical, plus a full regression run. Main risks: silent auto-config gaps (Flyway) and Hibernate 7 query strictness.

## 4. ADR-5: Spring Boot 4 migration approach
**Status**: Proposed — option A approved by user 2026-11-18; becomes Accepted after the Story 11.1 spike confirms it
**Context**: Two CRITICAL CVEs can only be fixed on Spring 7, and the risk acceptance expires 2027-01-17. The measured surface is small (above).
**Options**:
- 🟢 **A) Single migration branch, one PR.** Bump to 4.1.2, fix compile errors, do the Jackson 3 rename, and run the full regression in one pass. Pros: fastest (~1–1.5 days), one review, matches the small surface. Cons: if something breaks, it's harder to tell whether Boot, Hibernate or Jackson caused it.
- 🟡 **B) Two steps.** (1) Boot 4.1.2 with the Jackson 2 compatibility module (`spring-boot-jackson2`), keeping `com.fasterxml` imports; (2) a separate PR for the Jackson 3 rename. Pros: isolates Jackson from framework/Hibernate breakage. Cons: two regression cycles, and step 1 uses a module Boot has deprecated.
- 🔴 **C) Migration plus adopting new Boot 4 features** (API versioning, HTTP service clients, JSpecify null-safety). Rejected under YAGNI: none are needed to fix the CVEs.

**Recommendation**: **A**, with a 2-hour spike first (Story 11.1). If the spike shows Hibernate 7 or Security 7 breakage beyond ~10 failing tests, fall back to B.
**Consequences**: Removes all three dependency pins and both spring-webmvc `.trivyignore` entries. springdoc → 3.x requires regenerating the frontend `schema.d.ts` and checking the OpenAPI diff.
**Re-evaluate when**: Boot 4.2 / Spring 7.1 ships, or if the spike surfaces blockers.

## 5. Risks
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Flyway silently not running (starter split) | Medium | High (empty schema in a real env) | Explicit starter; integration tests assert tables exist; check the startup log for "Successfully applied" |
| Hibernate 7 rejects/changes an existing JPQL query | Medium | Medium | 339-test suite + e2e 19/19; fix query by query |
| OpenAPI contract drift from springdoc 3 | Low | Medium (frontend types) | Regenerate `schema.d.ts`, review the diff, frontend tests catch type breaks |
| Redis cache entries in the old (Jackson 2) format | Low | Low | `FLUSHDB` on dev; no production deploy exists yet |
| Slipping past 2027-01-17 | Low | Medium (CI red) | Start by mid-December; the expiry makes a slip visible, not silent |

## 6. Out of scope
Spring Boot 4 feature adoption (option C), Java version change, any refactoring not forced by the upgrade. The 9 date-dependent test helpers (`.logs/issues.md` 2026-11-18) are a separate small fix, but worth doing first so they can't add noise to the migration's regression runs.
