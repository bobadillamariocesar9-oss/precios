-- ═══════════════════════════════════════════════════════════════
-- Reemplaza direcciones/coordenadas inventadas por direcciones
-- REALES de sucursales existentes de cada cadena (investigadas vía
-- web: sucursales24.com.ar, tiendeo.com.ar, sitios oficiales).
--
-- Algunas tiendas se RENOMBRAN porque la cadena no tiene sucursal
-- real en el barrio que se había usado como nombre:
--   - "Changomas Escobar"   → "Carrefour Escobar"  (no se encontró
--     Changomas confirmado en Belén de Escobar; sí hay Carrefour)
--   - "La Anónima La Plata" → "Carrefour La Plata"  (dirección de
--     La Anónima en La Plata no pudo confirmarse; Carrefour sí)
--   - "Coto Microcentro"    → "Coto Almagro"        (Microcentro es
--     zona de oficinas, sin sucursal Coto real; la más cercana
--     confirmada está en Av. Corrientes, límite Almagro/Chacarita)
--   - "Jumbo Floresta"      → "Coto Floresta"       (no se encontró
--     Jumbo en Floresta; sí hay Coto confirmado en esa zona)
--
-- Las coordenadas son aproximadas (estimadas a partir de la
-- dirección real y la cuadrícula de calles), no geocodificación
-- exacta — suficiente para la búsqueda por radio del demo.
--
-- "Supermercado Demo" y "Coto Digital" (online) quedan sin cambios.
-- ═══════════════════════════════════════════════════════════════

UPDATE stores SET
  address = 'Av. Santa Fe 3368, Palermo, CABA', latitude = -34.5891, longitude = -58.4204
WHERE name = 'Carrefour Palermo';

UPDATE stores SET
  address = 'José María Moreno 400, Caballito, CABA', latitude = -34.6187, longitude = -58.4407
WHERE name = 'Dia Caballito';

UPDATE stores SET
  address = 'Nansen 323, Rosario, Santa Fe', latitude = -32.9425, longitude = -60.6379
WHERE name = 'Jumbo Rosario';

UPDATE stores SET
  address = 'Av. Constitución 4002, Mar del Plata, Buenos Aires', latitude = -37.9847, longitude = -57.5716
WHERE name = 'La Anónima Mar del Plata';

UPDATE stores SET
  address = 'Av. Colón 4880, Córdoba Capital, Córdoba', latitude = -31.3985, longitude = -64.2181
WHERE name = 'Carrefour Córdoba';

UPDATE stores SET
  address = 'Italia 62, Avellaneda, Buenos Aires', latitude = -34.6626, longitude = -58.3648
WHERE name = 'Coto Avellaneda';

UPDATE stores SET
  address = 'Av. Calchaquí 3950, Quilmes Oeste, Buenos Aires', latitude = -34.7454, longitude = -58.2977
WHERE name = 'Jumbo Quilmes';

UPDATE stores SET
  address = 'Av. Pdte. J. D. Perón 1350, San Isidro, Buenos Aires', latitude = -34.4726, longitude = -58.5265
WHERE name = 'Carrefour San Isidro';

UPDATE stores SET
  address = 'Santa María 2539, Tigre, Buenos Aires', latitude = -34.4362, longitude = -58.5661
WHERE name = 'Dia Tigre';

UPDATE stores SET
  address = 'Av. del Libertador 552, Moreno, Buenos Aires', latitude = -34.6534, longitude = -58.7897
WHERE name = 'Vea Moreno';

UPDATE stores SET
  name = 'Carrefour Escobar',
  address = 'Gelvez 530, Belén de Escobar, Buenos Aires', latitude = -34.3487, longitude = -58.7935
WHERE name = 'Changomas Escobar';

UPDATE stores SET
  name = 'Carrefour La Plata',
  address = 'Calle 7 N° 767, La Plata, Buenos Aires', latitude = -34.9182, longitude = -57.9553
WHERE name = 'La Anónima La Plata';

UPDATE stores SET
  name = 'Coto Almagro',
  address = 'Av. Corrientes 5748, CABA', latitude = -34.5966, longitude = -58.4364
WHERE name = 'Coto Microcentro';

UPDATE stores SET
  address = 'Sarmiento 1284, San Nicolás, CABA', latitude = -34.6047, longitude = -58.3809
WHERE name = 'Dia San Nicolás';

UPDATE stores SET
  address = 'Av. Rivadavia 2243, CABA', latitude = -34.6091, longitude = -58.3980
WHERE name = 'Carrefour Congreso';

UPDATE stores SET
  address = 'Av. J.B. Alberdi 1743, CABA', latitude = -34.6376, longitude = -58.4636
WHERE name = 'Coto Flores';

UPDATE stores SET
  address = 'Av. Rivadavia 7370, Flores, CABA', latitude = -34.6357, longitude = -58.4629
WHERE name = 'Dia Flores Sur';

UPDATE stores SET
  address = 'Av. Directorio 2320, Flores, CABA', latitude = -34.6363, longitude = -58.4549
WHERE name = 'Carrefour Flores Este';

UPDATE stores SET
  name = 'Coto Floresta',
  address = 'Av. Avellaneda 3758, CABA', latitude = -34.6324, longitude = -58.4636
WHERE name = 'Jumbo Floresta';

-- ── Verificación ─────────────────────────────────────────────
-- SELECT name, address, latitude, longitude FROM stores ORDER BY name;
