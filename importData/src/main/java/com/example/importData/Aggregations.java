package com.example.importData;

import com.example.importData.dto.BarDto;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Aggregations {

    /** Aggregate 1-minute bars into N-minute bars (N must be >=1 minute). */
    public static List<BarDto> aggregate(List<BarDto> oneMinuteBars, Duration target) {
        if (target.toMinutes() < 1 || (target.toMinutes() % 1) != 0) {
            throw new IllegalArgumentException("Target must be whole minutes");
        }
        long n = target.toMinutes();
        if (n == 1) return oneMinuteBars;

        List<BarDto> out = new ArrayList<>();
        List<BarDto> bucket = new ArrayList<>((int)n);

        for (BarDto bar : oneMinuteBars) {
            bucket.add(bar);
            if (bucket.size() == n) {
                out.add(mergeBucket(bucket));
                bucket.clear();
            }
        }
        // drop partial tail
        return out;
    }

    private static BarDto mergeBucket(List<BarDto> bucket) {
        Instant t = bucket.get(bucket.size() - 1).time();
        double open  = bucket.get(0).open();
        double close = bucket.get(bucket.size()-1).close();
        double high  = bucket.stream().mapToDouble(BarDto::high).max().orElse(open);
        double low   = bucket.stream().mapToDouble(BarDto::low).min().orElse(open);
        double vol   = bucket.stream().mapToDouble(BarDto::volume).sum();
        return new BarDto(t, open, high, low, close, vol);
    }
}
