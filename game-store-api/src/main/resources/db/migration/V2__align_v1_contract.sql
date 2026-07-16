ALTER TABLE app_users
    ADD COLUMN display_name VARCHAR(100) NOT NULL DEFAULT 'Customer';

ALTER TABLE games
    ADD COLUMN genre VARCHAR(100) NOT NULL DEFAULT 'Uncategorized';

ALTER TABLE games
    ADD COLUMN platform VARCHAR(100) NOT NULL DEFAULT 'Unknown';

ALTER TABLE games
    MODIFY COLUMN price DECIMAL(12, 2) NOT NULL;

ALTER TABLE purchase_items
    MODIFY COLUMN unit_price DECIMAL(12, 2) NOT NULL;

ALTER TABLE purchases
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'EUR';

ALTER TABLE games
    ADD CONSTRAINT chk_games_price_positive CHECK (price > 0);

ALTER TABLE purchases
    ADD CONSTRAINT chk_purchases_currency_eur CHECK (currency = 'EUR');

CREATE INDEX idx_games_catalog_filters
    ON games (active, genre, platform, price, created_at);

CREATE INDEX idx_games_title
    ON games (title);

CREATE INDEX idx_purchase_items_game_purchase
    ON purchase_items (game_id, purchase_id);
