-- ============================================================
-- Datos de ejemplo — ejecutar una sola vez en psql o DBeaver
-- ============================================================

-- Tienda de ejemplo
INSERT INTO stores (name, base_url, created_at, updated_at)
VALUES ('Supermercado Demo', 'https://demo.com', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ── Alimentos ────────────────────────────────────────────────
INSERT INTO products (name, brand, description, category, created_at, updated_at) VALUES
  ('Arroz Largo Fino 1kg',   'La Unión',   'Arroz blanco largo fino de primera calidad', 'Alimento', NOW(), NOW()),
  ('Fideos Spaghetti 500g',  'Matarazzo',  'Pasta de sémola de trigo',                   'Alimento', NOW(), NOW()),
  ('Aceite de Girasol 1.5L', 'Cocinero',   'Aceite de girasol refinado',                 'Alimento', NOW(), NOW()),
  ('Azúcar Común 1kg',       'Ledesma',    'Azúcar blanca refinada',                     'Alimento', NOW(), NOW()),
  ('Harina 000 1kg',         'Cañuelas',   'Harina de trigo triple cero',                'Alimento', NOW(), NOW()),
  ('Sal Fina 500g',          'Celusal',    'Sal de mesa fina yodada',                    'Alimento', NOW(), NOW()),
  ('Galletitas Agua 200g',   'Terrabusi',  'Galletitas de agua clásicas',                'Alimento', NOW(), NOW()),
  ('Atún al Natural 170g',   'La Campagnola', 'Atún en lata al natural',                 'Alimento', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ── Bebidas ──────────────────────────────────────────────────
INSERT INTO products (name, brand, description, category, created_at, updated_at) VALUES
  ('Agua Mineral 2L',        'Villavicencio', 'Agua mineral natural sin gas',            'Bebidas', NOW(), NOW()),
  ('Gaseosa Cola 1.5L',      'Coca-Cola',     'Bebida gaseosa sabor cola',               'Bebidas', NOW(), NOW()),
  ('Jugo de Naranja 1L',     'Citric',        'Jugo de naranja listo para tomar',        'Bebidas', NOW(), NOW()),
  ('Cerveza Lager 1L',       'Quilmes',       'Cerveza rubia lager botella retornable',  'Bebidas', NOW(), NOW()),
  ('Vino Tinto 750ml',       'Trapiche',      'Vino tinto malbec de Mendoza',            'Bebidas', NOW(), NOW()),
  ('Leche Entera 1L',        'La Serenísima', 'Leche entera larga vida',                 'Bebidas', NOW(), NOW()),
  ('Infusión Té x20',        'Taragüi',       'Té negro en saquitos',                    'Bebidas', NOW(), NOW()),
  ('Café Molido 250g',       'Nescafé',       'Café molido tostado oscuro',              'Bebidas', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ── Indumentaria ─────────────────────────────────────────────
INSERT INTO products (name, brand, description, category, created_at, updated_at) VALUES
  ('Remera Algodón Hombre',  'Lacoste',   'Remera de algodón 100% manga corta',       'Indumentaria', NOW(), NOW()),
  ('Jean Slim Mujer',        'Levis',     'Pantalón jean corte slim fit',              'Indumentaria', NOW(), NOW()),
  ('Buzo con Capucha',       'Adidas',    'Buzo deportivo con capucha y bolsillos',    'Indumentaria', NOW(), NOW()),
  ('Campera Impermeable',    'Columbia',  'Campera cortaviento impermeable unisex',    'Indumentaria', NOW(), NOW()),
  ('Medias Deportivas x3',   'Nike',      'Pack 3 pares medias deportivas blancas',    'Indumentaria', NOW(), NOW()),
  ('Zapatillas Running',     'Asics',     'Zapatillas para correr amortiguación alta', 'Indumentaria', NOW(), NOW()),
  ('Camiseta Deportiva',     'Puma',      'Camiseta transpirable para entrenamiento',  'Indumentaria', NOW(), NOW()),
  ('Bermuda Cargo',          'Wrangler',  'Bermuda cargo con múltiples bolsillos',     'Indumentaria', NOW(), NOW())
ON CONFLICT DO NOTHING;


