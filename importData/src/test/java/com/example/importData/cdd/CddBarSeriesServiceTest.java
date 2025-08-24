package com.example.importData.cdd;

import com.example.importData.CddBarSeriesService;
import com.example.importData.CddClient;
import com.example.importData.config.CddProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;

@SpringBootTest
@ContextConfiguration(classes = { CddBarSeriesServiceTest.TestConfig.class })
public class CddBarSeriesServiceTest {

    private static HttpServer httpServer;
    private static String sampleCsv;

    @Configuration
    static class TestConfig {
        @Bean CddProperties props() { return new CddProperties(); }
        @Bean CddClient client(CddProperties p) { return new CddClient(p); }
        @Bean CddBarSeriesService service(CddClient c) { return new CddBarSeriesService(c); }
    }

    @BeforeAll
    static void startServer() throws Exception {
        // Load sample CSV from resources
        sampleCsv = Files.readString(Paths.get("src/main/resources/sample/Binance_BTCUSDT_minute.csv"), StandardCharsets.UTF_8);

        httpServer = HttpServer.create(new InetSocketAddress(0), 0); // random free port
        httpServer.createContext("/sample.csv", exchange -> {
            byte[] bytes = sampleCsv.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });
        httpServer.start();
    }

    @AfterAll
    static void stopServer() {
        if (httpServer != null) httpServer.stop(0);
    }

    @Test
    void fetchSeriesFromUrlAggregatesTo4m() throws Exception {
        int port = httpServer.getAddress().getPort();
        String url = "http://localhost:" + port + "/sample.csv";
        CddBarSeriesService service = new CddBarSeriesService(new CddClient(new CddProperties()));
        var series = service.fetchSeriesFromUrl(url, "BTCUSDT_4m", Duration.ofMinutes(4));
        assertNotNull(series);
        assertEquals("BTCUSDT_4m", series.getName());
        assertTrue(series.getBarCount() >= 1);
    }

    @Test
    void fetchSeriesFromUrlAggregatesTo24m() throws Exception {
        int port = httpServer.getAddress().getPort();
        String url = "http://localhost:" + port + "/sample.csv";
        CddBarSeriesService service = new CddBarSeriesService(new CddClient(new CddProperties()));
        var series = service.fetchSeriesFromUrl(url, "BTCUSDT_24m", Duration.ofMinutes(24));
        assertNotNull(series);
        assertEquals("BTCUSDT_24m", series.getName());
        // Our tiny sample has only 8 minutes; 24m aggregation should drop tail -> 0 bars.
        assertEquals(0, series.getBarCount());
    }
}
