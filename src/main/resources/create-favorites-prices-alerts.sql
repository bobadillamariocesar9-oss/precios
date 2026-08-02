-- ═══════════════════════════════════════════════════════════════
-- Tablas: prices, favorites, price_alerts
-- Requiere que existan primero: users, products, stores
-- ═══════════════════════════════════════════════════════════════

-- ── prices  ← entity Price ───────────────────────────────────
-- Campos: id, price, currency, recorded_at, product_id, store_id
CREATE TABLE IF NOT EXISTS prices (
    id          SERIAL        PRIMARY KEY,
    price       NUMERIC(12,2) NOT NULL,
    currency    VARCHAR(10),
    recorded_at TIMESTAMP,
    product_id  INTEGER       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    store_id    INTEGER       REFERENCES stores(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_prices_product  ON prices(product_id);
CREATE INDEX IF NOT EXISTS idx_prices_store    ON prices(store_id);
CREATE INDEX IF NOT EXISTS idx_prices_recorded ON prices(recorded_at);


-- ── favorites  ← entity Favorite ─────────────────────────────
-- Campos: id, created_at, user_id, product_id
-- La entidad declara @UniqueConstraint(user_id, product_id)
CREATE TABLE IF NOT EXISTS favorites (
    id         SERIAL    PRIMARY KEY,
    created_at TIMESTAMP DEFAULT NOW(),
    user_id    INTEGER   NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    product_id INTEGER   NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_favorites_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_favorites_user    ON favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_product ON favorites(product_id);


-- ── price_alerts  ← entity PriceAlert ────────────────────────
-- Campos: id, target_price, active, notified_at, created_at, user_id, product_id
CREATE TABLE IF NOT EXISTS price_alerts (
    id           SERIAL        PRIMARY KEY,
    target_price NUMERIC(12,2) NOT NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    notified_at  TIMESTAMP,
    created_at   TIMESTAMP     DEFAULT NOW(),
    user_id      INTEGER       NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    product_id   INTEGER       NOT NULL REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_alerts_user    ON price_alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_alerts_product ON price_alerts(product_id);
CREATE INDEX IF NOT EXISTS idx_alerts_active  ON price_alerts(active);


-- ── Migración: si las tablas YA existían sin estas columnas ──
ALTER TABLE prices       ADD COLUMN IF NOT EXISTS currency    VARCHAR(10);
ALTER TABLE prices       ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP;
ALTER TABLE price_alerts ADD COLUMN IF NOT EXISTS active      BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE price_alerts ADD COLUMN IF NOT EXISTS notified_at TIMESTAMP;
ALTER TABLE price_alerts ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP DEFAULT NOW();
ALTER TABLE favorites    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP DEFAULT NOW();


-- ── Verificación ─────────────────────────────────────────────
-- Ejecuta esto después para confirmar que las 3 tablas existen:
--
--   SELECT table_name FROM information_schema.tables
--   WHERE table_schema = 'public'
--   ORDER BY table_name;
--
-- Deben aparecer las 9: category, favorites, price_alerts, prices,
-- products, scraping_jobs, scrapings_logs, stores, users
