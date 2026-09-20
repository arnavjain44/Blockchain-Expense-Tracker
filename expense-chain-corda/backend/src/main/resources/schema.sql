-- =========================================================================
-- ExpenseChain PostgreSQL Schema
-- =========================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    corda_x500_name VARCHAR(255),
    created_at VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at VARCHAR(50) NOT NULL,
    CONSTRAINT uq_group_member UNIQUE (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS expenses (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    description TEXT,
    expense_date VARCHAR(50) NOT NULL,
    paid_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    split_type VARCHAR(50) NOT NULL DEFAULT 'EQUAL',
    corda_tx_id VARCHAR(255),
    created_at VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS expense_splits (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    share_amount DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS settlements (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    paid_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    paid_to BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    corda_tx_id VARCHAR(255),
    created_at VARCHAR(50) NOT NULL,
    settled_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_group_members_user ON group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_group_members_group ON group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_expenses_group ON expenses(group_id);
CREATE INDEX IF NOT EXISTS idx_expenses_paid_by ON expenses(paid_by);
CREATE INDEX IF NOT EXISTS idx_expense_splits_expense ON expense_splits(expense_id);
CREATE INDEX IF NOT EXISTS idx_expense_splits_user ON expense_splits(user_id);
CREATE INDEX IF NOT EXISTS idx_settlements_group ON settlements(group_id);
CREATE INDEX IF NOT EXISTS idx_settlements_paid_by ON settlements(paid_by);
CREATE INDEX IF NOT EXISTS idx_settlements_paid_to ON settlements(paid_to);
