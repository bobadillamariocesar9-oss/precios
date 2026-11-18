/**
 * Cliente HTTP — llama al proxy Spring Boot en /api/**
 * que internamente reenvía al backend en el puerto 8081.
 */
const API = {
  _auth: null,

  setAuth(username, password) {
    this._auth = username ? btoa(`${username}:${password}`) : null;
  },

  _headers(extra = {}) {
    const h = { 'Content-Type': 'application/json', ...extra };
    if (this._auth) h['Authorization'] = `Basic ${this._auth}`;
    return h;
  },

  async _fetch(method, path, body = null) {
    const opts = { method, headers: this._headers() };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(`/api${path}`, opts);
    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || `HTTP ${res.status}`);
    }
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  },

  // ── Productos ──────────────────────────────────────────────────────────────
  getProducts:      ()   => API._fetch('GET', '/products'),
  searchProducts:   (kw) => API._fetch('GET', `/products/search?keyword=${encodeURIComponent(kw)}`),
  getProduct:       (id) => API._fetch('GET', `/products/${id}`),

  // ── Precios ────────────────────────────────────────────────────────────────
  getPricesByProduct: (id) => API._fetch('GET', `/prices/product/${id}`),
  comparePrices:      (id) => API._fetch('GET', `/prices/product/${id}/compare`),
  comparePricesNearby: (id, lat, lng, radiusKm) =>
    API._fetch('GET', `/prices/product/${id}/compare/nearby?lat=${lat}&lng=${lng}&radiusKm=${radiusKm}`),

  // ── Comparador de cesta (API propia Java + IA opcional) ────────────────────
  compareBasket: (productIds) =>
    API._fetch('POST', '/shopping-advice/compare', { productIds }),
  explainBasket: (productIds) =>
    API._fetch('POST', '/shopping-advice/explain', { productIds }),

  // ── Tiendas ────────────────────────────────────────────────────────────────
  getStores: () => API._fetch('GET', '/stores'),

  // ── Usuarios ───────────────────────────────────────────────────────────────
  getUsers:    ()   => API._fetch('GET', '/users'),
  createUser:  (u)  => API._fetch('POST', '/users', u),
  verifyLogin: ()   => API._fetch('GET', '/users'), // GET es público, usamos como ping con auth

  // ── Favoritos ──────────────────────────────────────────────────────────────
  getFavorites:   () => API._fetch('GET', '/favorites'),
  createFavorite: (f) => API._fetch('POST', '/favorites', f),
  deleteFavorite: (id) => API._fetch('DELETE', `/favorites/${id}`),

  // ── Alertas ────────────────────────────────────────────────────────────────
  getAlerts:   () => API._fetch('GET', '/price-alerts'),
  createAlert: (a) => API._fetch('POST', '/price-alerts', a),
  deleteAlert: (id) => API._fetch('DELETE', `/price-alerts/${id}`),

  // ── Scraping ───────────────────────────────────────────────────────────────
  getScrapingJobs: () => API._fetch('GET', '/scraping-jobs'),
  getScrapingLogs: () => API._fetch('GET', '/scraping-logs'),
};
