ALTER TABLE books
    ADD COLUMN stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0);

-- Existing rows backfill to zero available stock; the default is then dropped so every
-- future catalog insert must declare its stock explicitly rather than silently defaulting.
ALTER TABLE books
    ALTER COLUMN stock_quantity DROP DEFAULT;
