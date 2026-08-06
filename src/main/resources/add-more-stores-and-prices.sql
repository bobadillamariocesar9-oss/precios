-- ═══════════════════════════════════════════════════════════════
-- Agrega 5 tiendas más con direcciones y coordenadas reales,
-- para que la búsqueda por radio (km) tenga sentido: algunas
-- quedan cerca de CABA y otras a cientos de km (quedan afuera
-- del radio salvo que el usuario lo agrande mucho).
--
-- Cercanas a CABA (dentro de ~10 km del Obelisco):
--   - Carrefour Palermo
--   - Dia Caballito
-- Lejanas (fuera de radios normales, > 250 km):
--   - Jumbo Rosario     (~300 km)
--   - La Anónima Mar del Plata (~400 km)
--   - Carrefour Córdoba (~650 km)
--
-- Idempotente: usa WHERE NOT EXISTS para las tiendas y borra los
-- precios previos de estas tiendas antes de insertar de nuevo.
-- ═══════════════════════════════════════════════════════════════

-- ── Tiendas nuevas ──────────────────────────────────────────────
INSERT INTO stores (name, base_url, address, latitude, longitude, created_at, updated_at)
SELECT v.name, v.base_url, v.address, v.lat, v.lng, NOW(), NOW()
FROM (VALUES
  ('Carrefour Palermo',        'https://www.carrefour.com.ar', 'Av. Santa Fe 3450, Palermo, CABA',        -34.5875, -58.4306),
  ('Dia Caballito',            'https://diaonline.supermercadosdia.com.ar', 'Av. Rivadavia 5300, Caballito, CABA', -34.6178, -58.4392),
  ('Jumbo Rosario',            'https://www.jumbo.com.ar',     'Av. Pellegrini 1400, Rosario, Santa Fe',   -32.9468, -60.6393),
  ('La Anónima Mar del Plata', 'https://www.laanonimaonline.com', 'Av. Juan B. Justo 2500, Mar del Plata, Buenos Aires', -37.9979, -57.5514),
  ('Carrefour Córdoba',        'https://www.carrefour.com.ar', 'Av. Colón 1200, Córdoba Capital, Córdoba', -31.4201, -64.1888)
) AS v(name, base_url, address, lat, lng)
WHERE NOT EXISTS (SELECT 1 FROM stores s WHERE s.name = v.name);

-- Por si la tienda ya existía sin dirección/coordenadas, las completa
UPDATE stores SET address = 'Av. Santa Fe 3450, Palermo, CABA', latitude = -34.5875, longitude = -58.4306
WHERE name = 'Carrefour Palermo' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. Rivadavia 5300, Caballito, CABA', latitude = -34.6178, longitude = -58.4392
WHERE name = 'Dia Caballito' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. Pellegrini 1400, Rosario, Santa Fe', latitude = -32.9468, longitude = -60.6393
WHERE name = 'Jumbo Rosario' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. Juan B. Justo 2500, Mar del Plata, Buenos Aires', latitude = -37.9979, longitude = -57.5514
WHERE name = 'La Anónima Mar del Plata' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. Colón 1200, Córdoba Capital, Córdoba', latitude = -31.4201, longitude = -64.1888
WHERE name = 'Carrefour Córdoba' AND (address IS NULL OR latitude IS NULL);

-- ── Limpia precios previos de estas tiendas (idempotente) ────────
DELETE FROM prices
WHERE store_id IN (
  SELECT id FROM stores WHERE name IN (
    'Carrefour Palermo','Dia Caballito','Jumbo Rosario',
    'La Anónima Mar del Plata','Carrefour Córdoba'
  )
);

-- ── Precios: mismo set base de 24 productos, con un multiplicador
--    distinto por tienda para que los precios varíen de forma
--    realista (más baratas/caras según la cadena) ────────────────
WITH base_prices AS (
  SELECT p.id AS product_id, v.price AS base_price
  FROM (VALUES
    ('Arroz Largo Fino 1kg',     2200.00),
    ('Fideos Spaghetti 500g',    1200.00),
    ('Aceite de Girasol 1.5L',   3200.00),
    ('Azúcar Común 1kg',         1400.00),
    ('Harina 000 1kg',           1100.00),
    ('Sal Fina 500g',             700.00),
    ('Galletitas Agua 200g',     1300.00),
    ('Atún al Natural 170g',     2800.00),
    ('Agua Mineral 2L',          1400.00),
    ('Gaseosa Cola 1.5L',        2600.00),
    ('Jugo de Naranja 1L',       1800.00),
    ('Cerveza Lager 1L',         2200.00),
    ('Vino Tinto 750ml',         4500.00),
    ('Leche Entera 1L',          1700.00),
    ('Infusión Té x20',          1600.00),
    ('Café Molido 250g',         4200.00),
    ('Remera Algodón Hombre',   16000.00),
    ('Jean Slim Mujer',         35000.00),
    ('Buzo con Capucha',        20000.00),
    ('Campera Impermeable',     48000.00),
    ('Medias Deportivas x3',     9000.00),
    ('Zapatillas Running',      85000.00),
    ('Camiseta Deportiva',      28000.00),
    ('Bermuda Cargo',           22000.00)
  ) AS v(name, price)
  JOIN products p ON p.name = v.name
),
store_mult AS (
  SELECT s.id AS store_id, m.multiplier
  FROM stores s
  JOIN (VALUES
    ('Carrefour Palermo',        0.95),
    ('Dia Caballito',            1.05),
    ('Jumbo Rosario',            0.90),
    ('La Anónima Mar del Plata', 1.12),
    ('Carrefour Córdoba',        0.88)
  ) AS m(name, multiplier) ON m.name = s.name
)
INSERT INTO prices (price, currency, recorded_at, product_id, store_id)
SELECT ROUND((bp.base_price * sm.multiplier)::numeric, 2), 'ARS', NOW(), bp.product_id, sm.store_id
FROM base_prices bp
CROSS JOIN store_mult sm;

-- ── Verificación ─────────────────────────────────────────────
-- SELECT s.name, s.address, s.latitude, s.longitude, COUNT(pr.id) AS precios
-- FROM stores s LEFT JOIN prices pr ON pr.store_id = s.id
-- GROUP BY s.id, s.name, s.address, s.latitude, s.longitude
-- ORDER BY s.name;
