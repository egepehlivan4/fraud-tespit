package com.fraud.detector.model;

import java.math.BigDecimal;

public record SehirKategoriRaporSatiri(
        String sehir,
        String kategori,
        long islemSayisi,
        long alarmSayisi,
        long fraudSayisi,
        BigDecimal alarmOrani
) {
}
