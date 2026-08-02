-- Agrega la ubicación del proveedor (tienda) a la tabla stores
ALTER TABLE stores ADD COLUMN IF NOT EXISTS address VARCHAR(300);

-- Ejemplo: completar ubicación de las tiendas ya cargadas
UPDATE stores SET address = 'Online — cotodigital.com.ar'
WHERE name = 'Coto Digital' AND address IS NULL;

UPDATE stores SET address = 'Online — demo.com'
WHERE name = 'Supermercado Demo' AND address IS NULL;
