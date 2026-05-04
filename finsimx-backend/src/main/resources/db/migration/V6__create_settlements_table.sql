CREATE TABLE settlements (
    id BIGSERIAL PRIMARY KEY,

    trade_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,

    asset VARCHAR(50) NOT NULL,
    quantity DECIMAL(20,8) NOT NULL,
    price DECIMAL(20,8) NOT NULL,
    total_value DECIMAL(20,8) NOT NULL,

    settlement_status VARCHAR(30) DEFAULT 'PENDING',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_settlement_trade
        FOREIGN KEY (trade_id)
        REFERENCES trades(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_settlement_buyer
        FOREIGN KEY (buyer_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_settlement_seller
        FOREIGN KEY (seller_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);