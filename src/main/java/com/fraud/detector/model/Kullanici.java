package com.fraud.detector.model;

import java.time.LocalDateTime;

public record Kullanici(
        Integer id,
        String adSoyad,
        String email,
        String sifreHash,
        int rolId,
        String rolAdi,
        boolean aktif,
        LocalDateTime olusturma
) {
}
