package com.fraud.detector.model;

import java.time.LocalDateTime;

public record DenetimLogSatiri(
        long id,
        String tablo,
        Long kayitId,
        String islemTipi,
        String eskiJson,
        String yeniJson,
        Integer kullaniciId,
        String kullaniciAdi,
        LocalDateTime zaman
) {
}
