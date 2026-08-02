-- ═══════════════════════════════════════════════════════════════
-- Precios de referencia — mercado argentino, julio 2026
-- Fuente: promedios de Precios Claros (SEPA), Servidos.ar, Preciosmundi
-- Tienda: 'Supermercado Demo' (ya insertada previamente)
-- Moneda: ARS
-- Seguro para re-ejecutar: borra precios previos de estos productos
-- antes de insertar, para evitar duplicados.
-- ═══════════════════════════════════════════════════════════════

-- Limpia precios anteriores de estos productos en esta tienda (idempotente)
DELETE FROM prices
WHERE store_id = (SELECT id FROM stores WHERE name = 'Supermercado Demo')
  AND product_id IN (
    SELECT id FROM products WHERE name IN (
      'Arroz Largo Fino 1kg','Fideos Spaghetti 500g','Aceite de Girasol 1.5L',
      'Azúcar Común 1kg','Harina 000 1kg','Sal Fina 500g',
      'Galletitas Agua 200g','Atún al Natural 170g',
      'Agua Mineral 2L','Gaseosa Cola 1.5L','Jugo de Naranja 1L',
      'Cerveza Lager 1L','Vino Tinto 750ml','Leche Entera 1L',
      'Infusión Té x20','Café Molido 250g',
      'Remera Algodón Hombre','Jean Slim Mujer','Buzo con Capucha',
      'Campera Impermeable','Medias Deportivas x3','Zapatillas Running',
      'Camiseta Deportiva','Bermuda Cargo'
    )
  );

-- ── Alimento ─────────────────────────────────────────────────
INSERT INTO prices (price, currency, recorded_at, product_id, store_id)
SELECT v.price, 'ARS', NOW(), p.id, s.id
FROM (VALUES
  ('Arroz Largo Fino 1kg',   2200.00),
  ('Fideos Spaghetti 500g',  1200.00),
  ('Aceite de Girasol 1.5L', 3200.00),
  ('Azúcar Común 1kg',       1400.00),
  ('Harina 000 1kg',         1100.00),
  ('Sal Fina 500g',           700.00),
  ('Galletitas Agua 200g',   1300.00),
  ('Atún al Natural 170g',   2800.00)
) AS v(name, price)
JOIN products p ON p.name = v.name
JOIN stores   s ON s.name = 'Supermercado Demo';

-- ── Bebidas ──────────────────────────────────────────────────
INSERT INTO prices (price, currency, recorded_at, product_id, store_id)
SELECT v.price, 'ARS', NOW(), p.id, s.id
FROM (VALUES
  ('Agua Mineral 2L',    1400.00),
  ('Gaseosa Cola 1.5L',  2600.00),
  ('Jugo de Naranja 1L', 1800.00),
  ('Cerveza Lager 1L',   2200.00),
  ('Vino Tinto 750ml',   4500.00),
  ('Leche Entera 1L',    1700.00),
  ('Infusión Té x20',    1600.00),
  ('Café Molido 250g',   4200.00)
) AS v(name, price)
JOIN products p ON p.name = v.name
JOIN stores   s ON s.name = 'Supermercado Demo';

-- ── Indumentaria ─────────────────────────────────────────────
INSERT INTO prices (price, currency, recorded_at, product_id, store_id)
SELECT v.price, 'ARS', NOW(), p.id, s.id
FROM (VALUES
  ('Remera Algodón Hombre', 16000.00),
  ('Jean Slim Mujer',       35000.00),
  ('Buzo con Capucha',      20000.00),
  ('Campera Impermeable',   48000.00),
  ('Medias Deportivas x3',   9000.00),
  ('Zapatillas Running',    85000.00),
  ('Camiseta Deportiva',    28000.00),
  ('Bermuda Cargo',         22000.00)
) AS v(name, price)
JOIN products p ON p.name = v.name
JOIN stores   s ON s.name = 'Supermercado Demo';

-- ── Verificación ─────────────────────────────────────────────
-- SELECT pr.name, price.price, price.currency, price.recorded_at
-- FROM prices price JOIN products pr ON pr.id = price.product_id
-- ORDER BY pr.category, pr.name;
