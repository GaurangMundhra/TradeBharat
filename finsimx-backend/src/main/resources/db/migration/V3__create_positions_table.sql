CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    asset VARCHAR(50) NOT NULL,

    quantity DECIMAL(20,8) NOT NULL DEFAULT 0,

    average_price DECIMAL(20,8) NOT NULL DEFAULT 0,

    realized_pnl DECIMAL(20,8) DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_positions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);