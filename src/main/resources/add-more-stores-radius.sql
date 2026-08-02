-- ═══════════════════════════════════════════════════════════════
-- Agrega 7 tiendas más en localidades del Gran Buenos Aires,
-- escalonadas en distancia desde el Obelisco (referencia de
-- ubicación del usuario en las pruebas) para que el slider de
-- radio (1-50 km) tenga variedad real de resultados en cada tramo.
--
-- Distancia aproximada desde el Obelisco (-34.6037,-58.3816):
--   Coto Avellaneda        ~6  km
--   Jumbo Quilmes          ~18 km
--   Carrefour San Isidro   ~19 km
--   Dia Tigre              ~27 km
--   Vea Moreno             ~38 km
--   Changomas Escobar      ~47 km
--   La Anónima La Plata    ~53 km  (justo afuera del máximo del slider)
--
-- Sumadas a las tiendas previas (Supermercado Demo y Coto Digital
-- ~5km, Carrefour Palermo/Dia Caballito ~6km, y las lejanas de
-- Rosario/Mar del Plata/Córdoba a cientos de km), el radio de
-- 1 a 50 km ahora muestra resultados distintos en cada escalón.
--
-- Idempotente: usa WHERE NOT EXISTS para las tiendas y borra los
-- precios previos de estas tiendas antes de insertar de nuevo.
-- ═══════════════════════════════════════════════════════════════

-- ── Tiendas nuevas ──────────────────────────────────────────────
INSERT INTO stores (name, base_url, address, latitude, longitude, created_at, updated_at)
SELECT v.name, v.base_url, v.address, v.lat, v.lng, NOW(), NOW()
FROM (VALUES
  ('Coto Avellaneda',      'https://www.cotodigital.com.ar', 'Av. Mitre 750, Avellaneda, Buenos Aires',        -34.6600, -58.3648),
  ('Jumbo Quilmes',        'https://www.jumbo.com.ar',       'Av. Rivadavia 400, Quilmes, Buenos Aires',        -34.7206, -58.2519),
  ('Carrefour San Isidro', 'https://www.carrefour.com.ar',   'Av. Centenario 300, San Isidro, Buenos Aires',    -34.4708, -58.5062),
  ('Dia Tigre',            'https://diaonline.supermercadosdia.com.ar', 'Av. Cazón 1450, Tigre, Buenos Aires', -34.4264, -58.5800),
  ('Vea Moreno',           'https://www.vea.com.ar',         'Av. San Martín 800, Moreno, Buenos Aires',        -34.6534, -58.7897),
  ('Changomas Escobar',    'https://www.changomas.com.ar',   'Ruta 25 Km 4, Escobar, Buenos Aires',             -34.3496, -58.7928),
  ('La Anónima La Plata',  'https://www.laanonimaonline.com', 'Calle 7 N° 850, La Plata, Buenos Aires',         -34.9215, -57.9545)
) AS v(name, base_url, address, lat, lng)
WHERE NOT EXISTS (SELECT 1 FROM stores s WHERE s.name = v.name);

-- Por si la tienda ya existía sin dirección/coordenadas, las completa
UPDATE stores SET address = 'Av. Mitre 750, Avellaneda, Buenos Aires', latitude = -34.6600, longitude = -58.3648
WHERE name = 'Coto Avellaneda' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. Rivadavia 400, Quilmes, Buenos Aires', latitude = -34.7206, longitude = -58.2519
WHERE name = 'Jumbo Quilmes' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. Centenario 300, San Isidro, Buenos Aires', latitude = -34.4708, longitude = -58.5062
WHERE name = 'Carrefour San Isidro' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. Cazón 1450, Tigre, Buenos Aires', latitude = -34.4264, longitude = -58.5800
WHERE name = 'Dia Tigre' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Av. San Martín 800, Moreno, Buenos Aires', latitude = -34.6534, longitude = -58.7897
WHERE name = 'Vea Moreno' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Ruta 25 Km 4, Escobar, Buenos Aires', latitude = -34.3496, longitude = -58.7928
WHERE name = 'Changomas Escobar' AND (address IS NULL OR latitude IS NULL);

UPDATE stores SET address = 'Calle 7 N° 850, La Plata, Buenos Aires', latitude = -34.9215, longitude = -57.9545
WHERE name = 'La Anónima La Plata' AND (address IS NULL OR latitude IS NULL);

-- ── Limpia precios previos de estas tiendas (idempotente) ────────
DELETE FROM prices
WHERE store_id IN (
  SELECT id FROM stores WHERE name IN (
    'Coto Avellaneda','Jumbo Quilmes','Carrefour San Isidro','Dia Tigre',
    'Vea Moreno','Changomas Escobar','La Anónima La Plata'
  )
);

-- ── Precios: mismo set base de 24 productos, con un multiplicador
--    distinto por tienda para que los precios varíen de forma
--    realista según la cadena ────────────────────────────────────
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
    ('Coto Avellaneda',      0.97),
    ('Jumbo Quilmes',        1.03),
    ('Carrefour San Isidro', 1.08),
    ('Dia Tigre',            0.93),
    ('Vea Moreno',           0.89),
    ('Changomas Escobar',    0.91),
    ('La Anónima La Plata',  1.10)
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
--        ))))::numeric, 1) AS distancia_km
-- FROM stores
-- ORDER BY distancia_km;
