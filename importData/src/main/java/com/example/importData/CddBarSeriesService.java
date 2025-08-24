package com.example.importData;

import com.example.importData.dto.BarDto;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DecimalNum;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CddBarSeriesService {

    private final CddClient client;

    public CddBarSeriesService(CddClient client) { this.client = client; }

    public BarSeries fetchSeries(String exchange, String symbol, String interval, Duration target) throws Exception {
        List<BarDto> oneMinute = client.fetchMinuteBars(exchange, symbol, interval);
        List<BarDto> agg = Aggregations.aggregate(oneMinute, target);
        String name = symbol + "-" + target.toMinutes() + "m-cdd";
        BarSeries series = new BaseBarSeriesBuilder().withName(name).build();
        for (BarDto b : agg) {
            series.addBar(new BaseBar(target, b.time(), 
                DecimalNum.valueOf(b.open()), 
                DecimalNum.valueOf(b.high()), 
                DecimalNum.valueOf(b.low()), 
                DecimalNum.valueOf(b.close()), 
                DecimalNum.valueOf(b.volume()), 
                DecimalNum.valueOf(0), // amount - not available in BarDto
                0)); // trades - not available in BarDto
        }
        return series;
    }

    public BarSeries fetchSeriesFromUrl(String csvUrl, String seriesName, Duration target) throws Exception {
        List<BarDto> oneMinute = client.fetchFromDirectUrl(csvUrl);
        List<BarDto> agg = Aggregations.aggregate(oneMinute, target);
        BarSeries series = new BaseBarSeriesBuilder().withName(seriesName).build();
        for (BarDto b : agg) {
            series.addBar(new BaseBar(target, b.time(), 
                DecimalNum.valueOf(b.open()), 
                DecimalNum.valueOf(b.high()), 
                DecimalNum.valueOf(b.low()), 
                DecimalNum.valueOf(b.close()), 
                DecimalNum.valueOf(b.volume()), 
                DecimalNum.valueOf(0), // amount - not available in BarDto
                0)); // trades - not available in BarDto
        }
        return series;
    }

    public BarSeries fetchSeriesFromFolder(Path folder, String exchange, String symbol, Duration target) throws Exception {
        // 1) load all minute bars
        List<BarDto> all = client.fetchAllFromFolder(folder, exchange, symbol, "minute");
        // 2) dedupe + sort ascending
        List<BarDto> cleaned = dedupeAndSort(all);
        // 3) aggregate
        List<BarDto> agg = Aggregations.aggregate(cleaned, target);
        // 4) build TA4J series
        String name = symbol + "-" + target.toMinutes() + "m-local";
        BarSeries series = new BaseBarSeriesBuilder().withName(name).build();
        for (BarDto b : agg) {
            series.addBar(
                new BaseBar(target, b.time(), 
                DecimalNum.valueOf(b.open()), 
                DecimalNum.valueOf(b.high()), 
                DecimalNum.valueOf(b.low()), 
                DecimalNum.valueOf(b.close()), 
                DecimalNum.valueOf(b.volume()), 
                DecimalNum.valueOf(0), // amount - not available in BarDto
                0)
            );
        }
        return series;
    }

    private static List<BarDto> dedupeAndSort(List<BarDto> bars) {
        // TreeMap sorts by Instant ascending and overwrites duplicates by key (last wins)
        Map<Instant, BarDto> byTime = new TreeMap<>();
        for (BarDto b : bars) {
            // skip malformed rows
            if (!Double.isFinite(b.open()) || !Double.isFinite(b.high()) ||
                !Double.isFinite(b.low())  || !Double.isFinite(b.close())) continue;
            byTime.put(b.time(), b);
        }
        return new ArrayList<>(byTime.values());
    }

    public BarSeries fetchSeriesFromClasspathDir(String classpathDir,
                                             String exchange, String symbol,
                                             Duration target) throws Exception {
        List<BarDto> all = client.fetchAllFromClasspathDir(classpathDir, exchange, symbol, "minute");
        List<BarDto> cleaned = dedupeAndSort(all);                // your existing helper
        List<BarDto> agg = Aggregations.aggregate(cleaned, target);
        BarSeries s = new BaseBarSeriesBuilder().withName(symbol + "-" + target.toMinutes() + "m-local").build();
        for (BarDto b : agg) s.addBar(new BaseBar(target, b.time(), 
                DecimalNum.valueOf(b.open()), 
                DecimalNum.valueOf(b.high()), 
                DecimalNum.valueOf(b.low()), 
                DecimalNum.valueOf(b.close()), 
                DecimalNum.valueOf(b.volume()), 
                DecimalNum.valueOf(0), // amount - not available in BarDto
                0));
        return s;
    }
}
