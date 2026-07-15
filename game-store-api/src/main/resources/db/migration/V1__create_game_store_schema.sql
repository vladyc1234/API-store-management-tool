CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT chk_app_users_email_not_blank CHECK (CHAR_LENGTH(TRIM(email)) > 3),
    CONSTRAINT chk_app_users_password_not_blank CHECK (CHAR_LENGTH(TRIM(password_hash)) > 0),
    CONSTRAINT chk_app_users_role CHECK (role IN ('BUYER', 'MANAGER'))
);

CREATE TABLE games (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000),
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_games_sku UNIQUE (sku),
    CONSTRAINT chk_games_sku_not_blank CHECK (CHAR_LENGTH(TRIM(sku)) > 0),
    CONSTRAINT chk_games_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_games_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_games_stock_non_negative CHECK (stock_quantity >= 0),
    CONSTRAINT chk_games_version_non_negative CHECK (version >= 0)
);

CREATE TABLE purchases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    buyer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_purchases_buyer FOREIGN KEY (buyer_id) REFERENCES app_users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_purchases_status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_purchases_total_non_negative CHECK (total_amount >= 0)
);

CREATE TABLE purchase_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    purchase_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    game_title VARCHAR(200) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_purchase_items_purchase_game UNIQUE (purchase_id, game_id),
    CONSTRAINT fk_purchase_items_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_items_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE RESTRICT,
    CONSTRAINT chk_purchase_items_title_not_blank CHECK (CHAR_LENGTH(TRIM(game_title)) > 0),
    CONSTRAINT chk_purchase_items_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT chk_purchase_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_purchase_items_total_non_negative CHECK (line_total >= 0),
    CONSTRAINT chk_purchase_items_total_matches CHECK (line_total = unit_price * quantity)
);

CREATE INDEX idx_purchases_buyer_created_at ON purchases (buyer_id, created_at);
CREATE INDEX idx_purchases_status_created_at ON purchases (status, created_at);
CREATE INDEX idx_purchase_items_game ON purchase_items (game_id);
