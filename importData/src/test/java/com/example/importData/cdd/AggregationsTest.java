package com.example.importData.cdd;

import com.example.importData.Aggregations;
import com.example.importData.dto.BarDto;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationsTest {

    @Test
    void aggregatesTo4mCorrectly() {
        List<BarDto> mins = new ArrayList<>();
        Instant t0 = Instant.parse("2024-08-23T00:00:00Z");
        // Create 8 one-minute bars with rising close and varying highs/lows and volumes
        for (int i = 0; i < 8; i++) {
            double base = 100 + i;
            mins.add(new BarDto(
                    t0.plusSeconds(60L * i),
                    base,         // open
                    base + 2,     // high
                    base - 2,     // low
                    base + 0.5,   // close
                    10 + i        // volume
            ));
        }

        List<BarDto> agg = Aggregations.aggregate(mins, Duration.ofMinutes(4));
        assertEquals(2, agg.size(), "Expected 2 aggregated 4m bars from 8 minutes");

        BarDto b0 = agg.get(0);
        assertEquals(t0.plusSeconds(60L * 3), b0.time());
        assertEquals(100.0, b0.open(), 1e-9);
        assertEquals(103 + 2, b0.high(), 1e-9); // last minute high in bucket
        assertEquals(100 - 2, b0.low(), 1e-9);  // min low in bucket
        assertEquals(103.5, b0.close(), 1e-9);  // last minute close in bucket
        assertEquals((10+0)+(10+1)+(10+2)+(10+3), b0.volume(), 1e-9);

        BarDto b1 = agg.get(1);
        assertEquals(t0.plusSeconds(60L * 7), b1.time());
        assertEquals(104.0, b1.open(), 1e-9);
        assertEquals(107 + 2, b1.high(), 1e-9);
        assertEquals(104 - 2, b1.low(), 1e-9);
        assertEquals(107.5, b1.close(), 1e-9);
        assertEquals((10+4)+(10+5)+(10+6)+(10+7), b1.volume(), 1e-9);
    }

    @Test
    void dropsPartialTailByDefault() {
        List<BarDto> mins = new ArrayList<>();
        Instant t0 = Instant.parse("2024-08-23T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            mins.add(new BarDto(
                    t0.plusSeconds(60L * i),
                    1+i, 2+i, 0+i, 1.5+i, 10
            ));
        }
        List<BarDto> agg = Aggregations.aggregate(mins, Duration.ofMinutes(4));
        assertEquals(1, agg.size(), "5 minutes should yield only one 4m bar (tail dropped)");
    }
}
