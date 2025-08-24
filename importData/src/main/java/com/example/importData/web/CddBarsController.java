package com.example.importData.web;

import com.example.importData.CddBarSeriesService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ta4j.core.BarSeries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

@RestController
public class CddBarsController {

    private static final String CLASSPATH_DIR = "sample/BinanceBTCData";

    private final CddBarSeriesService service;

    public CddBarsController(CddBarSeriesService service) {
        this.service = service;
    }

    @GetMapping("/cdd/bars")
    public Map<String, Object> cddBars(
            @RequestParam(defaultValue = "Binance") String exchange,
            @RequestParam String symbol,
            @RequestParam(name = "tf", defaultValue = "4m") String tf
    ) throws Exception {
        Duration target = parseTf(tf);
        BarSeries s = service.fetchSeries(exchange, symbol, "minute", target);
        return Map.of(
                "seriesName", s.getName(),
                "barCount", s.getBarCount(),
                "lastClose", s.getLastBar().getClosePrice().toString(),
                "lastTime", s.getLastBar().getEndTime().toString()
        );
    }

    @GetMapping("/cdd/bars/url")
    public Map<String, Object> cddBarsFromUrl(
            @RequestParam String csvUrl,
            @RequestParam String name,
            @RequestParam(name = "tf", defaultValue = "4m") String tf
    ) throws Exception {
        Duration target = parseTf(tf);
        BarSeries s = service.fetchSeriesFromUrl(csvUrl, name, target);
        return Map.of(
                "seriesName", s.getName(),
                "barCount", s.getBarCount(),
                "lastClose", s.getLastBar().getClosePrice().toString(),
                "lastTime", s.getLastBar().getEndTime().toString()
        );
    }

    // Serve the synthetic CSV from resources to demo the /url endpoint
    @GetMapping(value = "/sample/Binance_BTCUSDT_minute.csv", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> sampleCsv() throws Exception {
        var res = new ClassPathResource("sample/Binance_BTCUSDT_minute.csv");
        try (var in = res.getInputStream()) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(in.readAllBytes());
        }
    }

    @GetMapping("/cdd/bars/4m")
    public Map<String, Object> cddBars4m(
            @RequestParam(defaultValue = "Binance") String exchange,
            @RequestParam String symbol
    ) throws Exception {
        BarSeries s = service.fetchSeries(exchange, symbol, "minute", Duration.ofMinutes(4));
        return Map.of(
                "seriesName", s.getName(),
                "barCount", s.getBarCount(),
                "lastClose", s.getLastBar().getClosePrice().toString(),
                "lastTime", s.getLastBar().getEndTime().toString()
        );
    }

    @GetMapping("/cdd/bars/url/4m")
    public Map<String, Object> cddBarsFromUrl4m(
            @RequestParam String csvUrl,
            @RequestParam String name
    ) throws Exception {
        BarSeries s = service.fetchSeriesFromUrl(csvUrl, name, Duration.ofMinutes(4));
        return Map.of(
                "seriesName", s.getName(),
                "barCount", s.getBarCount(),
                "lastClose", s.getLastBar().getClosePrice().toString(),
                "lastTime", s.getLastBar().getEndTime().toString()
        );
    }

    @GetMapping("/cdd/bars/24m")
    public Map<String, Object> cddBars24m(
            @RequestParam(defaultValue = "Binance") String exchange,
            @RequestParam String symbol
    ) throws Exception {
        BarSeries s = service.fetchSeries(exchange, symbol, "minute", Duration.ofMinutes(24));
        return Map.of(
                "seriesName", s.getName(),
                "barCount", s.getBarCount(),
                "lastClose", s.getLastBar().getClosePrice().toString(),
                "lastTime", s.getLastBar().getEndTime().toString()
        );
    }

    @GetMapping("/cdd/bars/url/24m")
    public Map<String, Object> cddBarsFromUrl24m(
            @RequestParam String csvUrl,
            @RequestParam String name
    ) throws Exception {
        BarSeries s = service.fetchSeriesFromUrl(csvUrl, name, Duration.ofMinutes(24));
        return Map.of(
                "seriesName", s.getName(),
                "barCount", s.getBarCount(),
                "lastClose", s.getLastBar().getClosePrice().toString(),
                "lastTime", s.getLastBar().getEndTime().toString()
        );
    }

    // ---- LOCAL (fixed folder) ----
    @GetMapping("/cdd/bars/local/4m")
    public Map<String, Object> cddBarsLocal4m(
        @RequestParam(defaultValue = "Binance") String exchange,
        @RequestParam String symbol
    ) throws Exception {
        BarSeries s = service.fetchSeriesFromClasspathDir(CLASSPATH_DIR, exchange, symbol, Duration.ofMinutes(4));
        return Map.of("seriesName", s.getName(), "barCount", s.getBarCount(),
                "lastClose", s.getLastBar().getClosePrice().toString(),
                "lastTime", s.getLastBar().getEndTime().toString());
    }

    @GetMapping("/cdd/bars/local/24m")
    public Map<String, Object> cddBarsLocal24m(
        @RequestParam(defaultValue = "Binance") String exchange,
        @RequestParam String symbol
    ) throws Exception {
        BarSeries s = service.fetchSeriesFromClasspathDir(CLASSPATH_DIR, exchange, symbol, Duration.ofMinutes(24));
        return Map.of("seriesName", s.getName(), "barCount", s.getBarCount(),
                "lastClose", s.getLastBar().getClosePrice().toString(),
                "lastTime", s.getLastBar().getEndTime().toString());
    }

    private static Duration parseTf(String tf) {
        String v = tf.trim().toLowerCase();
        if (v.endsWith("m")) {
            int mins = Integer.parseInt(v.substring(0, v.length() - 1));
            return Duration.ofMinutes(mins);
        }
        throw new IllegalArgumentException("Unsupported tf: " + tf + " (use 4m or 24m)");
    }
}
