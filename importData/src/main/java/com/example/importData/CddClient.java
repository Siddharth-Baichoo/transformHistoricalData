package com.example.importData;

import com.example.importData.dto.BarDto;
import com.example.importData.config.CddProperties;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Downloads CryptoDataDownload CSV and parses into minute-level BarDto list.
 * Handles header comments ('#') and flexible column names.
 */
@Component
public class CddClient {

    private final CddProperties props;
    private final HttpClient http = HttpClient.newHttpClient();

    public CddClient(CddProperties props) {
        this.props = props;
    }

    public List<BarDto> fetchMinuteBars(String exchange, String symbol, String interval) throws Exception {
        String lower = exchange.toLowerCase(Locale.ROOT);
        String fileName = exchange + "_" + symbol + "_" + interval + ".csv";

        List<URI> candidates = List.of(
                UriComponentsBuilder.fromHttpUrl(props.getBaseUrl()).path("/data/" + lower + "/").path(fileName).build(true).toUri(),
                UriComponentsBuilder.fromHttpUrl(props.getBaseUrl()).path("/cdd/").path(fileName).build(true).toUri()
        );

        Exception lastEx = null;
        for (URI uri : candidates) {
            try {
                return downloadAndParse(uri);
            } catch (Exception ex) {
                lastEx = ex;
            }
        }
        throw lastEx != null ? lastEx : new IllegalStateException("No candidate URL worked");
    }

    public List<BarDto> fetchFromDirectUrl(String csvUrl) throws Exception {
        return downloadAndParse(URI.create(csvUrl));
    }

    private List<BarDto> downloadAndParse(URI uri) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("CDD HTTP " + resp.statusCode() + " at " + uri);
        }
        String body = new String(resp.body(), StandardCharsets.UTF_8);
        return parseCsv(body);
    }

    private List<BarDto> parseCsv(String csv) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));

        List<String> lines = reader.lines().filter(l -> !l.trim().startsWith("#") && !l.trim().isEmpty()).collect(Collectors.toList());
        if (lines.isEmpty()) return List.of();

        String header = lines.get(0);
        String[] cols = header.split(",", -1);
        Map<String,Integer> idx = new HashMap<>();
        for (int i = 0; i < cols.length; i++) {
            idx.put(normalize(cols[i]), i);
        }

        Integer tIdx = idx.getOrDefault("unix", idx.get("timestamp"));
        if (tIdx == null) tIdx = idx.get("date");
        Integer oIdx = idx.get("open");
        Integer hIdx = idx.get("high");
        Integer lIdx = idx.get("low");
        Integer cIdx = idx.get("close");
        Integer vIdx = firstNonNull(
                idx.get("volumebtc"), idx.get("volume(crypto)"), idx.get("volume crypto"),
                idx.get("volume"), idx.get("volumebaseccy"), idx.get("volumebase")
        );

        if (oIdx == null || hIdx == null || lIdx == null || cIdx == null || vIdx == null || tIdx == null) {
            throw new IllegalStateException("CSV missing required columns. Got: " + header);
        }

        List<BarDto> out = new ArrayList<>(lines.size());
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

        for (int i = 1; i < lines.size(); i++) {
            String[] f = lines.get(i).split(",", -1);
            if (f.length < cols.length) continue;

            Instant time;
            String tRaw = f[tIdx].trim();
            if (tRaw.matches("\\d+")) {
                long v = Long.parseLong(tRaw);
                if (tRaw.length() > 10) time = Instant.ofEpochMilli(v);
                else time = Instant.ofEpochSecond(v);
            } else {
                time = Instant.from(dt.parse(tRaw));
            }

            double open  = parseDoubleSafe(f[oIdx]);
            double high  = parseDoubleSafe(f[hIdx]);
            double low   = parseDoubleSafe(f[lIdx]);
            double close = parseDoubleSafe(f[cIdx]);
            double vol   = parseDoubleSafe(f[vIdx]);

            out.add(new BarDto(time, open, high, low, close, vol));
        }

        out.sort(Comparator.comparing(BarDto::time));
        return out;
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replace(" ", "").replace("_","").trim();
    }
    @SafeVarargs private static <T> T firstNonNull(T... arr) {
        for (T t : arr) if (t != null) return t;
        return null;
    }
    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return Double.NaN; }
    }

    public List<BarDto> fetchFromPath(Path path) throws Exception {
    String body = Files.readString(path, StandardCharsets.UTF_8);
    return parseCsv(body);
    }

    /** Load ALL CSVs in a folder matching exchange+symbol+interval (e.g., Binance_BTCUSDT_minute*.csv). */
/** Load ALL CSVs in a FOLDER that match the CDD naming (supports year between symbol & interval). */
    public List<BarDto> fetchAllFromFolder(Path folder, String exchange, String symbol, String interval) throws Exception {
        Pattern pat = cddFilePattern(exchange, symbol, interval);

        if (!Files.exists(folder)) {
            throw new IllegalStateException("Folder not found: " + folder.toAbsolutePath());
        }

        List<Path> files;
        try (var stream = Files.list(folder)) {
            files = stream
                    .filter(p -> pat.matcher(p.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (files.isEmpty()) {
            throw new IllegalStateException("No CSVs matching pattern for " + exchange + "_" + symbol + "_<YEAR>_" + interval
                    + " under " + folder.toAbsolutePath());
        }

        List<BarDto> merged = new ArrayList<>();
        for (Path p : files) {
            merged.addAll(fetchFromPath(p));
        }
        return merged;
    }

    /** Load ALL CSVs from CLASSPATH dir that match the CDD naming (supports year between symbol & interval). */
    public List<BarDto> fetchAllFromClasspathDir(String classpathDir, String exchange, String symbol, String interval) throws Exception {
        Pattern pat = cddFilePattern(exchange, symbol, interval);
        String pattern = "classpath:" + (classpathDir.endsWith("/") ? classpathDir : classpathDir + "/") + "*.csv";

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(pattern);
        if (resources.length == 0) {
            throw new IllegalStateException("No CSVs found on classpath: " + pattern);
        }

        Arrays.sort(resources, Comparator.comparing(r -> {
            String n = r.getFilename();
            return n == null ? "" : n.toLowerCase(Locale.ROOT);
        }));

        List<BarDto> merged = new ArrayList<>();
        for (Resource r : resources) {
            String name = Optional.ofNullable(r.getFilename()).orElse("");
            if (!pat.matcher(name).matches()) continue;

            String body = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            merged.addAll(parseCsv(body));
        }

        if (merged.isEmpty()) {
            throw new IllegalStateException("Found CSVs but none matched pattern for "
                    + exchange + "_" + symbol + "_<YEAR>_" + interval + " in classpath:" + classpathDir);
        }
        return merged;
    }



    // NEW: filename pattern helper (supports both new and old layouts)
    private static Pattern cddFilePattern(String exchange, String symbol, String interval) {
        String ex = Pattern.quote(exchange.toLowerCase(Locale.ROOT));
        String sy = Pattern.quote(symbol.toLowerCase(Locale.ROOT));
        String iv = Pattern.quote(interval.toLowerCase(Locale.ROOT));

        // Matches:
        //   Binance_BTCUSDT_2020_minute.csv
        //   Binance_BTCUSDT_2020-01_minute.csv   (optional YYYY-MM variant)
        //   Binance_BTCUSDT_minute.csv           (old format, no year)
        String regex = "^(?:" + ex + "_" + sy + ")(?:_\\d{4}(?:-\\d{2})?)?_" + iv + "\\.csv$";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

}
