# Retail System Database Design Notes

## Overview
This schema implements a production-grade retail system covering:
- Employee management & authentication/authorization (RBAC)
- Attendance tracking
- Product & customer master data
- Sales invoicing with inventory deduction
- Historical stock ledger
- Loyalty program with yearly reset

**Target**: PostgreSQL 14+ (TIMESTAMPTZ, GENERATED columns, MATERIALIZED VIEWs)
**Timezone**: Asia/Ho_Chi_Minh (UTC+7)

---

## Key Design Decisions

### 1. Separation of Auth from Business Data (Scenarios A, L)
- `EMPLOYEE` holds business data; `USER_ACCOUNT` holds credentials
- `ON DELETE RESTRICT` on `employee_id` FK prevents deleting employee with user account
- Employee termination = status change + `terminated_at`, not physical delete
- Historical invoices/attendance/stock movements retain FK to employee

### 2. RBAC with Proper N:M Tables (Scenario A)
- `ROLE` + `PERMISSION` are separate entities
- `USER_ROLE` and `ROLE_PERMISSION` use composite PKs (no surrogate ID)
- Supports: cashier, supervisor, manager, inventory_clerk, admin roles

### 3. Attendance Flexibility (Scenario B)
- `check_out_at` nullable = employee currently working
- `CHECK (check_out_at >= check_in_at)` enforces logical order
- No unique constraint on `(employee_id, check_in_at::date)` → multiple shifts/day supported
- Index on `(employee_id, check_in_at)` for shift queries

### 4. Product Snapshots for Historical Accuracy (Scenario K)
- `INVOICE_DETAIL` stores `product_code_snapshot`, `product_name_snapshot`
- `unit_price` at time of sale (not current `PRODUCT.selling_price`)
- `line_total` = `(quantity * unit_price) - discount_amount` as GENERATED STORED
- Product status `DISCONTINUED`/`ARCHIVED` prevents new sales but preserves history
- `ON DELETE RESTRICT` on `product_id` FK prevents deleting used products

### 5. Customer Walk-in Support (Scenarios F, G)
- `customer_id` nullable on `INVOICE`
- `LOYALTY_ACCOUNT` is 1:1 with `CUSTOMER` but optional
- Walk-in = invoice with NULL customer_id, no loyalty points
- Registered customer = customer_id set, loyalty earned if account exists

### 6. Invoice Lifecycle - No Physical Delete (Scenarios C, D, J)
- Status: `DRAFT` → `COMPLETED` → `CANCELLED`
- `COMPLETED` invoices cannot be edited (app-level enforcement + FK cascade from details)
- `CANCELLED` records:
  - `cancelled_at` + `cancelled_by` (audit trail)
  - Stock reversal via new `STOCK_MOVEMENT` with `RETURN_IN`/`ADJUSTMENT_IN`
  - Loyalty reversal via new `POINT_TRANSACTION` type `REVERSAL`
  - Original records untouched

### 7. Stock Ledger as Source of Truth (Scenarios C, D, E, M)
- `STOCK_MOVEMENT` is append-only, never updated/deleted
- `quantity` sign convention: +IN, -OUT
- `movement_type` categorizes business reason
- `employee_id` = who performed operation (audit)
- `reference_document_type/id` = traceability to source (invoice, PO, adjustment)
- `reason` for manual adjustments

### 8. Atomic Sale Completion (Scenarios D, E)
```
BEGIN TRANSACTION;
  -- 1. Lock product rows for stock check
  SELECT * FROM INVENTORY_BALANCE WHERE product_id IN (...) FOR UPDATE;
  
  -- 2. Validate stock
  -- 3. Create INVOICE (status DRAFT)
  -- 4. Create INVOICE_DETAIL rows
  -- 5. Insert SALE_OUT STOCK_MOVEMENT rows (negative quantity)
  -- 6. Update INVENTORY_BALANCE (current_stock - qty, version + 1)
  -- 7. If customer has loyalty: INSERT POINT_TRANSACTION EARN
  -- 8. Update INVOICE status = COMPLETED
COMMIT;
```
- `FOR UPDATE` on balance table prevents overselling (Scenario E)
- Alternative: `SELECT FOR UPDATE` on `STOCK_MOVEMENT` with aggregation if no balance table
- Single transaction = all-or-nothing

### 9. Inventory Balance Table (Optional, for Performance)
- `INVENTORY_BALANCE` = cached `SUM(STOCK_MOVEMENT.quantity)` per product
- `version` column for optimistic locking
- Must be updated in same transaction as `STOCK_MOVEMENT` insert
- Can be implemented as `MATERIALIZED VIEW` refreshed `CONCURRENTLY`
- Never modified independently - always via stock movement transaction

