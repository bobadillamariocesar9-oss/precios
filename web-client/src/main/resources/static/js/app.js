const { createApp } = Vue;

createApp({
  data() {
    return {
      currentRoute: 'categories',
      alert: { message: '', type: 'info' },

      // Auth
      auth: { username: '', password: '' },
      loginForm: { username: '', password: '' },
      loginLoading: false,
      registerForm: { username: '', email: '', password: '' },
      registerLoading: false,

      // Categorías — definición fija con ícono y color
      fixedCategories: [
        { name: 'Alimento',     icon: 'bi-basket2-fill',    color: 'success',   description: 'Alimentos básicos y comestibles' },
        { name: 'Bebidas',      icon: 'bi-cup-straw',       color: 'info',      description: 'Bebidas frías, calientes y alcohólicas' },
        { name: 'Indumentaria', icon: 'bi-bag-heart-fill',  color: 'danger',    description: 'Ropa, calzado y accesorios' },
        { name: 'Electrónica',  icon: 'bi-cpu-fill',        color: 'primary',   description: 'Tecnología, electrodomésticos y accesorios' },
        { name: 'Hogar',        icon: 'bi-house-door-fill', color: 'warning',   description: 'Muebles, textiles y decoración' },
        { name: 'Limpieza',     icon: 'bi-droplet-fill',    color: 'secondary', description: 'Limpieza del hogar e higiene personal' },
        { name: 'Mascotas',     icon: 'bi-heart-fill',      color: 'dark',      description: 'Alimento y accesorios para mascotas' },
        { name: 'Bazar',        icon: 'bi-cup-hot-fill',    color: 'purple',    description: 'Utensilios y artículos de cocina' },
      ],
      allProductsForCat: [],   // todos los productos para agrupar
      selectedCategory: null,  // categoría activa
      catLoading: false,
      expandedFamilies: {},    // { 'Arroz': true } — qué familias están desplegadas

      // Modal de comparación de precios (se abre desde Categorías)
      selectedProduct: null,
      prices: [],           // normalizado: {id, price, currency, recordedAt, storeName, storeAddress, distanceKm}
      pricesLoading: false,

      // Carro de compra: acumula los ítems tildados en el modal, de
      // cualquier producto, hasta que se presiona "Comprar".
      basketAdvice: null,
      basketExplanation: '',
      basketLoading: false,
      basketAiLoading: false,
      basketError: '',
      cart: [],  // { priceId, productId, productName, brand, storeName, storeAddress, price, currency }
      pricesMode: 'all',    // 'nearby' | 'all' — cómo se obtuvo la lista actual
      radiusKm: 10,
      userLocation: { lat: null, lng: null, status: 'idle' }, // idle | loading | granted | denied | unsupported

      // Usuarios
      users: [],
      usersLoading: false,

      // Favoritos & Alertas
      favorites: [],
      favLoading: false,
      priceAlerts: [],
      alertForm: { productId: '', targetPrice: '' },

      // Admin
      stores: [],
      scrapingJobs: [],
      scrapingLogs: [],
    };
  },

  computed: {
    // Agrupa productos por categoría: { "Electrónica": [...], "Ropa": [...] }
    categories() {
      const map = {};
      for (const p of this.allProductsForCat) {
        const cat = p.category || 'Sin categoría';
        if (!map[cat]) map[cat] = [];
        map[cat].push(p);
      }
      return map;
    },

    // Productos de la categoría seleccionada, ordenados alfabéticamente
    categoryProducts() {
      if (!this.selectedCategory) return [];
      const list = this.categories[this.selectedCategory] || [];
      return [...list].sort((a, b) =>
        (a.name || '').localeCompare(b.name || '', 'es', { sensitivity: 'base' })
      );
    },

    // Productos de la categoría agrupados por familia (el tipo base del
    // producto, ej. "Arroz", "Fideos", "Zapatillas"), cada grupo colapsado
    // por defecto. Familias y productos dentro de cada una, ordenados
    // alfabéticamente.
    familyGroups() {
      const map = {};
      for (const p of this.categoryProducts) {
        const family = this.productFamily(p.name);
        if (!map[family]) map[family] = [];
        map[family].push(p);
      }
      return Object.keys(map)
        .sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }))
        .map(family => ({ family, products: map[family] }));
    },

    // Nombres de categorías ordenadas
    categoryNames() {
      return Object.keys(this.categories).sort();
    },

    // Suma total del carro de compra (todos los productos tildados)
    cartTotal() {
      return this.cart.reduce((sum, item) => sum + Number(item.price || 0), 0);
    },

    // Ítems del carro que pertenecen al producto abierto actualmente en el modal
    productItemsInCart() {
      if (!this.selectedProduct) return [];
      return this.cart.filter(item => item.productId === this.selectedProduct.id);
    },
  },

  mounted() {
    const saved = sessionStorage.getItem('auth');
    if (saved) {
      this.auth = JSON.parse(saved);
      API.setAuth(this.auth.username, this.auth.password);
    }
    this.loadCategories();
  },

  methods: {
    // ── Navegación ────────────────────────────────────────────────────────────

    navigate(route) {
      this.currentRoute = route;
      this.alert.message = '';
      if (route === 'categories') this.loadCategories();
      if (route === 'users')      this.loadUsers();
      if (route === 'favorites')  { this.loadFavorites(); this.loadAlerts(); }
      if (route === 'admin')      { this.loadStores(); this.loadJobs(); this.loadLogs(); }
    },

    showAlert(message, type = 'success') {
      this.alert = { message, type };
      setTimeout(() => this.alert.message = '', 5000);
    },

    formatDate(dt) {
      if (!dt) return '—';
      return new Date(dt).toLocaleDateString('es-AR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    },

    statusColor(s) {
      const map = {
        ACTIVE: 'success', INACTIVE: 'secondary', PENDING: 'warning',
        SUCCESS: 'success', ERROR: 'danger', RUNNING: 'primary',
      };
      return map[s?.toUpperCase()] || 'secondary';
    },

    // Ícono por categoría
    categoryIcon(name) {
      const icons = {
        'electrónica': 'bi-cpu', 'electronica': 'bi-cpu',
        'computación': 'bi-laptop', 'computacion': 'bi-laptop',
        'celulares': 'bi-phone', 'telefonía': 'bi-phone',
        'ropa': 'bi-bag', 'indumentaria': 'bi-bag',
        'calzado': 'bi-bootstrap',
        'hogar': 'bi-house', 'muebles': 'bi-house',
        'deportes': 'bi-bicycle',
        'libros': 'bi-book',
        'juguetes': 'bi-puzzle',
        'alimentos': 'bi-basket',
        'salud': 'bi-heart-pulse',
        'herramientas': 'bi-tools',
        'sin categoría': 'bi-grid',
      };
      const key = name.toLowerCase();
      for (const [k, v] of Object.entries(icons)) {
        if (key.includes(k)) return v;
      }
      return 'bi-tag';
    },

    // Color de card por posición
    categoryColor(index) {
      const colors = ['primary', 'success', 'info', 'warning', 'danger', 'secondary', 'dark'];
      return colors[index % colors.length];
    },

    // ── Categorías ────────────────────────────────────────────────────────────

    async loadCategories() {
      this.catLoading = true;
      this.selectedCategory = null;
      try {
        this.allProductsForCat = await API.getProducts();
      } catch (e) {
        this.showAlert('Error al cargar categorías: ' + e.message, 'danger');
      } finally {
        this.catLoading = false;
      }
    },

    selectCategory(name) {
      this.selectedCategory = this.selectedCategory === name ? null : name;
      this.expandedFamilies = {};
    },

    toggleFamily(family) {
      this.expandedFamilies = { ...this.expandedFamilies, [family]: !this.expandedFamilies[family] };
    },

    // Deriva la "familia" (tipo base) de un producto a partir de su nombre,
    // tomando la primera palabra significativa. Ej: "Arroz Integral 500g"
    // → "Arroz". No requiere ningún campo nuevo en la base de datos.
    productFamily(name) {
      if (!name) return 'Otros';
      const firstWord = name.trim().split(/\s+/)[0];
      return firstWord;
    },

    // ── Productos ─────────────────────────────────────────────────────────────

    async selectProduct(product) {
      this.selectedProduct = product;
      this.prices = [];
      this.pricesLoading = true;
      try {
        await this.loadPricesForSelectedProduct();
      } finally {
        this.pricesLoading = false;
      }
    },

    isInCart(priceId) {
      return this.cart.some(item => item.priceId === priceId);
    },

    // Tilda/destilda una fila de precio: la agrega o quita del carro de
    // compra, junto con los datos del producto y la tienda a la que
    // pertenece (necesarios porque el carro persiste aunque se cierre
    // el modal o se abra otro producto).
    togglePriceSelection(price) {
      if (this.isInCart(price.id)) {
        this.cart = this.cart.filter(item => item.priceId !== price.id);
        return;
      }
      this.cart = [...this.cart, {
        priceId: price.id,
        productId: this.selectedProduct.id,
        productName: this.selectedProduct.name,
        brand: this.selectedProduct.brand,
        storeName: price.storeName,
        storeAddress: price.storeAddress,
        price: price.price,
        currency: price.currency || 'ARS',
      }];
    },

    // Quita del carro todos los ítems del producto abierto actualmente
    clearProductFromCart() {
      if (!this.selectedProduct) return;
      this.cart = this.cart.filter(item => item.productId !== this.selectedProduct.id);
    },

    // Quita un ítem puntual del carro (desde la pantalla Carrito)
    removeFromCart(priceId) {
      this.cart = this.cart.filter(item => item.priceId !== priceId);
    },

    // Recomienda usando datos reales. La IA solo explica los cálculos ya hechos.
    async analyzeBasket() {
      const ids = [...new Set(this.cart.map(item => item.productId))];
      if (ids.length === 0) return;
      if (ids.length > 10) {
        this.basketError = 'Para esta primera versión, elegí hasta 10 productos distintos.';
        return;
      }
      this.basketError = '';
      this.basketAdvice = null;
      this.basketExplanation = '';
      this.basketLoading = true;
      try {
        this.basketAdvice = await API.compareBasket(ids);
      } catch (e) {
        this.basketError = 'Iniciá sesión para analizar tu carrito. ' + e.message;
      } finally {
        this.basketLoading = false;
      }
    },

    async explainBasketWithAi() {
      if (!this.basketAdvice || this.basketAiLoading) return;
      const ids = [...new Set(this.cart.map(item => item.productId))];
      this.basketAiLoading = true;
      this.basketExplanation = '';
      this.basketError = '';
      try {
        const result = await API.explainBasket(ids);
        this.basketExplanation = result.explanation;
      } catch (e) {
        this.basketError = 'No se pudo generar la explicación de IA. ' + e.message;
      } finally {
        this.basketAiLoading = false;
      }
    },

    // Simula la compra: no hay pasarela de pago real, solo confirma el
    // total y vacía el carro.
    comprar() {
      if (this.cart.length === 0) return;
      const count = this.cart.length;
      const total = this.cartTotal.toLocaleString('es-AR');
      this.cart = [];
      this.showAlert(`¡Compra realizada! ${count} producto${count !== 1 ? 's' : ''} por ARS ${total}.`, 'success');
    },

    // Pide la ubicación al navegador (una sola vez, se reutiliza después)
    requestUserLocation() {
      if (!navigator.geolocation) {
        this.userLocation.status = 'unsupported';
        return Promise.resolve(this.userLocation);
      }

      this.userLocation.status = 'loading';

      return new Promise((resolve) => {
        navigator.geolocation.getCurrentPosition(
            (pos) => {
              this.userLocation = {
                lat: pos.coords.latitude,
                lng: pos.coords.longitude,
                status: 'granted',
              };
              resolve(this.userLocation);
            },
            () => {
              this.userLocation.status = 'denied';
              resolve(this.userLocation);
            },
            {
              enableHighAccuracy: true,
              maximumAge: 0,
              timeout: 10000
            }
        );
      });
    },
    // Carga precios del producto seleccionado: intenta por radio (geolocalización),
    // y si no hay ubicación disponible, cae a la comparación general.
    async loadPricesForSelectedProduct() {
      if (!this.selectedProduct) return;
      await this.requestUserLocation();

      if (this.userLocation.status === 'granted') {
        try {
          const nearby = await API.comparePricesNearby(
            this.selectedProduct.id, this.userLocation.lat, this.userLocation.lng, this.radiusKm
          );
          this.pricesMode = 'nearby';
          this.prices = nearby.map(p => ({
            id: p.id, price: p.price, currency: p.currency, recordedAt: p.recordedAt,
            storeName: p.storeName, storeAddress: p.storeAddress, distanceKm: p.distanceKm,
          }));
          return;
        } catch (e) {
          this.showAlert('No se pudo buscar por ubicación, mostrando todas las tiendas.', 'warning');
        }
      }

      // Sin ubicación (denegada/no soportada/error) → comparación general, sin distancia
      try {
        const all = await API.comparePrices(this.selectedProduct.id);
        this.pricesMode = 'all';
        this.prices = all.map(p => ({
          id: p.id, price: p.price, currency: p.currency, recordedAt: p.recordedAt,
          storeName: p.store?.name, storeAddress: p.store?.address, distanceKm: null,
        }));
      } catch {
        this.prices = [];
      }
    },

    // Se llama al cambiar el radio de búsqueda en el modal
    onRadiusChange() {
      if (this.selectedProduct) this.loadPricesForSelectedProduct();
    },

    // ── Usuarios ──────────────────────────────────────────────────────────────

    async login() {
      this.loginLoading = true;
      try {
        API.setAuth(this.loginForm.username, this.loginForm.password);
        await API.getUsers(); // verificar conectividad
        this.auth = { username: this.loginForm.username, password: this.loginForm.password };
        sessionStorage.setItem('auth', JSON.stringify(this.auth));
        this.showAlert(`Bienvenido, ${this.auth.username}!`);
        this.loginForm = { username: '', password: '' };
      } catch (e) {
        API.setAuth(null);
        this.showAlert('Credenciales incorrectas.', 'danger');
      } finally {
        this.loginLoading = false;
      }
    },

    logout() {
      this.auth = { username: '', password: '' };
      sessionStorage.removeItem('auth');
      API.setAuth(null);
      this.navigate('categories');
      this.showAlert('Sesión cerrada.');
    },

    async register() {
      this.registerLoading = true;
      try {
        await API.createUser(this.registerForm);
        this.showAlert('Cuenta creada. Ya puedes iniciar sesión.');
        this.registerForm = { username: '', email: '', password: '' };
        this.loadUsers();
      } catch (e) {
        this.showAlert(e.message || 'Error al registrar.', 'danger');
      } finally {
        this.registerLoading = false;
      }
    },

    async loadUsers() {
      this.usersLoading = true;
      try {
        this.users = await API.getUsers();
      } catch (e) {
        this.showAlert('Error al cargar usuarios: ' + e.message, 'danger');
      } finally {
        this.usersLoading = false;
      }
    },

    // ── Favoritos ─────────────────────────────────────────────────────────────

    async loadFavorites() {
      this.favLoading = true;
      try { this.favorites = await API.getFavorites(); }
      catch { this.favorites = []; }
      finally { this.favLoading = false; }
    },

    async deleteFavorite(id) {
      try {
        await API.deleteFavorite(id);
        this.favorites = this.favorites.filter(f => f.id !== id);
        this.showAlert('Favorito eliminado.');
      } catch (e) {
        this.showAlert('Error: ' + e.message, 'danger');
      }
    },

    // ── Alertas ───────────────────────────────────────────────────────────────

    async loadAlerts() {
      try { this.priceAlerts = await API.getAlerts(); }
      catch { this.priceAlerts = []; }
    },

    async createAlert() {
      try {
        const users = await API.getUsers();
        const user = users.find(x => x.username === this.auth.username);
        await API.createAlert({
          userId: user?.id,
          productId: Number(this.alertForm.productId),
          targetPrice: Number(this.alertForm.targetPrice),
        });
        this.alertForm = { productId: '', targetPrice: '' };
        this.showAlert('Alerta creada.');
        this.loadAlerts();
      } catch (e) {
        this.showAlert('Error: ' + e.message, 'danger');
      }
    },

    async deleteAlert(id) {
      try {
        await API.deleteAlert(id);
        this.priceAlerts = this.priceAlerts.filter(a => a.id !== id);
        this.showAlert('Alerta eliminada.');
      } catch (e) {
        this.showAlert('Error: ' + e.message, 'danger');
      }
    },

    // ── Admin ─────────────────────────────────────────────────────────────────

    async loadStores()  { try { this.stores       = await API.getStores();       } catch { this.stores       = []; } },
    async loadJobs()    { try { this.scrapingJobs  = await API.getScrapingJobs(); } catch { this.scrapingJobs  = []; } },
    async loadLogs()    { try { this.scrapingLogs  = await API.getScrapingLogs(); } catch { this.scrapingLogs  = []; } },
  }
}).mount('#app');
