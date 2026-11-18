# CORRECTIONS — Tabib.ma



### [2026-11-18] [CORRECTION] — Batch 1 scope grew: newer Trivy DB found 3 more items
- Local Trivy (fresh DB) found items not in CI's 2026-11-08 run: tomcat CVE-2026-65182/65637/65905/68525 (fix 10.1.58/59) → bumped tomcat to 10.1.62 instead of 10.1.56; spring-webmvc CVE-2026-47890 (fix only 7.0.9, SSE — no SSE in backend) → time-boxed ignore; picomatch CVE-2026-112366 (no fix exists, dev/build-only) → time-boxed ignore. All ignores expire 2027-01-17.
