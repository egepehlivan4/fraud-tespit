package com.fraud.detector.model;

public record Isyeri(
        Integer id,
        String ad,
        int kategoriId,
        String kategoriAdi,
        int sehirId,
        String sehirAdi
) {
}
