-- ═══════════════════════════════════════════════════════════════
-- Agrega 2 productos nuevos a la categoría Alimento/Bebidas:
--   - Leche Descremada 1L  (Bebidas, junto a Leche Entera 1L)
--   - Queso Blanco 370g    (Alimento)
--
-- Y les carga precio en TODAS las tiendas existentes, reusando el
-- mismo multiplicador que ya tiene cada tienda (calculado a partir
-- del precio de 'Leche Entera 1L', que ya existe en todas), para
-- mantener la variación de precios realista entre cadenas.
--
-- Idempotente: usa WHERE NOT EXISTS para los productos y borra los
-- precios previos de estos 2 productos antes de insertar de nuevo.
-- ═══════════════════════════════════════════════════════════════

-- ── Productos nuevos ──────────────────────────────────────────
INSERT INTO products (name, brand, description, category, created_at, updated_at)
SELECT v.name, v.brand, v.description, v.category, NOW(), NOW()
FROM (VALUES
  ('Leche Descremada 1L', 'La Serenísima', 'Leche descremada larga vida', 'Bebidas'),
  ('Queso Blanco 370g',   'Sancor',        'Queso blanco untable clásico', 'Alimento')
) AS v(name, brand, description, category)
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = v.name);

-- ── Limpia precios previos de estos productos (idempotente) ────
DELETE FROM prices
WHERE product_id IN (
  SELECT id FROM products WHERE name IN ('Leche Descremada 1L', 'Queso Blanco 370g')
);

-- ── Precios en todas las tiendas, usando el multiplicador que ya
--    tiene cada una (precio actual de Leche Entera 1L / 1700, su
--    precio base) para mantener la variación entre cadenas ──────
WITH store_mult AS (
  SELECT pr.store_id, pr.price / 1700.0 AS multiplier
  FROM prices pr
  JOIN products p ON p.id = pr.product_id
  WHERE p.name = 'Leche Entera 1L'
),
base_prices AS (
  SELECT p.id AS product_id, v.price AS base_price
  FROM (VALUES
    ('Leche Descremada 1L', 1800.00),
    ('Queso Blanco 370g',   3600.00)
  ) AS v(name, price)
  JOIN products p ON p.name = v.name
)
INSERT INTO prices (price, currency, recorded_at, product_id, store_id)
SELECT ROUND((bp.base_price * sm.multiplier)::numeric, 2), 'ARS', NOW(), bp.product_id, sm.store_id
FROM base_prices bp
CROSS JOIN store_mult sm;

-- ── Verificación ─────────────────────────────────────────────
-- SELECT pr.name, price.price, s.name AS tienda
-- FROM prices price
-- JOIN products pr ON pr.id = price.product_id
-- JOIN stores s ON s.id = price.store_id
-- WHERE pr.name IN ('Leche Descremada 1L','Queso Blanco 370g')
-- ORDER BY pr.name, price.price;
