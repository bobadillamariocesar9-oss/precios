# Botpress -> Precios -> Aiven: importador regional SEPA (desarrollo)

Esta integración está **sin activar por defecto**. Los cambios viven en la rama de
desarrollo, no en main. Nunca dar por confirmada una importación hasta que
el job esté COMPLETED y se haya verificado la base.

## Qué fuente utiliza el prototipo

**Fuente primaria:** [SEPA Precios Claros, datos abiertos del Gobierno argentino](https://www.datos.gob.ar/dataset/produccion-precios-claros---base-sepa),
licencia CC BY 4.0 (atribución requerida).

**Proveedor técnico de prototipo:** [superprecios-claros](https://github.com/hysmarcos/superprecios-claros),
API comunitaria que transforma la base SEPA en JSON pequeño por EAN y región.
No es una API oficial del Estado; comprobar su disponibilidad, condiciones de uso
y exactitud antes de desplegar. El formato proviene de su README público.

La API devuelve **mediana del precio de lista por cadena en AMBA**. **NO devuelve
el precio de una sucursal de Flores, Caballito ni otro local concreto**.
Se almacena en una tienda lógica `SEPA AMBA | {cadena}`, sin latitud ni
longitud, para no presentarla como una dirección de comercio.

El dato se registra con `sourceName`, `sourceUrl`, `sourceDate`,
`priceScope=AMBA_CHAIN_MEDIAN` y `recordedAt`. La aplicación debe
mostrar ese alcance y atribución. No afirmar que es un precio de góndola.
La app original de comparación por cercanía NO utilizará estos registros
porque esas tiendas lógicas no tienen coordenadas.

## Qué está implementado

- POST `/api/agent/price-refresh` con cabecera `X-Agent-Token`.
- JSON: `{"productId":7,"source":"SEPA_AMBA","dryRun":true}`
- Devuelve HTTP 202 y un `jobId`; GET `/api/agent/price-refresh/{id}` consulta
  PENDING/RUNNING/COMPLETED/FAILED.
- Una tarea y un producto por llamada, cola limitada (un worker).
- `dryRun=true` predeterminado: crea trabajo y log, no descarga datos
  y no escribe precios.
- `dryRun=false`: requiere `AGENT_LIVE_IMPORT_ENABLED=true`.
- Con la función activada: comprueba EAN-13 (incluyendo dígito verificador),
  coincidencia de EAN devuelto, región AMBA, fecha reciente, precios positivos,
  cadena identificada y duplicados por fecha/importe.
- Ante 404, datos antiguos, estructuras incorrectas o ausencia de precios válidos,
  el trabajo se marca FAILED, sin inventar datos.

**Importante:** una importación puede guardar algunas cadenas antes de
encontrar errores posteriores. Todavía no es atómica ni hay etapa de aprobación
humana para datos anómalos. Mantener deshabilitado el modo real hasta validar el
pipeline completo.

## Requisitos previos para una prueba real

1. Backend Spring Boot desplegado por HTTPS, con variables de entorno para Aiven.
2. Rotar las credenciales que aparecieron en el historial del repositorio.
3. Tener un producto existente con `ean` EAN-13 verdadero. No alcanza con
   el nombre ("leche 1L") ni un ID arbitrario.
4. Crear `AGENT_API_TOKEN` seguro y secreto, usado también en Botpress.
5. Probar `dryRun:true`. Después, y sólo con autorización, cambiar
   `AGENT_LIVE_IMPORT_ENABLED=true` y pedir `dryRun:false`.
6. Confirmar que Aiven no tiene datos falsos, la fuente devuelve fecha reciente,
   el costo de conexiones es aceptable y el usuario del bot es administrador.

## Botpress Studio: acción de prueba

En **Configuration Variables** de Botpress definí:

- `PRECIOS_BASE_URL`: URL de tu backend HTTPS, sin barra final.
- `PRECIOS_AGENT_TOKEN`: token idéntico al `AGENT_API_TOKEN` del backend.

Creá variables de Workflow numéricas `productId` y `priceJobId`.
En una tarjeta **Execute Code** (o Action personalizada) usá:

```javascript
const result = await axios.post(
  `${env.PRECIOS_BASE_URL}/api/agent/price-refresh`,
  { productId: Number(workflow.productId), source: 'SEPA_AMBA', dryRun: true },
  {
    headers: { 'X-Agent-Token': env.PRECIOS_AGENT_TOKEN },
    timeout: 15000
  }
)
workflow.priceJobId = result.data.jobId
```

Consultá el estado desde otra Action GET con el mismo token. En Autonomous Node,
declarar la Action como herramienta `refresh_price` y darle instrucciones
para ejecutarla **sólo con autorización de administrador**.
Un bot público no debe poder solicitar escrituras arbitrarias a la base.

Para modo real, el administrador cambia `dryRun` a `false` en la Action
(y configura el flag en el backend). El LLM no debe elegir ese valor por sí solo.

## Ejemplo con curl para probar simulación

```bash
curl -X POST "https://TU_BACKEND/api/agent/price-refresh" \
  -H "Content-Type: application/json" \
  -H "X-Agent-Token: TU_TOKEN_SECRETO" \
  -d '{"productId":7,"source":"SEPA_AMBA","dryRun":true}'
```

## Próximos controles antes de producción

- Test end-to-end contra fuente y PostgreSQL de prueba; todavía no ejecutado.
- Revisión de la licencia de la API intermedia; alternativa definitiva:
  descargar los ZIP oficiales de SEPA por CKAN en un job **separado** y
  procesarlos por streaming (son cientos de MB/día).
- Mantener un registro de mapeos EAN por producto y sucursal si se pretenden
  precios por local, no medianas regionales.
- Distinguir precios de lista de promociones; control de outliers, auditoría,
  rate limits, trazabilidad y protección de los GET públicos existentes.
- Tests de cambios en SQL/JPQL y migraciones controladas en vez de DDL auto-update.
