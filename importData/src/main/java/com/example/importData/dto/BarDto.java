package com.example.importData.dto;

import java.time.Instant;

public record BarDto(
        Instant time,
        double open,
        double high,
        double low,
        double close,
        double volume
) {}