### 10. Loyalty Points - Ledger + Yearly Reset (Scenarios H, I)
- `POINT_TRANSACTION` ledger = source of truth
- `transaction_type`: `EARN` (+), `REVERSAL` (-), `ADJUSTMENT` (±)
- Current balance = `SUM(points)` filtered by year
- **Yearly reset**: no UPDATE/DELETE, just filter by `occurred_at`:
  ```sql
  -- Current year balance (using Asia/Ho_Chi_Minh)
  SELECT SUM(points) 
  FROM POINT_TRANSACTION
  WHERE loyalty_account_id = $1
    AND occurred_at >= date_trunc('year', NOW() AT TIME ZONE 'Asia/Ho_Chi_Minh')
    AND occurred_at <  date_trunc('year', NOW() AT TIME ZONE 'Asia/Ho_Chi_Minh') + INTERVAL '1 year';
  ```
- 2026 points stay in 2026; 2027 starts at 0 (Scenario I)
- Invoice cancellation → `REVERSAL` transaction (Scenario J)

### 11. Loyalty Calculation Rule
- 1,000 VND = 1 point
- Based on `INVOICE.total_amount` (after discounts)
- `FLOOR(total_amount / 1000)` using integer arithmetic
- Example: 99,999 → 99; 100,000 → 100; 100,999 → 100

### 12. Referential Integrity Summary
| Parent | Child | FK Action | Reason |
|--------|-------|-----------|--------|
| EMPLOYEE | USER_ACCOUNT | RESTRICT | Auth must exist |
| EMPLOYEE | ATTENDANCE | RESTRICT | History preserved |
| EMPLOYEE | INVOICE | RESTRICT | Audit trail |
| EMPLOYEE | STOCK_MOVEMENT | RESTRICT | Audit trail |
| EMPLOYEE | INVOICE (cancelled_by) | SET NULL | If employee deleted later |
| PRODUCT | INVOICE_DETAIL | RESTRICT | Price snapshots exist |
| PRODUCT | STOCK_MOVEMENT | RESTRICT | Ledger integrity |
| CUSTOMER | INVOICE | SET NULL | Walk-in support |
| CUSTOMER | LOYALTY_ACCOUNT | CASCADE | Loyalty dies with customer |
| INVOICE | INVOICE_DETAIL | CASCADE | Details meaningless alone |
| INVOICE | POINT_TRANSACTION | SET NULL | Points may exist without invoice (adjustment) |
| LOYALTY_ACCOUNT | POINT_TRANSACTION | CASCADE | Points meaningless without account |

### 13. Critical Indexes for Scenarios
| Scenario | Index |
|----------|-------|
| E (concurrent sales) | `INVENTORY_BALANCE` PK + version for OPTIMISTIC LOCK |
| D (sale + inventory) | `STOCK_MOVEMENT(product_id, occurred_at)` for stock calc |
| G (customer purchase) | `INVOICE(customer_id, issued_at)` for history |
| H/I (loyalty) | `POINT_TRANSACTION(loyalty_account_id, occurred_at)` |
| B (attendance) | `ATTENDANCE(employee_id, check_in_at)` |

### 14. Audit Trail Coverage (Scenario N)
| Action | Actor Captured |
|--------|----------------|
| Invoice creation | `INVOICE.employee_id` |
| Invoice cancellation | `INVOICE.cancelled_by` + `cancelled_at` |
| Stock movement | `STOCK_MOVEMENT.employee_id` |
| Attendance | `ATTENDANCE.employee_id` |
| Loyalty adjustment | `POINT_TRANSACTION` (implied by transaction context) |
| Product/employee/customer changes | `updated_at` + app-level audit log (not in DB) |

---

## Scenario Validation Matrix

