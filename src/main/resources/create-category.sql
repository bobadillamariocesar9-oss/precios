-- Crear tabla category
CREATE TABLE IF NOT EXISTS category (
    id           SERIAL PRIMARY KEY,
    nombre_categ VARCHAR(100) NOT NULL,
    categ        VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);

-- Datos iniciales
INSERT INTO category (nombre_categ, categ) VALUES
  ('Alimento',     'ALIMENTO'),
  ('Bebidas',      'BEBIDAS'),
  ('Indumentaria', 'INDUMENTARIA')
ON CONFLICT DO NOTHING;
