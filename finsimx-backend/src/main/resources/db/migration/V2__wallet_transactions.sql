-- Create wallet_transactions table for tracking all balance changes
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    transaction_type VARCHAR(30) NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'BUY_ORDER', 'SELL_ORDER', 'TRADE_EXECUTION', 'REFUND')),
    amount DECIMAL(20, 2) NOT NULL,
    balance_before DECIMAL(20, 2) NOT NULL,
    balance_after DECIMAL(20, 2) NOT NULL,
    description VARCHAR(255),
    reference_id VARCHAR(50) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_transaction_amount CHECK (amount > 0),
    CONSTRAINT check_balances CHECK (balance_before >= 0 AND balance_after >= 0)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user_id ON wallet_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_created_at ON wallet_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user_type ON wallet_transactions(user_id, transaction_type);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_reference_id ON wallet_transactions(reference_id);
