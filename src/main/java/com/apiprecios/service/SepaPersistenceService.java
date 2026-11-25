package com.apiprecios.service;

import com.apiprecios.entity.Price;
import com.apiprecios.entity.Product;
import com.apiprecios.entity.Store;
import com.apiprecios.repository.PriceRepository;
import com.apiprecios.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Supplier;

/**
 * Persists validated regional observations using the configured Spring datasource
 * (Aiven in production). Does not download data or verify shelf prices.
 */
@Service
@RequiredArgsConstructor
public class SepaPersistenceService {
    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String SOURCE = "SEPA via superprecios-claros";
    private static final String SCOPE = "AMBA_CHAIN_MEDIAN";
    private static final String PROVIDER =
            "https://hysmarcos.github.io/superprecios-claros/v1/sku/";
    private final EntityManager entityManager;
    private final StoreRepository stores;
    private final PriceRepository prices;

    /**
     * One transaction per product: a failed observation rolls back the whole batch.
     * A product row lock serializes concurrent imports of this product.
     * A repeated source/day/chain observation is updated on correction, not appended.
     */
    @Transactional
    public Result persistAmba(Integer productId, String ean, LocalDate sourceDate,
                              String sourceUrl, List<Observation> observations) {
        if (productId == null || !SepaAmbaPriceService.isValidEan13(ean)) {
            throw new IllegalArgumentException("A product ID and valid EAN-13 are required");
        }
        LocalDate today = LocalDate.now(ARGENTINA);
        if (sourceDate == null || sourceDate.isAfter(today)
                || sourceDate.isBefore(today.minusDays(2))) {
            throw new IllegalArgumentException("SEPA date is stale or future-dated");
        }
        if (!(PROVIDER + ean + "/AMBA.json").equals(sourceUrl)) {
            throw new IllegalArgumentException("Unexpected SEPA provider URL");
        }
        if (observations == null || observations.isEmpty()) {
            throw new IllegalArgumentException("At least one observation is required");
        }
        // Validate everything before issuing writes.
        for (Observation observation : observations) {
            if (observation == null || observation.chain() == null
                    || observation.chain().isBlank() || observation.chain().trim().length() > 100
                    || observation.price() == null
                    || observation.price().compareTo(BigDecimal.ONE) < 0
                    || observation.price().compareTo(new BigDecimal("100000000")) > 0
                    || observation.price().scale() > 2) {
                throw new IllegalArgumentException("Invalid SEPA chain or list price");
            }
        }

        var rows = observations.stream().map(observation -> {
            String name = "SEPA AMBA | " + observation.chain().trim();
            return new Row(name, () -> stores.findByName(name).orElseGet(() ->
                    stores.save(Store.builder().name(name)
                            .baseUrl("https://www.datos.gob.ar/dataset/produccion-precios-claros---base-sepa")
                            .address("Mediana AMBA por cadena; no es una sucursal").build())),
                    sourceDate, observation.price());
        }).toList();
        return persist(productId, ean, sourceUrl, SOURCE, SCOPE, rows);
    }

    @Transactional
    public Result persistOfficial(Integer productId, String ean, List<SepaOfficialArchive.Observation> observations) {
        if (observations.isEmpty()) {
            throw new IllegalArgumentException("No official SEPA prices matched the product and province");
        }
        var rows = observations.stream().map(observation -> {
            String key = "SEPA " + observation.branch().key() + " | ";
            return new Row(key, () -> findOrCreateBranch(key, observation.branch()),
                    observation.date(), observation.price());
        }).toList();
        return persist(productId, ean, SepaOfficialArchive.DATASET, "SEPA", "BRANCH_LIST", rows);
    }

    private Store findOrCreateBranch(String key, SepaOfficialArchive.Branch branch) {
        List<Store> matches = entityManager.createQuery(
                        "select s from Store s where s.name like :prefix", Store.class)
                .setParameter("prefix", key + "%").getResultList();
        if (matches.size() > 1) {
            throw new IllegalStateException("Duplicate branch: " + key);
        }
        if (!matches.isEmpty()) {
            return matches.getFirst();
        }
        String name = key + branch.name();
        return stores.save(Store.builder().name(name.substring(0, Math.min(150, name.length())))
                .address(branch.address()).latitude(branch.latitude()).longitude(branch.longitude())
                .baseUrl(SepaOfficialArchive.DATASET).build());
    }

    private Result persist(Integer productId, String ean, String sourceUrl, String source,
                           String scope, List<Row> rows) {
        Product product = entityManager.find(Product.class, productId, LockModeType.PESSIMISTIC_WRITE);
        if (product == null || !ean.equals(product.getEan())) {
            throw new IllegalArgumentException("SEPA EAN does not match the stored product");
        }
        int saved = 0, unchanged = 0, corrected = 0;
        var sorted = rows.stream().sorted(java.util.Comparator.comparing(Row::key)).toList();
        for (Row row : sorted) {
            entityManager.createNativeQuery(
                    "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:key, 0))")
                    .setParameter("key", row.key()).getSingleResult();
            Store store = row.store().get();
            LocalDate sourceDate = row.date();
            List<Price> existing = entityManager.createQuery("""
                    select p from Price p where p.product.id = :productId
                    and p.store.id = :storeId and p.sourceName = :source
                    and p.priceScope = :scope and p.sourceDate = :date
                    """, Price.class)
                    .setParameter("productId", productId).setParameter("storeId", store.getId())
                    .setParameter("source", source).setParameter("scope", scope)
                    .setParameter("date", sourceDate).getResultList();
            if (existing.size() > 1) {
                throw new IllegalStateException("Duplicate SEPA observations require reconciliation");
            }
            if (!existing.isEmpty()) {
                Price old = existing.getFirst();
                if (old.getPrice().compareTo(row.price()) == 0) {
                    unchanged++;
                } else {
                    old.setPrice(row.price());
                    old.setSourceUrl(sourceUrl);
                    // Keep original recordedAt: a correction must not reorder source dates.
                    corrected++;
                }
                continue;
            }
            prices.saveAndFlush(Price.builder().product(product).store(store)
                    .price(row.price()).currency("ARS")
                    // Queries for current prices order by recordedAt; use source day so an
                    // older file imported later cannot displace a newer observation.
                    .recordedAt(sourceDate.atStartOfDay())
                    .sourceName(source).sourceUrl(sourceUrl).sourceDate(sourceDate)
                    .priceScope(scope).build());
            saved++;
        }
        return new Result(saved, unchanged, corrected);
    }

    private record Row(String key, Supplier<Store> store, LocalDate date, BigDecimal price) {}

    public record Observation(String chain, BigDecimal price) {}
    public record Result(int saved, int unchanged, int corrected) {}
}
