package com.apiprecios.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SepaOfficialArchiveTest {
    @TempDir Path directory;

    @Test
    void joinsByAllBranchIdsAndIgnoresInternalCodesAndOtherProvinces() throws Exception {
        Path archive = directory.resolve("sepa.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            entry(zip, "comercio.csv", """
                    id_comercio|id_bandera|comercio_ultima_actualizacion
                    1|2|2026-01-10T10:00:00-03:00
                    """);
            entry(zip, "sucursales.csv", """
                    id_comercio|id_bandera|id_sucursal|sucursales_provincia|sucursales_tipo|sucursales_calle|sucursales_numero|sucursales_localidad|sucursales_nombre|sucursales_latitud|sucursales_longitud
                    1|2|03|AR-C|Supermercado|Rivadavia|7000|CABA|"Flores | Centro"|-34.63|-58.46
                    1|2|04|AR-B|Supermercado|Belgrano|100|Morón|Centro|-34.65|-58.62
                    """);
            entry(zip, "productos.csv", """
                    id_comercio|id_bandera|id_sucursal|id_producto|productos_ean|productos_precio_lista
                    1|2|03|7790139101169|0|999.99
                    1|2|04|7790139101169|1|1999.99
                    1|2|03|7790139101169|1|2500.50

                    Última actualización: 2026-01-10T10:00:00-03:00
                    """);
        }
        var rows = SepaOfficialArchive.read(archive, "7790139101169", "AR-C");
        assertEquals(1, rows.size());
        var row = rows.getFirst();
        assertEquals("1/2/03", row.branch().key());
        assertEquals("Flores | Centro", row.branch().name());
        assertEquals("Rivadavia 7000, CABA", row.branch().address());
        assertEquals(LocalDate.of(2026, 1, 10), row.date());
        assertEquals(new BigDecimal("2500.50"), row.price());
    }

    private void entry(ZipOutputStream zip, String name, String data) throws Exception {
        zip.putNextEntry(new ZipEntry("cadena/" + name));
        zip.write(data.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
