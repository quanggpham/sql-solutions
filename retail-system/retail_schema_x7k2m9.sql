-- Retail Sales System - PostgreSQL Schema
-- Designed for business integrity, auditability, and transactional consistency
-- Timezone: Asia/Ho_Chi_Minh (ICT, UTC+7)

-- 1. EMPLOYEE - Master employee data
CREATE TABLE EMPLOYEE (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'TERMINATED', 'ON_LEAVE')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    terminated_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. USER_ACCOUNT - Authentication separated from employee data
CREATE TABLE USER_ACCOUNT (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL UNIQUE REFERENCES EMPLOYEE(id) ON DELETE RESTRICT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,  -- bcrypt/scrypt/argon2 hash
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOCKED', 'EXPIRED')),
    last_login_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. ROLE - RBAC roles
CREATE TABLE ROLE (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

-- 4. PERMISSION - Fine-grained permissions
CREATE TABLE PERMISSION (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

-- 5. USER_ROLE - M:N User-Role relationship
CREATE TABLE USER_ROLE (
    user_account_id BIGINT NOT NULL REFERENCES USER_ACCOUNT(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES ROLE(id) ON DELETE CASCADE,
    PRIMARY KEY (user_account_id, role_id)
);

-- 6. ROLE_PERMISSION - M:N Role-Permission relationship
CREATE TABLE ROLE_PERMISSION (
    role_id BIGINT NOT NULL REFERENCES ROLE(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES PERMISSION(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 7. ATTENDANCE - Employee time tracking
CREATE TABLE ATTENDANCE (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES EMPLOYEE(id) ON DELETE RESTRICT,
    check_in_at TIMESTAMPTZ NOT NULL,
    check_out_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_attendance_checkout_after_checkin CHECK (
        check_out_at IS NULL OR check_out_at >= check_in_at
    )
);

-- 8. PRODUCT - Product master data
CREATE TABLE PRODUCT (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'cái',
    selling_price NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (selling_price >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISCONTINUED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. CUSTOMER - Customer master data
CREATE TABLE CUSTOMER (
    id BIGSERIAL PRIMARY KEY,
    customer_code VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLACKLISTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. INVOICE - Sales invoice header
CREATE TABLE INVOICE (
    id BIGSERIAL PRIMARY KEY,
    invoice_code VARCHAR(30) NOT NULL UNIQUE,
    employee_id BIGINT NOT NULL REFERENCES EMPLOYEE(id) ON DELETE RESTRICT,
    customer_id BIGINT NULL REFERENCES CUSTOMER(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED')),
    subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMPTZ NULL,
    cancelled_by BIGINT NULL REFERENCES EMPLOYEE(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_invoice_total_check CHECK (
        total_amount = (subtotal - discount_amount)
    )
);

-- 11. INVOICE_DETAIL - Invoice line items with price snapshots
CREATE TABLE INVOICE_DETAIL (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES INVOICE(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES PRODUCT(id) ON DELETE RESTRICT,
    product_code_snapshot VARCHAR(50) NOT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    quantity NUMERIC(10, 3) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    line_total NUMERIC(15, 2) NOT NULL GENERATED ALWAYS AS (
        (quantity * unit_price) - discount_amount
    ) STORED,
    CONSTRAINT chk_line_total_nonneg CHECK (line_total >= 0)
);

-- 12. STOCK_MOVEMENT - Historical inventory ledger (source of truth)
CREATE TABLE STOCK_MOVEMENT (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES PRODUCT(id) ON DELETE RESTRICT,
    quantity NUMERIC(10, 3) NOT NULL,  -- Positive = IN, Negative = OUT
    movement_type VARCHAR(20) NOT NULL CHECK (
        movement_type IN (
            'PURCHASE_IN',
            'SALE_OUT',
            'RETURN_IN',
            'ADJUSTMENT_IN',
            'ADJUSTMENT_OUT'
        )
    ),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    employee_id BIGINT NOT NULL REFERENCES EMPLOYEE(id) ON DELETE RESTRICT,
    reference_document_type VARCHAR(50),  -- e.g., 'INVOICE', 'PO', 'ADJUSTMENT'
    reference_document_id BIGINT NULL,    -- FK to invoice_id, po_id, etc.
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. INVENTORY_BALANCE - Optional cached current stock (for performance)
-- This is a MATERIALIZED VIEW in practice, but table form shown for clarity
-- In reality: CREATE MATERIALIZED VIEW INVENTORY_BALANCE AS ... REFRESH CONCURRENTLY
CREATE TABLE INVENTORY_BALANCE (
    product_id BIGINT PRIMARY KEY REFERENCES PRODUCT(id) ON DELETE CASCADE,
    current_stock NUMERIC(10, 3) NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0  -- For optimistic locking
);

-- 14. LOYALTY_ACCOUNT - One per customer
CREATE TABLE LOYALTY_ACCOUNT (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE REFERENCES CUSTOMER(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 15. POINT_TRANSACTION - Loyalty point ledger
CREATE TABLE POINT_TRANSACTION (
    id BIGSERIAL PRIMARY KEY,
    loyalty_account_id BIGINT NOT NULL REFERENCES LOYALTY_ACCOUNT(id) ON DELETE CASCADE,
    invoice_id BIGINT NULL REFERENCES INVOICE(id) ON DELETE SET NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (
        transaction_type IN ('EARN', 'REVERSAL', 'ADJUSTMENT')
    ),
    points INTEGER NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- INDEXES FOR PERFORMANCE
-- =============================================================================

-- Master data lookups
CREATE INDEX idx_employee_code ON EMPLOYEE(employee_code);
CREATE INDEX idx_employee_status ON EMPLOYEE(status);
CREATE INDEX idx_user_account_username ON USER_ACCOUNT(username);
CREATE INDEX idx_user_account_employee ON USER_ACCOUNT(employee_id);
CREATE INDEX idx_product_code ON PRODUCT(product_code);
CREATE INDEX idx_product_status ON PRODUCT(status);
CREATE INDEX idx_customer_code ON CUSTOMER(customer_code);
CREATE INDEX idx_customer_status ON CUSTOMER(status);

-- Transactional tables - critical for reporting
CREATE INDEX idx_invoice_code ON INVOICE(invoice_code);
CREATE INDEX idx_invoice_employee ON INVOICE(employee_id);
CREATE INDEX idx_invoice_customer ON INVOICE(customer_id);
CREATE INDEX idx_invoice_status ON INVOICE(status);
CREATE INDEX idx_invoice_issued_at ON INVOICE(issued_at);
CREATE INDEX idx_invoice_customer_issued ON INVOICE(customer_id, issued_at);
CREATE INDEX idx_invoice_employee_issued ON INVOICE(employee_id, issued_at);

CREATE INDEX idx_invoice_detail_invoice ON INVOICE_DETAIL(invoice_id);
CREATE INDEX idx_invoice_detail_product ON INVOICE_DETAIL(product_id);
CREATE INDEX idx_invoice_detail_product_code ON INVOICE_DETAIL(product_code_snapshot);

CREATE INDEX idx_stock_movement_product ON STOCK_MOVEMENT(product_id);
CREATE INDEX idx_stock_movement_occurred ON STOCK_MOVEMENT(occurred_at);
CREATE INDEX idx_stock_movement_product_occurred ON STOCK_MOVEMENT(product_id, occurred_at);
CREATE INDEX idx_stock_movement_employee ON STOCK_MOVEMENT(employee_id);
CREATE INDEX idx_stock_movement_type ON STOCK_MOVEMENT(movement_type);
CREATE INDEX idx_stock_movement_reference ON STOCK_MOVEMENT(reference_document_type, reference_document_id);

CREATE INDEX idx_attendance_employee_checkin ON ATTENDANCE(employee_id, check_in_at);
CREATE INDEX idx_attendance_employee ON ATTENDANCE(employee_id);
CREATE INDEX idx_attendance_date ON ATTENDANCE(check_in_at);

CREATE INDEX idx_loyalty_account_customer ON LOYALTY_ACCOUNT(customer_id);
CREATE INDEX idx_loyalty_account_status ON LOYALTY_ACCOUNT(status);

CREATE INDEX idx_point_transaction_account ON POINT_TRANSACTION(loyalty_account_id);
CREATE INDEX idx_point_transaction_account_occurred ON POINT_TRANSACTION(loyalty_account_id, occurred_at);
CREATE INDEX idx_point_transaction_invoice ON POINT_TRANSACTION(invoice_id);
CREATE INDEX idx_point_transaction_type ON POINT_TRANSACTION(transaction_type);
CREATE INDEX idx_point_transaction_occurred ON POINT_TRANSACTION(occurred_at);

-- =============================================================================
-- TRIGGERS FOR AUTOMATIC UPDATED_AT
-- =============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply to all tables that need updated_at
DO $$
DECLARE
    tables TEXT[] := ARRAY[
        'EMPLOYEE', 'USER_ACCOUNT', 'ROLE', 'PERMISSION',
        'ATTENDANCE', 'PRODUCT', 'CUSTOMER', 'INVOICE',
        'LOYALTY_ACCOUNT', 'INVENTORY_BALANCE'
    ];
    t TEXT;
BEGIN
    FOREACH t IN ARRAY tables LOOP
        EXECUTE format(
            'DROP TRIGGER IF EXISTS update_%s_updated_at ON %s;',
            t, t
        );
        EXECUTE format(
            'CREATE TRIGGER update_%s_updated_at BEFORE UPDATE ON %s FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();',
            t, t
        );
    END LOOP;
END $$;

-- =============================================================================
-- COMMENTS FOR DOCUMENTATION
-- =============================================================================

COMMENT ON TABLE EMPLOYEE IS 'Master employee data - never deleted if referenced in historical transactions';
COMMENT ON COLUMN EMPLOYEE.status IS '''ACTIVE'' | ''INACTIVE'' | ''TERMINATED'' | ''ON_LEAVE''';
COMMENT ON COLUMN EMPLOYEE.terminated_at IS 'NULL while active; set when employment ends';

COMMENT ON TABLE USER_ACCOUNT IS 'Authentication credentials - separated from employee data for security';
COMMENT ON COLUMN USER_ACCOUNT.password_hash IS 'bcrypt/scrypt/argon2 hash - NEVER store plaintext';

COMMENT ON TABLE ATTENDANCE IS 'Time tracking - supports multiple shifts per day; check_out_at NULL means currently working';

COMMENT ON TABLE PRODUCT IS 'Product master - selling_price is current price; historical prices preserved in snapshots';
COMMENT ON COLUMN PRODUCT.status IS '''ACTIVE'' | ''DISCONTINUED'' | ''ARCHIVED'' - never delete if used in transactions';

COMMENT ON TABLE CUSTOMER IS 'Customer master - phone/email not necessarily unique';
COMMENT ON COLUMN CUSTOMER.status IS '''ACTIVE'' | ''INACTIVE'' | ''BLACKLISTED''';

COMMENT ON TABLE INVOICE IS 'Sales invoice header - status lifecycle prevents deletion/editing of completed invoices';
COMMENT ON COLUMN INVOICE.status IS '''DRAFT'' | ''COMPLETED'' | ''CANCELLED''';
COMMENT ON COLUMN INVOICE.cancelled_at IS 'SET when invoice is cancelled';
COMMENT ON COLUMN INVOICE.cancelled_by IS 'Employee who performed cancellation';

COMMENT ON TABLE INVOICE_DETAIL IS 'Invoice line items with price/name snapshots for historical accuracy';
COMMENT ON COLUMN INVOICE_DETAIL.product_code_snapshot IS 'Product code at time of sale';
COMMENT ON COLUMN INVOICE_DETAIL.product_name_snapshot IS 'Product name at time of sale';

COMMENT ON TABLE STOCK_MOVEMENT IS 'Historical stock ledger - SOURCE OF TRUTH for inventory';
COMMENT ON COLUMN STOCK_MOVEMENT.quantity IS 'Positive = IN (receipt/return/adjustment-in), Negative = OUT (sale/adjustment-out)';
COMMENT ON COLUMN STOCK_MOVEMENT.movement_type IS '''PURCHASE_IN'', ''SALE_OUT'', ''RETURN_IN'', ''ADJUSTMENT_IN'', ''ADJUSTMENT_OUT''';
COMMENT ON COLUMN STOCK_MOVEMENT.reference_document_type IS 'Optional: ''INVOICE'', ''PURCHASE_ORDER'', etc.';
COMMENT ON COLUMN STOCK_MOVEMENT.reference_document_id IS 'FK to referenced document (invoice_id, po_id, etc.)';

COMMENT ON TABLE INVENTORY_BALANCE IS 'OPTIONAL: Cached current stock for performance - MUST be kept in sync with STOCK_MOVEMENT';
COMMENT ON COLUMN INVENTORY_BALANCE.version IS 'Used for optimistic locking during concurrent updates';

COMMENT ON TABLE LOYALTY_ACCOUNT IS 'One-to-one with CUSTOMER for loyalty program participation';
COMMENT ON TABLE POINT_TRANSACTION IS 'Point ledger - SOURCE OF TRUTH for loyalty points';
COMMENT ON COLUMN POINT_TRANSACTION.transaction_type IS '''EARN'' (points gained), ''REVERSAL'' (points removed due to invoice cancellation), ''ADJUSTMENT'' (manual correction)';
COMMENT ON COLUMN POINT_TRANSACTION.points IS 'Can be negative for REVERSAL/ADJUSTMENT_OUT';

-- =============================================================================
-- VALIDATION QUERIES (run after inserting sample data)
-- =============================================================================

-- Verify no negative inventory (unless business allows)
-- SELECT product_id, SUM(quantity) as current_stock
-- FROM STOCK_MOVEMENT
-- GROUP BY product_id
-- HAVING SUM(quantity) < 0;

-- Verify invoice math
-- SELECT id, subtotal, discount_amount, total_amount
-- FROM INVOICE
-- WHERE total_amount <> (subtotal - discount_amount);

-- Verify loyalty calculation
-- SELECT pt.id, lt.id as loyalty_account, pt.points, i.total_amount
-- FROM POINT_TRANSACTION pt
-- JOIN LOYALTY_ACCOUNT lt ON pt.loyalty_account_id = lt.id
-- LEFT JOIN INVOICE i ON pt.invoice_id = i.id
-- WHERE pt.transaction_type = 'EARN'
-- AND pt.points <> FLOOR((COALESCE(i.total_amount, 0)) / 1000);

-- Verify attendance logic
-- SELECT * FROM ATTENDANCE
-- WHERE check_out_at IS NOT NULL AND check_out_at < check_in_at;