| Scenario | Supported? | How |
|----------|------------|-----|
| A. Employee login & RBAC | ✅ | USER_ACCOUNT + ROLE/PERMISSION |
| B. Check-in/out | ✅ | ATTENDANCE with nullable checkout |
| C. Product import | ✅ | STOCK_MOVEMENT type PURCHASE_IN |
| D. Sale + auto inventory | ✅ | Single transaction: INVOICE + DETAIL + SALE_OUT + BALANCE + POINTS |
| E. Concurrent sales | ✅ | SELECT FOR UPDATE on INVENTORY_BALANCE or SERIALIZABLE isolation |
| F. Walk-in customer | ✅ | INVOICE.customer_id nullable |
| G. Registered customer | ✅ | customer_id set, LOYALTY_ACCOUNT optional |
| H. Loyalty earn | ✅ | POINT_TRANSACTION EARN from invoice total |
| I. Yearly reset | ✅ | Date filter on occurred_at, no DELETE/UPDATE |
| J. Invoice cancel + reversals | ✅ | CANCELLED status + RETURN_IN + REVERSAL |
| K. Price changes | ✅ | Snapshots in INVOICE_DETAIL |
| L. Employee termination | ✅ | Status TERMINATED + terminated_at, FK RESTRICT |
| M. Inventory adjustment | ✅ | ADJUSTMENT_IN/OUT with reason |
| N. Audit who did what | ✅ | employee_id on all transactional tables |

---

## Implementation Notes for Spring Boot / Java

### Transaction Boundary (Scenario D)
```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public Invoice completeSale(CompleteSaleRequest req) {
    // 1. Lock balance rows
    List<InventoryBalance> balances = inventoryBalanceRepository
        .lockByProductIds(req.getProductIds()); // SELECT ... FOR UPDATE
    
    // 2. Validate stock
    validateStock(req, balances);
    
    // 3. Create invoice (DRAFT)
    Invoice invoice = invoiceRepository.save(new Invoice(...));
    
    // 4. Create details
    List<InvoiceDetail> details = createDetails(invoice, req);
    invoiceDetailRepository.saveAll(details);
    
    // 5. Create stock movements (SALE_OUT)
    createSaleOutMovements(invoice, details);
    
    // 6. Update balances
    updateBalances(balances, details);
    
    // 7. Loyalty points
    if (req.getCustomerId() != null) {
        createEarnTransaction(invoice);
    }
    
    // 8. Mark COMPLETED
    invoice.setStatus(InvoiceStatus.COMPLETED);
    return invoiceRepository.save(invoice);
}
```

### Loyalty Yearly Query (Scenario I)
```sql
-- Spring Data JPA @Query
@Query("SELECT SUM(pt.points) FROM PointTransaction pt " +
       "WHERE pt.loyaltyAccountId = :accountId " +
       "AND pt.occurredAt >= :yearStart " +
       "AND pt.occurredAt < :yearEnd")
Integer getCurrentYearPoints(Long accountId, Instant yearStart, Instant yearEnd);
```

### Stock Balance Refresh (Background Job)
```sql
-- Materialized view refresh (run nightly or after each shift)
REFRESH MATERIALIZED VIEW CONCURRENTLY INVENTORY_BALANCE;

-- Or manual sync in transaction
UPDATE INVENTORY_BALANCE ib
SET current_stock = (
    SELECT COALESCE(SUM(quantity), 0)
    FROM STOCK_MOVEMENT sm
    WHERE sm.product_id = ib.product_id
),
version = version + 1,
last_updated = NOW()
WHERE product_id = :productId;
```

---

## Files in This Design

| File | Purpose |
|------|---------|
| `retail_schema.sql` | Complete PostgreSQL DDL with all tables, constraints, indexes, triggers |
| `DESIGN_NOTES.md` | This document - design rationale and scenario validation |

---

## Extensibility Points

| Future Need | Current Support |
|-------------|-----------------|
| Loyalty redemption | Add `REDEEM` to transaction_type, track redeemed points |
| Purchase orders | Add `PURCHASE_ORDER` table, reference from STOCK_MOVEMENT |
| Multi-store | Add `STORE` table, FK on INVOICE/STOCK_MOVEMENT/EMPLOYEE |
| Serialized items | Add `SERIAL_NUMBER` table linked to STOCK_MOVEMENT |
| Promotions/discounts | Add `PROMOTION` table, reference from INVOICE_DETAIL |
| Supplier management | Add `SUPPLIER`, `PURCHASE_ORDER` tables |
| Returns/exchanges | Already supported via RETURN_IN movement type |

---

## What This Design Intentionally Does NOT Include

| Omitted | Reason |
|---------|--------|
| Generic audit_log table | Per-entity `employee_id` + `updated_at` covers 90% of needs; add only if cross-cutting compliance requires |
| Soft deletes on master data | Status fields (`ACTIVE`/`DISCONTINUED`/`TERMINATED`) are clearer and don't pollute FK |
| Polymorphic FKs | Every relationship uses real FK; `reference_document_type/id` on stock movement is informational only |
| Triggers for stock balance | Application-level transaction control is more visible and testable |
| Stored procedures | Business logic belongs in application layer (Spring Boot services) |