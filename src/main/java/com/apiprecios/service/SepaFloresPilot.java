package com.apiprecios.service;

import com.apiprecios.entity.Product;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

/** Loads the explicitly enabled, size-limited Flores sample into the development database. */
@Component
@ConditionalOnProperty(name = "agent.sepa-pilot-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SepaFloresPilot implements ApplicationRunner {
    private static final Path DIRECTORY = Path.of("/app/sepa-pilot");
    private static final int PRICE_LIMIT = 72;

    private final EntityManager em;
    private final ObjectMapper json;
    private final SepaPersistenceService persistence;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        if (!"precios_dev".equals(em.createNativeQuery("SELECT current_database()").getSingleResult())) {
            throw new IllegalStateException("The Flores pilot is restricted to precios_dev");
        }
        List<PilotProduct> catalog = json.readValue(DIRECTORY.resolve("catalog.json").toFile(),
                new TypeReference<List<PilotProduct>>() {});
        if (catalog.isEmpty() || catalog.size() > 3) {
            throw new IllegalArgumentException("Expected one to three pilot products");
        }
        em.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(4839601702145763131)")
                .getSingleResult();

        int total = 0;
        for (PilotProduct item : catalog) {
            if (!SepaAmbaPriceService.isValidEan13(item.ean())) {
                throw new IllegalArgumentException("Invalid pilot EAN");
            }
            var observations = SepaOfficialArchive.read(DIRECTORY.resolve("flores.zip"), item.ean(), "AR-C");
            total += observations.size();
            if (observations.size() != item.count() || total > PRICE_LIMIT
                    || observations.stream().anyMatch(o -> !isAroundFlores(o))) {
                throw new IllegalArgumentException("Unexpected pilot observation count or location");
            }
            var matches = em.createQuery("FROM Product p WHERE p.ean = :ean", Product.class)
                    .setParameter("ean", item.ean()).getResultList();
            if (matches.size() > 1) {
                throw new IllegalStateException("Multiple products with pilot EAN " + item.ean());
            }
            Product product;
            if (matches.isEmpty()) {
                product = Product.builder().name(item.name()).brand(item.brand())
                        .category(item.category()).ean(item.ean()).build();
                em.persist(product);
            } else {
                product = matches.getFirst();
            }
            var result = persistence.persistOfficial(product.getId(), item.ean(), observations);
            log.info("Flores pilot: productId={}, EAN={}, new={}, unchanged={}, corrected={}",
                    product.getId(), item.ean(), result.saved(), result.unchanged(), result.corrected());
        }
    }

    private static boolean isAroundFlores(SepaOfficialArchive.Observation observation) {
        var branch = observation.branch();
        return branch.latitude() != null && branch.longitude() != null
                && Math.hypot((branch.latitude() + 34.628) * 111.2,
                              (branch.longitude() + 58.463) * 91.5) <= 2.0;
    }

    record PilotProduct(String ean, String name, String brand, String category, int count) {}
}
