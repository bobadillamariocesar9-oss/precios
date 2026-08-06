-- Agrega coordenadas a las tiendas para poder buscar por radio (km)
ALTER TABLE stores ADD COLUMN IF NOT EXISTS latitude  DOUBLE PRECISION;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Coordenadas de ejemplo (Ciudad de Buenos Aires)
UPDATE stores SET latitude = -34.6037, longitude = -58.3816
WHERE name = 'Supermercado Demo' AND latitude IS NULL;

-- Coto Palermo (sucursal de referencia)
UPDATE stores SET latitude = -34.5883, longitude = -58.4241
WHERE name = 'Coto Digital' AND latitude IS NULL;

-- Verificación
-- SELECT name, address, latitude, longitude FROM stores;
