-- ═══════════════════════════════════════════════════════════════
-- Agrega 3 tiendas a menos de 1 km del punto de referencia usado
-- como "tu ubicación" en las pruebas (Obelisco, -34.6037,-58.3816),
-- para que el radio mínimo del slider (1 km) también devuelva
-- resultados.
--
-- Distancia aproximada desde el Obelisco:
--   Coto Microcentro    ~0.2 km
--   Dia San Nicolás     ~0.5 km
--   Carrefour Congreso  ~0.9 km
--
-- Idempotente: usa WHERE NOT EXISTS para las tiendas y borra los
-- precios previos de estas tiendas antes de insertar de nuevo.
-- ═══════════════════════════════════════════════════════════════

-- ── Tiendas nuevas ──────────────────────────────────────────────
INSERT INTO stores (name, base_url, address, latitude, longitude, created_at, updated_at)
SELECT v.name, v.base_url, v.address, v.lat, v.lng, NOW(), NOW()
FROM (VALUES
  ('Coto Microcentro',   'https://www.cotodigital.com.ar',            'Av. 9 de Julio 500, CABA',     -34.6019, -58.3816),
  ('Dia San Nicolás',    'https://diaonline.supermercadosdia.com.ar', 'Av. de Mayo 800, CABA',         -34.6037, -58.3761),
  ('Carrefour Congreso', 'https://www.carrefour.com.ar',              'Av. Rivadavia 1800, CABA',      -34.6118, -58.3816)
) AS v(name, base_url, address, lat, lng)
WHERE NOT EXISTS (SELECT 1 FROM stores s WHERE s.name = v.name);

-- Por si la tienda ya existía sin dirección/coordenadas, las completa
UPDATE stores SET address = 'Av. 9 de Julio 500, CABA', latitude = -34.6019, longitude = -58.3816
WHERE name = 'Coto Microcentro' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. de Mayo 800, CABA', latitude = -34.6037, longitude = -58.3761
WHERE name = 'Dia San Nicolás' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. Rivadavia 1800, CABA', latitude = -34.6118, longitude = -58.3816
WHERE name = 'Carrefour Congreso' AND (address IS NULL OR latitude IS NULL);

-- ── Limpia precios previos de estas tiendas (idempotente) ────────
DELETE FROM prices
WHERE store_id IN (
  SELECT id FROM stores WHERE name IN (
    'Coto Microcentro','Dia San Nicolás','Carrefour Congreso'
  )
);

-- ── Precios: mismo set base de 24 productos, con un multiplicador
--    distinto por tienda ─────────────────────────────────────────
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
    ('Coto Microcentro',   1.02),
    ('Dia San Nicolás',    0.94),
    ('Carrefour Congreso', 1.06)
  ) AS m(name, multiplier) ON m.name = s.name
)
INSERT INTO prices (price, currency, recorded_at, product_id, store_id)
SELECT ROUND((bp.base_price * sm.multiplier)::numeric, 2), 'ARS', NOW(), bp.product_id, sm.store_id
FROM base_prices bp
CROSS JOIN store_mult sm;

-- ── Verificación: distancia aproximada de cada tienda al Obelisco ──
-- SELECT name, address,
--        ROUND((6371 * acos(LEAST(1, GREATEST(-1,
--          cos(radians(-34.6037)) * cos(radians(latitude)) * cos(radians(longitude) - radians(-58.3816))
--          + sin(radians(-34.6037)) * sin(radians(latitude))
--        ))))::numeric, 2) AS distancia_km
-- FROM stores
-- ORDER BY distancia_km;
