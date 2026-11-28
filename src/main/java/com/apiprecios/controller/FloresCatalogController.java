package com.apiprecios.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/flores/products")
public class FloresCatalogController {
    private static final Set<String> STOP_WORDS = Set.of(
            "de", "del", "el", "la", "los", "las", "en", "flores", "caba", "y");
    private static final String IN_FLORES = """
            s.latitude IS NOT NULL AND s.longitude IS NOT NULL
            AND CAST(:boundary AS polygon) @> point(s.longitude, s.latitude)
            """;
    private static final String SOURCE = "o.source_name = 'SEPA' AND o.price_scope = 'BRANCH_LIST'";

    private final JdbcClient jdbc;
    private final String boundary;

    public FloresCatalogController(JdbcClient jdbc) throws IOException {
        this.jdbc = jdbc;
        this.boundary = new ClassPathResource("geo/flores.polygon")
                .getContentAsString(StandardCharsets.UTF_8).trim();
    }

    @GetMapping
    public List<ProductMatch> search(@RequestParam String keyword) {
        if (keyword.isBlank() || keyword.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribí un producto o código de barras");
        }
        String normalized = Normalizer.normalize(keyword.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("(\\d)([a-z])", "$1 $2");
        List<String> terms = Arrays.stream(normalized.split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank() && !STOP_WORDS.contains(token))
                .distinct().limit(9).toList();
        if (terms.isEmpty() || terms.size() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buscá con hasta ocho palabras");
        }
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.name, p.brand, p.ean, p.category
                FROM products p
                WHERE
                """);
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) sql.append(" AND ");
            sql.append("""
                    strpos(regexp_replace(translate(lower(concat_ws(' ', p.name, p.brand, p.ean, p.description)),
                      'áéíóúüñ', 'aeiouun'), '([0-9])([a-z])', '\\1 \\2', 'g'),
                    :term""").append(i).append(") > 0");
        }
        sql.append(" AND EXISTS (SELECT 1 FROM prices o JOIN stores s ON s.id = o.store_id")
                .append(" WHERE o.product_id = p.id AND ").append(SOURCE).append(" AND ")
                .append(IN_FLORES).append(") ORDER BY (p.ean = :exact) DESC NULLS LAST, p.name, p.id LIMIT 12");
        var query = jdbc.sql(sql.toString()).param("boundary", boundary).param("exact", keyword.trim());
        for (int i = 0; i < terms.size(); i++) query.param("term" + i, terms.get(i));
        return query.query((rs, n) -> new ProductMatch(rs.getInt("id"), rs.getString("name"),
                rs.getString("brand"), rs.getString("ean"), rs.getString("category"))).list();
    }

    @GetMapping("/{productId}/prices")
    public List<Offer> prices(@PathVariable int productId) {
        if (productId < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID no válido");
        String sql = """
                SELECT * FROM (
                  SELECT DISTINCT ON (s.id) o.price, o.currency, o.source_date,
                         s.name AS store_name, s.address AS store_address,
                         6371 * acos(least(1.0, greatest(-1.0,
                           sin(radians(-34.628)) * sin(radians(s.latitude))
                           + cos(radians(-34.628)) * cos(radians(s.latitude))
                             * cos(radians(s.longitude + 58.463))
                         ))) AS distance_km
                  FROM prices o JOIN stores s ON s.id = o.store_id
                  WHERE o.product_id = :productId AND
                """ + SOURCE + " AND " + IN_FLORES + """
                  ORDER BY s.id, o.source_date DESC NULLS LAST, o.recorded_at DESC NULLS LAST, o.id DESC
                ) latest
                ORDER BY price, distance_km, store_name
                LIMIT 12
                """;
        return jdbc.sql(sql).param("productId", productId).param("boundary", boundary)
                .query((rs, n) -> new Offer(rs.getBigDecimal("price"), rs.getString("currency"),
                        rs.getString("store_name"), rs.getString("store_address"),
                        rs.getDouble("distance_km"), rs.getObject("source_date", LocalDate.class))).list();
    }

    public record ProductMatch(int id, String name, String brand, String ean, String category) {}
    public record Offer(BigDecimal price, String currency, String storeName, String storeAddress,
                        double distanceKm, LocalDate sourceDate) {}
}
