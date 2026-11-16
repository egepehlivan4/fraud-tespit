package com.fraud.detector.model;

import java.math.BigDecimal;

public record Kural(
        Integer id,
        String kod,
        String ad,
        int puan,
        BigDecimal esik,
        boolean aktif
) {
}
