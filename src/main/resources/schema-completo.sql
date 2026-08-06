-- ═══════════════════════════════════════════════════════════════
-- DDL COMPLETO — generado desde las 9 entidades JPA de apiPrecios
-- Base: apiPrecios (PostgreSQL)
--
-- Orden de creación respeta las FK:
--   1. users, stores, category  (sin dependencias)
--   2. products                 (→ stores)
--   3. prices, favorites, price_alerts, scraping_jobs
--   4. scrapings_logs           (→ scraping_jobs)
-- ═══════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────
-- 1. TABLAS BASE
-- ─────────────────────────────────────────────────────────────

-- users  ← entity User
CREATE TABLE IF NOT EXISTS users (
    id         SERIAL       PRIMARY KEY,
    username   VARCHAR(100) NOT NULL UNIQUE,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP    DEFAULT NOW()
);

-- stores  ← entity Store
CREATE TABLE IF NOT EXISTS stores (
    id         SERIAL       PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    base_url   VARCHAR(500),
    logo_url   VARCHAR(500),
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP    DEFAULT NOW()
);

-- category  ← entity Category
CREATE TABLE IF NOT EXISTS category (
    id           SERIAL       PRIMARY KEY,
    nombre_categ VARCHAR(100) NOT NULL,
    categ        VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP    DEFAULT NOW(),
    updated_at   TIMESTAMP    DEFAULT NOW()
);


-- ─────────────────────────────────────────────────────────────
-- 2. PRODUCTS (depende de stores)
-- ─────────────────────────────────────────────────────────────

-- products  ← entity Product
CREATE TABLE IF NOT EXISTS products (
    id          SERIAL       PRIMARY KEY,
    name        VARCHAR(300) NOT NULL,
    brand       VARCHAR(150),
    description TEXT,
    category    VARCHAR(100),
    image_url   VARCHAR(500),
    url         VARCHAR(500),
    store_id    INTEGER      REFERENCES stores(id) ON DELETE SET NULL,
    created_at  TIMESTAMP    DEFAULT NOW(),
    updated_at  TIMESTAMP    DEFAULT NOW()
);


-- ─────────────────────────────────────────────────────────────
-- 3. TABLAS DEPENDIENTES
-- ─────────────────────────────────────────────────────────────

-- prices  ← entity Price
CREATE TABLE IF NOT EXISTS prices (
    id          SERIAL         PRIMARY KEY,
    price       NUMERIC(12,2)  NOT NULL,
    currency    VARCHAR(10),
    recorded_at TIMESTAMP,
    product_id  INTEGER        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    store_id    INTEGER        REFERENCES stores(id) ON DELETE SET NULL
);

-- favorites  ← entity Favorite (constraint única user+product)
CREATE TABLE IF NOT EXISTS favorites (
    id         SERIAL    PRIMARY KEY,
    user_id    INTEGER   NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    product_id INTEGER   NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_favorites_user_product UNIQUE (user_id, product_id)
);

-- price_alerts  ← entity PriceAlert
CREATE TABLE IF NOT EXISTS price_alerts (
    id           SERIAL        PRIMARY KEY,
    target_price NUMERIC(12,2) NOT NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    notified_at  TIMESTAMP,
    created_at   TIMESTAMP     DEFAULT NOW(),
    user_id      INTEGER       NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    product_id   INTEGER       NOT NULL REFERENCES products(id) ON DELETE CASCADE
);

-- scraping_jobs  ← entity ScrapingJob (status es enum STRING)
CREATE TABLE IF NOT EXISTS scraping_jobs (
    id           SERIAL      PRIMARY KEY,
    url          VARCHAR(500),
    status       VARCHAR(20) DEFAULT 'PENDING',
    scheduled_at TIMESTAMP,
    started_at   TIMESTAMP,
    finished_at  TIMESTAMP,
    created_at   TIMESTAMP   DEFAULT NOW(),
    store_id     INTEGER     REFERENCES stores(id) ON DELETE SET NULL
);


-- ─────────────────────────────────────────────────────────────
-- 4. LOGS (depende de scraping_jobs)
-- ─────────────────────────────────────────────────────────────

-- scrapings_logs  ← entity ScrapingLog (level es enum STRING)
CREATE TABLE IF NOT EXISTS scrapings_logs (
    id              SERIAL      PRIMARY KEY,
    message         TEXT,
    level           VARCHAR(10) DEFAULT 'INFO',
    created_at      TIMESTAMP   DEFAULT NOW(),
    scraping_job_id INTEGER     NOT NULL REFERENCES scraping_jobs(id) ON DELETE CASCADE
);


-- ─────────────────────────────────────────────────────────────
-- 5. ÍNDICES recomendados
-- ─────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_products_store     ON products(store_id);
CREATE INDEX IF NOT EXISTS idx_products_category  ON products(category);
CREATE INDEX IF NOT EXISTS idx_prices_product     ON prices(product_id);
CREATE INDEX IF NOT EXISTS idx_prices_store       ON prices(store_id);
CREATE INDEX IF NOT EXISTS idx_prices_recorded    ON prices(recorded_at);
CREATE INDEX IF NOT EXISTS idx_favorites_user     ON favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_alerts_user        ON price_alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_alerts_active      ON price_alerts(active);
CREATE INDEX IF NOT EXISTS idx_jobs_store         ON scraping_jobs(store_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status        ON scraping_jobs(status);
CREATE INDEX IF NOT EXISTS idx_logs_job           ON scrapings_logs(scraping_job_id);


-- ─────────────────────────────────────────────────────────────
-- 6. MIGRACIÓN — para tablas que YA existen sin estas columnas
-- ─────────────────────────────────────────────────────────────

ALTER TABLE products      ADD COLUMN IF NOT EXISTS brand      VARCHAR(150);
ALTER TABLE products      ADD COLUMN IF NOT EXISTS category   VARCHAR(100);
ALTER TABLE products      ADD COLUMN IF NOT EXISTS image_url  VARCHAR(500);
ALTER TABLE products      ADD COLUMN IF NOT EXISTS url        VARCHAR(500);
ALTER TABLE stores        ADD COLUMN IF NOT EXISTS base_url   VARCHAR(500);
ALTER TABLE stores        ADD COLUMN IF NOT EXISTS logo_url   VARCHAR(500);
ALTER TABLE price_alerts  ADD COLUMN IF NOT EXISTS active     BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE price_alerts  ADD COLUMN IF NOT EXISTS notified_at TIMESTAMP;
ALTER TABLE prices        ADD COLUMN IF NOT EXISTS currency   VARCHAR(10);
ALTER TABLE prices        ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP;


-- ─────────────────────────────────────────────────────────────
-- 7. DATOS INICIALES
-- ─────────────────────────────────────────────────────────────

INSERT INTO stores (name, base_url, created_at, updated_at)
SELECT 'Supermercado Demo', 'https://demo.com', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE name = 'Supermercado Demo');

INSERT INTO category (nombre_categ, categ, created_at, updated_at)
SELECT v.nombre_categ, v.categ, NOW(), NOW()
FROM (VALUES
  ('Alimento',     'ALIMENTO'),
  ('Bebidas',      'BEBIDAS'),
  ('Indumentaria', 'INDUMENTARIA')
) AS v(nombre_categ, categ)
WHERE NOT EXISTS (
  SELECT 1 FROM category c WHERE c.nombre_categ = v.nombre_categ
);
