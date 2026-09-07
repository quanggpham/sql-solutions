# AGENTS.md

SQL answers for a Vietnamese university database course assignment. Everything runs against the standard MySQL **`employees`** sample database (datacharmer/test_db: `employees`, `salaries`, `titles`, `dept_emp`, `dept_manager`, `departments`). That DB is **not** in this repo; load it before running anything. Schema diagram: `employees-schema.png`.

## Layout / mapping

- `bai_tap_database.md` — authoritative problem statement (Vietnamese; same content as `bai tap.docx`).
- `1/1.1.sql` … `1/1.13.sql` — Câu 1: read-only `SELECT` queries.
- `2/2.1.sql` … `2/2.3.sql` — Câu 2: `transaction` scripts that mutate data.
- `3/3.1.sql` — Câu 3: one stored procedure (two result sets).
- `4/4.1.sql` — Câu 4: stored procedure `proc_2` that transfers an employee to a new dept + title (mutates data; rejects `new_title = 'Manager'` and same-dept transfers via `SIGNAL` + `rollback`).
- `5/` — Câu 5: Maven project (`pom.xml`) — Java + HikariCP calling `proc_2` via `CallableStatement`; run `mvn -f 5/pom.xml exec:java -Ddb.password=...`.
- `6/` — Câu 6: Maven project — Java transaction (auto-commit off, rollback on error, commit on success) executing Câu 2.1's Staff→Senior Staff promotion; run `mvn -f 6/pom.xml exec:java -Ddb.password=...`.
- `7/` — Câu 7: Spring Boot REST service (`GET /employees`) with optional filters `hire_date_from`/`salary`/`dept_no`/`title` via JdbcTemplate + HikariCP; run `mvn -f 7/pom.xml spring-boot:run` (DB creds via `DB_URL`/`DB_USER`/`DB_PASSWORD` env vars).
- `8/` — Câu 8: Spring Boot HR & payroll system — `POST /employees` (splits full name), `PUT /employees/transfer`, `GET /payroll/summary?month=&year=`, scheduled tax job (23:59 on the 25th, cron `0 59 23 25 * ?`) computing TNCN per Câu 1.6 into an **auto-created** `income_tax(emp_no, tax, month, year)` with `FOR UPDATE` locking, `GET /analytics/unusual-promotions`, `GET /analytics/promotion-proposals` (25/30/20/25% weights, top 10); manually re-runnable via `POST /payroll/tax/run`. Pure helpers (`IncomeTaxJob.computeTax`, `AnalyticsService.combineScore`/`.analyze`, `EmployeeService.splitName`) are unit-tested (12 tests). Run `mvn -f 8/pom.xml package`; DB creds via `DB_URL`/`DB_USER`/`DB_PASSWORD` env vars.
- Root `1.11.sql` is a stray duplicate of `1/1.11.sql`; keep the `1/` copy. Câu 9 (grocery-store schema) exists only in the spec — no code here.

Every solution folder (`1/`–`8/`) has a `TEST.md`: end-to-end run/verify instructions for that bài (expected results, cross-check SQL, and mutation/rollback notes where relevant).

## Running

Single file, from repo root:

```
mysql -u root -p employees < 1/1.2.sql
```

- Dialect is **MySQL** (`LIMIT` not `TOP`, `curdate()`, `period_diff`, `timestampdiff`, `str_to_date`, `start transaction`, `delimiter //`). Do not "normalize" to standard SQL or another dialect.
- `3/3.1.sql` needs the `mysql` client (uses `delimiter //`).
- `2/` and `3/` scripts **mutate** the `employees` DB (UPDATE/DELETE/INSERT, temp tables, `SET SQL_SAFE_UPDATES`). Run them against a disposable copy — 1.x query answers assume pristine sample data.

## Domain conventions (easy to get wrong)

- Current/ongoing rows use sentinel `to_date = '9999-01-01'` in `salaries`, `titles`, `dept_emp`, `dept_manager`. Nearly every query must exclude history with this.
- Payday is **the 25th of each month**; the month's salary comes from the `salaries` row whose `[from_date, to_date]` interval *contains* the 25th. Month-to-month totals are computed via `period_diff` on year-month shifted by 25 days.

## Style

- Files are UTF-8 (no BOM), self-contained, executed standalone (no run order between them). Comments and problem restatements are in **Vietnamese** (`-- 1.3.  Lấy ra các nội dung...`) — keep new comments Vietnamese to match. Windows terminals render these as mojibake; the files are fine, don't "fix" them.
- Some files append an AI-generated alternative under an `-- AI` comment header (e.g. `1/1.2.sql`); treat that as an optional variant per file.