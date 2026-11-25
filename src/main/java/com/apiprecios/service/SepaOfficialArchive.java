package com.apiprecios.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Reads the UTF-8 pipe-delimited files defined by the official SEPA specification. */
final class SepaOfficialArchive {
    static final String DATASET = "https://datos.produccion.gob.ar/dataset/sepa-precios";
    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Pattern FIELD = Pattern.compile("\"((?:[^\"\\r\\n]|\"\")*)\"|([^\"|]*)");
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9.-]{1,32}");

    private SepaOfficialArchive() {}

    static List<Observation> read(Path path, String ean, String province) throws IOException {
        Map<String, Observation> results = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(path.toFile(), StandardCharsets.UTF_8)) {
            Map<String, Map<String, ZipEntry>> packages = new TreeMap<>();
            for (ZipEntry entry : zip.stream().filter(e -> !e.isDirectory()).toList()) {
                String name = entry.getName();
                int slash = name.lastIndexOf('/');
                String file = name.substring(slash + 1);
                if (List.of("comercio.csv", "sucursales.csv", "productos.csv").contains(file)) {
                    var group = packages.computeIfAbsent(name.substring(0, slash + 1), k -> new LinkedHashMap<>());
                    if (group.putIfAbsent(file, entry) != null) {
                        throw new IOException("Duplicate SEPA archive entry: " + name);
                    }
                }
            }
            if (packages.isEmpty()) {
                throw new IOException("The ZIP does not contain an official daily SEPA CSV package");
            }
            for (var group : packages.values()) {
                readPackage(zip, group, ean, province, results);
            }
        }
        return List.copyOf(results.values());
    }

    private static void readPackage(ZipFile zip, Map<String, ZipEntry> group, String ean,
                                    String province, Map<String, Observation> results) throws IOException {
        Map<String, LocalDate> dates = new LinkedHashMap<>();
        rows(zip, group.get("comercio.csv"), row -> {
            LocalDate date = OffsetDateTime.parse(row.get("comercio_ultima_actualizacion"))
                    .atZoneSameInstant(ARGENTINA).toLocalDate();
            if (date.isAfter(LocalDate.now(ARGENTINA))) {
                throw new IOException("SEPA package date is in the future");
            }
            dates.put(row.chain(), date);
        });
        Map<String, Branch> branches = new LinkedHashMap<>();
        rows(zip, group.get("sucursales.csv"), row -> {
            if (!province.equalsIgnoreCase(row.get("sucursales_provincia"))
                    || "web".equalsIgnoreCase(row.get("sucursales_tipo"))) {
                return;
            }
            String key = row.branch();
            String address = (row.get("sucursales_calle") + " " + row.get("sucursales_numero")).strip()
                    + ", " + row.get("sucursales_localidad");
            branches.put(key, new Branch(key, row.get("sucursales_nombre"),
                    address.substring(0, Math.min(300, address.length())),
                    coordinate(row.get("sucursales_latitud"), 90),
                    coordinate(row.get("sucursales_longitud"), 180)));
        });
        rows(zip, group.get("productos.csv"), row -> {
            if (!ean.equals(row.get("id_producto")) || !"1".equals(row.get("productos_ean"))) {
                return;
            }
            Branch branch = branches.get(row.branch());
            if (branch == null) {
                return;
            }
            LocalDate date = dates.get(row.chain());
            if (date == null) {
                throw new IOException("Missing commerce date for branch " + branch.key());
            }
            BigDecimal price = new BigDecimal(row.get("productos_precio_lista"));
            if (price.signum() <= 0 || price.stripTrailingZeros().scale() > 2
                    || price.compareTo(new BigDecimal("9999999999.99")) > 0) {
                throw new IOException("Invalid list price for branch " + branch.key());
            }
            var previous = results.putIfAbsent(branch.key() + "/" + date,
                    new Observation(branch, date, price));
            if (previous != null && previous.price().compareTo(price) != 0) {
                throw new IOException("Conflicting prices for branch " + branch.key() + " on " + date);
            }
        });
    }

    private static Double coordinate(String value, int limit) {
        if (value.isEmpty()) {
            return null;
        }
        double coordinate = Double.parseDouble(value);
        return Math.abs(coordinate) <= limit ? coordinate : null;
    }

    private static void rows(ZipFile zip, ZipEntry entry, RowConsumer consumer) throws IOException {
        if (entry == null) {
            throw new IOException("Incomplete SEPA package: expected comercio.csv, sucursales.csv and productos.csv");
        }
        try (var reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
            String first = reader.readLine();
            if (first == null) {
                throw new IOException("Empty CSV: " + entry.getName());
            }
            List<String> header = split(first.replaceFirst("^\\uFEFF", ""));
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) {
                if (columns.put(header.get(i), i) != null) {
                    throw new IOException("Duplicate CSV header in " + entry.getName());
                }
            }
            int lineNumber = 1;
            for (String line; (line = reader.readLine()) != null; ) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("Última actualización:")) {
                    continue;
                }
                try {
                    List<String> values = split(line);
                    if (values.size() != header.size()) {
                        throw new IOException("Unexpected column count");
                    }
                    consumer.accept(new Row(columns, values));
                } catch (IOException | RuntimeException ex) {
                    throw new IOException(entry.getName() + ":" + lineNumber + ": " + ex.getMessage(), ex);
                }
            }
        }
    }

    private static List<String> split(String line) throws IOException {
        var matcher = FIELD.matcher(line);
        List<String> fields = new ArrayList<>();
        int next = 0;
        while (true) {
            matcher.region(next, line.length());
            if (!matcher.lookingAt()) {
                throw new IOException("Malformed CSV field");
            }
            fields.add((matcher.group(1) == null ? matcher.group(2)
                    : matcher.group(1).replace("\"\"", "\"")).strip());
            int end = matcher.end();
            if (end == line.length()) {
                return fields;
            }
            if (line.charAt(end) != '|') {
                throw new IOException("Malformed CSV delimiter");
            }
            next = end + 1;
        }
    }

    private record Row(Map<String, Integer> columns, List<String> values) {
        String get(String field) throws IOException {
            Integer index = columns.get(field);
            if (index == null) {
                throw new IOException("Missing CSV column " + field);
            }
            return values.get(index);
        }

        String chain() throws IOException {
            return id("id_comercio") + "/" + id("id_bandera");
        }

        String branch() throws IOException {
            return chain() + "/" + id("id_sucursal");
        }

        String id(String field) throws IOException {
            String value = get(field);
            if (!ID.matcher(value).matches()) {
                throw new IOException("Invalid SEPA identifier " + field);
            }
            return value;
        }
    }

    @FunctionalInterface
    private interface RowConsumer {
        void accept(Row row) throws IOException;
    }

    record Branch(String key, String name, String address, Double latitude, Double longitude) {}
    record Observation(Branch branch, LocalDate date, BigDecimal price) {}
}
