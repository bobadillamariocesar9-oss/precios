package com.apiprecios.service;

import com.apiprecios.entity.Price;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.ScrapingJob;
import com.apiprecios.entity.ScrapingJob.Status;
import com.apiprecios.entity.Store;
import com.apiprecios.repository.PriceRepository;
import com.apiprecios.repository.StoreRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Scraper real de Coto Digital (cotodigital.com.ar).
 *
 * ENFOQUE — sin depender de clases CSS exactas:
 * Coto (y la mayoría de los e-commerce) cambian sus clases CSS seguido, así
 * que en vez de buscar `.product-price` o similar (frágil, se rompe con
 * cualquier rediseño), este scraper:
 *   1. Recorre todos los elementos "hoja" del DOM (sin hijos) de la página
 *      de resultados de búsqueda.
 *   2. Se queda con los que tienen texto con forma de precio argentino
 *      ($X.XXX,XX).
 *   3. Para cada candidato, mira el texto de sus 3 elementos ancestros y
 *      valida que contenga alguna palabra relevante del producto buscado
 *      (para no confundir con precios de envío, productos relacionados, etc.).
 *   4. Se queda con el primer candidato válido en orden de aparición en la
 *      página (los resultados más relevantes de una búsqueda aparecen primero).
 *
 * Esto es más robusto a cambios de HTML que selectores CSS fijos, aunque no
 * es infalible: si Coto cambia el formato de precio o el layout general,
 * puede seguir sin encontrar nada — revisar los logs de scraping_logs si
 * pasa eso.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CotoScraperService {

    private static final String BASE_SEARCH_URL =
            "https://www.cotodigital.com.ar/sitios/cdigi/browse?Ntt=";

    // Precio argentino: "$ 2.200,00", "$2200", "2.200,00", etc.
    // Exige o el símbolo $ al inicio, o una parte decimal ",XX" al final,
    // para no confundir con números sueltos (cantidades, ids, etc.)
    private static final Pattern PRECIO_PATTERN = Pattern.compile(
            "^\\$\\s?([0-9]{1,3}(?:\\.[0-9]{3})*(?:,[0-9]{2})?)$" +
            "|^([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})$");

    // Palabras demasiado comunes como para usarlas de "pista" de relevancia
    private static final Set<String> STOPWORDS = Set.of(
            "de", "la", "el", "los", "las", "con", "sin", "para", "por", "un", "una");

    private final StoreRepository storeRepository;
    private final PriceRepository priceRepository;
    private final ProductService productService;
    private final ScrapingJobService scrapingJobService;
    private final ScrapingLogService scrapingLogService;

    @Transactional
    public ScrapingJob scrapeAllProducts() {
        Store cotoStore = findOrCreateCotoStore();
        List<Product> products = productService.findAll();

        ScrapingJob job = ScrapingJob.builder()
                .store(cotoStore)
                .url(BASE_SEARCH_URL)
                .build();
        job = scrapingJobService.create(job);
        job = scrapingJobService.updateStatus(job.getId(), Status.RUNNING);

        scrapingLogService.info(job.getId(),
                "Iniciando scraping real de Coto Digital para " + products.size() + " productos");

        WebDriver driver = createHeadlessChrome();
        int encontrados = 0;
        int fallidos = 0;

        try {
            for (Product product : products) {
                try {
                    BigDecimal precio = buscarPrecioProducto(driver, product);
                    if (precio != null) {
                        guardarPrecio(product, cotoStore, precio);
                        encontrados++;
                        scrapingLogService.info(job.getId(),
                                "OK: " + product.getName() + " → $" + precio);
                    } else {
                        fallidos++;
                        scrapingLogService.warn(job.getId(),
                                "No se encontró precio para: " + product.getName());
                    }
                } catch (Exception ex) {
                    fallidos++;
                    scrapingLogService.error(job.getId(),
                            "Error procesando '" + product.getName() + "': " + ex.getMessage());
                    log.error("Error scrapeando producto {}", product.getName(), ex);
                }
            }
        } finally {
            driver.quit();
        }

        Status finalStatus = encontrados > 0 ? Status.COMPLETED : Status.FAILED;
        scrapingJobService.updateStatus(job.getId(), finalStatus);
        scrapingLogService.info(job.getId(),
                "Scraping finalizado. Encontrados: " + encontrados + " — Fallidos: " + fallidos);

        return scrapingJobService.findById(job.getId());
    }

    // ── Búsqueda de un producto puntual ───────────────────────────────────

    private BigDecimal buscarPrecioProducto(WebDriver driver, Product product) {
        String keyword = (product.getBrand() != null && !product.getBrand().isBlank())
                ? product.getBrand() + " " + firstWords(product.getName(), 2)
                : firstWords(product.getName(), 2);

        String url = BASE_SEARCH_URL + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        driver.get(url);

        // Esperamos a que la página tenga contenido dinámico cargado: al menos
        // unos cuantos elementos "hoja" con texto, señal de que el JS ya renderizó.
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            wait.until(d -> countLeafElementsWithText(d) > 20);
        } catch (Exception timeout) {
            return null; // la página no terminó de cargar contenido dinámico
        }

        Set<String> pistas = palabrasClave(keyword);
        List<WebElement> hojas = driver.findElements(By.xpath("//*[not(*)]"));

        BigDecimal primerCandidatoSinValidar = null;

        for (WebElement hoja : hojas) {
            String texto;
            try {
                texto = hoja.getText();
            } catch (Exception e) {
                continue; // elemento no interactuable/oculto, lo saltamos
            }
            if (texto == null) continue;
            texto = texto.trim();
            if (texto.isEmpty() || texto.length() > 20) continue;

            BigDecimal precio = parsearPrecioArgentino(texto);
            if (precio == null) continue;
            if (precio.compareTo(BigDecimal.ONE) < 0) continue; // descarta ruido tipo "$0"

            if (primerCandidatoSinValidar == null) {
                primerCandidatoSinValidar = precio;
            }

            if (contextoRelevante(hoja, pistas)) {
                return precio;
            }
        }

        // Nadie pasó la validación de contexto — devolvemos el primer precio
        // encontrado en la página como mejor esfuerzo (los resultados de
        // búsqueda suelen mostrar lo más relevante primero).
        return primerCandidatoSinValidar;
    }

    // ── Validación de contexto ─────────────────────────────────────────────

    /** Mira el texto de los ancestros cercanos del elemento y busca alguna palabra clave. */
    private boolean contextoRelevante(WebElement elemento, Set<String> pistas) {
        if (pistas.isEmpty()) return false;
        try {
            WebElement ancestro = elemento.findElement(By.xpath("./ancestor::*[position()<=4][last()]"));
            String textoAncestro = normalizar(ancestro.getText());
            for (String pista : pistas) {
                if (textoAncestro.contains(pista)) return true;
            }
        } catch (Exception ignorada) {
            // sin ancestro accesible, no podemos validar
        }
        return false;
    }

    private Set<String> palabrasClave(String keyword) {
        return Arrays.stream(keyword.split("\\s+"))
                .map(this::normalizar)
                .filter(w -> w.length() > 3 && !STOPWORDS.contains(w))
                .collect(Collectors.toSet());
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String sinAcentos = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim();
    }

    private long countLeafElementsWithText(WebDriver driver) {
        List<WebElement> hojas = driver.findElements(By.xpath("//*[not(*)]"));
        long conTexto = 0;
        for (WebElement hoja : hojas) {
            try {
                if (!hoja.getText().isBlank()) conTexto++;
            } catch (Exception ignorada) {
                // elemento no accesible, no cuenta
            }
            if (conTexto > 20) break; // no hace falta seguir contando
        }
        return conTexto;
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private WebDriver createHeadlessChrome() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--lang=es-AR");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        return new ChromeDriver(options);
    }

    private BigDecimal parsearPrecioArgentino(String texto) {
        if (texto == null || texto.isBlank()) return null;
        Matcher matcher = PRECIO_PATTERN.matcher(texto);
        if (!matcher.matches()) return null;

        String crudo = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        if (crudo == null) return null;

        // Formato argentino: punto = miles, coma = decimales → normalizar a formato Java
        String normalizado = crudo.replace(".", "").replace(",", ".");
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstWords(String text, int count) {
        String[] parts = text.trim().split("\\s+");
        int limit = Math.min(count, parts.length);
        return String.join(" ", List.of(parts).subList(0, limit));
    }

    private Store findOrCreateCotoStore() {
        return storeRepository.findByName("Coto Digital")
                .orElseGet(() -> storeRepository.save(Store.builder()
                        .name("Coto Digital")
                        .baseUrl("https://www.cotodigital.com.ar")
                        .address("Online — cotodigital.com.ar")
                        .latitude(-34.5883)
                        .longitude(-58.4241)
                        .build()));
    }

    private void guardarPrecio(Product product, Store store, BigDecimal precio) {
        priceRepository.save(Price.builder()
                .product(product)
                .store(store)
                .price(precio)
                .currency("ARS")
                .recordedAt(LocalDateTime.now())
                .build());
    }
}
