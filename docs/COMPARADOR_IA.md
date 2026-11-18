# Comparador de compras con Java + IA (MVP)

Este cambio NO modifica la base de datos, ni el cálculo existente por distancia, ni los endpoints existentes.

## Qué hace
- El carrito permite comparar hasta 10 productos únicos con precios registrados en PostgreSQL.
- El servidor Java calcula dos escenarios: comprar cada producto en la tienda más barata o comprar todos en una sola tienda con precios disponibles para todos.
- `/api/shopping-advice/compare` devuelve números verificables sin llamar a ninguna IA.
- `/api/shopping-advice/explain` usa opcionalmente un modelo para redactar una explicación en español a partir de los cálculos del servidor. La IA no calcula precios ni verifica la calidad.

## Antes del despliegue
1. Revisar y aprobar este PR; no cambia `main` hasta fusionarlo.
2. En Render, configurar `OPENAI_API_KEY` **solo** en el backend `precios-2` si se quiere activar «Explicar con IA». Es un servicio externo con costo por uso. El carrito comparador funciona SIN clave.
3. Opcional: `OPENAI_MODEL` (por defecto `gpt-4o-mini`). Consultar disponibilidad y precios actuales del proveedor.
4. Desplegar primero backend, luego frontend `precios-web` desde `main` después de fusionar. Mantener `API_BACKEND_URL` apuntando al backend en Render.
5. Iniciar sesión antes de analizar el carrito. Los dos endpoints son POST y permanecen protegidos por `SecurityConfig` existente.

## Verificar
Ejecutar `mvn test -Dtest=ShoppingAdviceServiceTest` en el backend (Java 21). Pruebas de integración en Render pendientes.

- Elegir dos productos y tiendas diferentes, añadirlos al carrito.
- Comparar y revisar los totales; repetir después de cambiar el carrito.
- Sin clave de IA el botón de explicación mostrará un error controlado.
- Con clave, la explicación jamás reemplaza los números calculados en Java.
- Los precios de ejemplo cargados por SQL pueden ser estimaciones derivadas de multiplicadores y NO precios de mercado reales.
- Comparar solo precios no equivale a comparar calidad, disponibilidad o gasto en viajes. Una siguiente versión podría usar un radio geográfico y preferencias elegidas por el usuario.
- Controlar el uso de IA y establecer cuotas/autorización antes de habilitar el servicio públicamente.

## Nota de seguridad
Se detectaron credenciales de PostgreSQL escritas históricamente en el repositorio. Rotar la contraseña en Render, actualizar `DB_PASSWORD` y purgarla del código y el historial de Git antes de exponer un entorno productivo.
