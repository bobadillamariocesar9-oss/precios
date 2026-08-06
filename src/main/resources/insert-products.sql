-- ═══════════════════════════════════════════════════════════════
-- Inserta 24 productos de ejemplo en Alimento, Bebidas, Indumentaria
-- Seguro para re-ejecutar: usa WHERE NOT EXISTS por nombre
-- ═══════════════════════════════════════════════════════════════

-- ── Alimento ─────────────────────────────────────────────────
INSERT INTO products (name, brand, description, category, created_at, updated_at)
SELECT v.name, v.brand, v.description, v.category, NOW(), NOW()
FROM (VALUES
  ('Arroz Largo Fino 1kg',   'La Unión',      'Arroz blanco largo fino de primera calidad', 'Alimento'),
  ('Fideos Spaghetti 500g',  'Matarazzo',     'Pasta de sémola de trigo',                    'Alimento'),
  ('Aceite de Girasol 1.5L', 'Cocinero',      'Aceite de girasol refinado',                  'Alimento'),
  ('Azúcar Común 1kg',       'Ledesma',       'Azúcar blanca refinada',                      'Alimento'),
  ('Harina 000 1kg',         'Cañuelas',      'Harina de trigo triple cero',                 'Alimento'),
  ('Sal Fina 500g',          'Celusal',       'Sal de mesa fina yodada',                     'Alimento'),
  ('Galletitas Agua 200g',   'Terrabusi',     'Galletitas de agua clásicas',                 'Alimento'),
  ('Atún al Natural 170g',   'La Campagnola', 'Atún en lata al natural',                     'Alimento')
) AS v(name, brand, description, category)
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = v.name);

-- ── Bebidas ──────────────────────────────────────────────────
INSERT INTO products (name, brand, description, category, created_at, updated_at)
SELECT v.name, v.brand, v.description, v.category, NOW(), NOW()
FROM (VALUES
  ('Agua Mineral 2L',    'Villavicencio', 'Agua mineral natural sin gas',           'Bebidas'),
  ('Gaseosa Cola 1.5L',  'Coca-Cola',     'Bebida gaseosa sabor cola',              'Bebidas'),
  ('Jugo de Naranja 1L', 'Citric',        'Jugo de naranja listo para tomar',       'Bebidas'),
  ('Cerveza Lager 1L',   'Quilmes',       'Cerveza rubia lager botella retornable', 'Bebidas'),
  ('Vino Tinto 750ml',   'Trapiche',      'Vino tinto malbec de Mendoza',           'Bebidas'),
  ('Leche Entera 1L',    'La Serenísima', 'Leche entera larga vida',                'Bebidas'),
  ('Infusión Té x20',    'Taragüi',       'Té negro en saquitos',                   'Bebidas'),
  ('Café Molido 250g',   'Nescafé',       'Café molido tostado oscuro',             'Bebidas')
) AS v(name, brand, description, category)
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = v.name);

-- ── Indumentaria ─────────────────────────────────────────────
INSERT INTO products (name, brand, description, category, created_at, updated_at)
SELECT v.name, v.brand, v.description, v.category, NOW(), NOW()
FROM (VALUES
  ('Remera Algodón Hombre', 'Lacoste',  'Remera de algodón 100% manga corta',       'Indumentaria'),
  ('Jean Slim Mujer',       'Levis',    'Pantalón jean corte slim fit',              'Indumentaria'),
  ('Buzo con Capucha',      'Adidas',   'Buzo deportivo con capucha y bolsillos',    'Indumentaria'),
  ('Campera Impermeable',   'Columbia', 'Campera cortaviento impermeable unisex',    'Indumentaria'),
  ('Medias Deportivas x3',  'Nike',     'Pack 3 pares medias deportivas blancas',    'Indumentaria'),
  ('Zapatillas Running',    'Asics',    'Zapatillas para correr amortiguación alta', 'Indumentaria'),
  ('Camiseta Deportiva',    'Puma',     'Camiseta transpirable para entrenamiento',  'Indumentaria'),
  ('Bermuda Cargo',         'Wrangler', 'Bermuda cargo con múltiples bolsillos',     'Indumentaria')
) AS v(name, brand, description, category)
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = v.name);

-- ── Verificación ─────────────────────────────────────────────
-- SELECT category, COUNT(*) FROM products GROUP BY category ORDER BY category;
