const $ = id => document.getElementById(id);
const zone = new URLSearchParams({lat: '-34.628', lng: '-58.463', radiusKm: '2'});
// Commercial labels verified against comercio.csv in the SEPA sample.
const merchants = new Map(Object.entries({
  '9/2': 'Disco', '15/1': 'DIA', '24/2': 'Farmacity', '10/4': 'Carrefour Maxi',
  '10/3': 'Carrefour Express', '10/1': 'Hipermercado Carrefour', '10/2': 'Carrefour Market',
  '12/1': 'Coto', '23/1': 'Axion Energy'
}));
const ignored = new Set(['busco', 'donde', 'compro', 'comprar', 'conseguir', 'consigo', 'cerca',
  'flores', 'caba', 'de', 'del', 'el', 'la', 'los', 'las', 'en', 'me', 'mostrame', 'precio', 'precios', 'por']);
const number = new Intl.NumberFormat('es-AR', {maximumFractionDigits: 2});
const currency = new Intl.NumberFormat('es-AR', {style: 'currency', currency: 'ARS', minimumFractionDigits: 0, maximumFractionDigits: 2});
let catalogRequest;
let requestId = 0;
let offers = [];
let expanded = false;

function words(text) {
  return (text || '').toLowerCase().normalize('NFD').replace(/\p{M}/gu, '')
    .replace(/(\d)([a-z])/g, '$1 $2').match(/[a-z0-9]+/g) || [];
}
function matchProduct(product, tokens) {
  const fields = new Set(words([product.name, product.brand, product.ean, product.description].join(' ')));
  return tokens.every(token => fields.has(token));
}
async function getJson(url) {
  const response = await fetch(url, {headers: {Accept: 'application/json'}});
  if (!response.ok) throw new Error('HTTP ' + response.status);
  const data = await response.json();
  if (!Array.isArray(data)) throw new Error('Respuesta inesperada');
  return data;
}
function catalog() {
  catalogRequest ||= getJson('/api/products').then(rows => rows.filter(p =>
    Number.isSafeInteger(p.id) && p.id > 0 && p.name && p.ean
  )).catch(error => {
    catalogRequest = undefined;
    throw error;
  });
  return catalogRequest;
}
function node(tag, text, className) {
  const element = document.createElement(tag);
  element.textContent = text;
  if (className) element.className = className;
  return element;
}
function merchant(name = '') {
  const parsed = /^SEPA\s+(\d+\/\d+)\/[^|]+\|/i.exec(name);
  if (parsed) return merchants.get(parsed[1]) || 'Comercio no identificado';
  return name && !/^SEPA\b/i.test(name) ? name : 'Comercio no identificado';
}
function distance(km) {
  return km < 1 ? number.format(Math.round(km * 1000)) + ' m' : number.format(km) + ' km';
}
function dateLabel(value) {
  const date = /^(\d{4})-(\d{2})-(\d{2})/.exec(value || '');
  return date ? date[3] + '/' + date[2] + '/' + date[1] : 'fecha no informada';
}
function renderOffers() {
  const byDistance = $('sort').value === 'distance';
  const sorted = [...offers].sort(byDistance
    ? (a, b) => a.distanceKm - b.distanceKm || a.price - b.price
    : (a, b) => a.price - b.price || a.distanceKm - b.distanceKm);
  const visible = expanded ? sorted : sorted.slice(0, 3);
  $('offers').replaceChildren(...visible.map((row, index) => {
    const card = node('li', '', 'offer');
    if (index === 0) card.append(node('span', byDistance ? 'Sucursal más cercana' : 'Menor precio encontrado', 'badge'));
    card.append(
      node('p', currency.format(row.price), 'price'),
      node('h3', merchant(row.storeName)),
      node('p', row.storeAddress || 'Dirección no informada'),
      node('p', 'A ' + distance(row.distanceKm) + ' del punto de búsqueda', 'meta'),
      node('p', 'Fecha del precio: ' + dateLabel(row.recordedAt), 'meta')
    );
    return card;
  }));
  $('show-all').hidden = sorted.length <= 3;
  $('show-all').textContent = expanded ? 'Mostrar solo tres opciones' : 'Ver todas las opciones (' + sorted.length + ')';
  $('show-all').setAttribute('aria-expanded', String(expanded));
  $('status').textContent = sorted.length + (sorted.length === 1 ? ' precio registrado.' : ' precios registrados.');
}
async function compare(product) {
  const current = ++requestId;
  $('choices').hidden = true;
  $('result').hidden = true;
  $('status').textContent = 'Buscando precios de ' + product.name + '…';
  try {
    const data = await getJson('/api/prices/product/' + product.id + '/compare/nearby?' + zone);
    if (current !== requestId) return;
    offers = data.filter(row => row.currency === 'ARS' && Number.isFinite(row.price) && row.price > 0
      && Number.isFinite(row.distanceKm) && row.distanceKm >= 0);
    if (!offers.length) {
      $('status').textContent = 'No hay precios registrados de ' + product.name + ' dentro de 2 km de Flores.';
      return;
    }
    expanded = false;
    $('sort').value = 'price';
    $('product-name').textContent = product.name;
    renderOffers();
    $('result').hidden = false;
    $('product-name').focus({preventScroll: false});
  } catch {
    if (current === requestId) $('status').textContent = 'No pudimos consultar los precios. Volvé a intentar.';
  }
}
async function search(query) {
  const current = ++requestId;
  $('choices').hidden = true;
  $('result').hidden = true;
  const tokens = words(query).filter(word => !ignored.has(word));
  if (!tokens.length) {
    $('status').textContent = 'Escribí un producto, una marca o un código de barras.';
    return;
  }
  $('status').textContent = 'Buscando en el catálogo…';
  try {
    const products = (await catalog()).filter(product => matchProduct(product, tokens));
    if (current !== requestId) return;
    if (!products.length) {
      $('status').textContent = 'No encontramos ese producto en el catálogo. Probá con un nombre más corto o con el código de barras.';
      return;
    }
    if (products.length === 1) {
      await compare(products[0]);
      return;
    }
    $('products').replaceChildren(...products.map(product => {
      const item = node('li', '');
      const button = node('button', product.name);
      button.type = 'button';
      button.addEventListener('click', () => compare(product));
      item.append(button);
      return item;
    }));
    $('status').textContent = 'Encontramos ' + products.length + ' productos.';
    $('choices').hidden = false;
    $('products').querySelector('button').focus();
  } catch {
    if (current === requestId) $('status').textContent = 'No pudimos consultar el catálogo. Volvé a intentar.';
  }
}
$('product-name').tabIndex = -1;
$('search').addEventListener('submit', event => {
  event.preventDefault();
  search($('query').value);
});
document.querySelectorAll('[data-query]').forEach(button => {
  button.addEventListener('click', () => {
    $('query').value = button.dataset.query;
    search(button.dataset.query);
  });
});
$('sort').addEventListener('change', renderOffers);
$('show-all').addEventListener('click', () => {
  expanded = !expanded;
  renderOffers();
});
