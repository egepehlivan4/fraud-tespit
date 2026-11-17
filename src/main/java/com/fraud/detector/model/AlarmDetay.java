package com.fraud.detector.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlarmDetay(
        int id,
        LocalDateTime tarih,
        String isyeriAdi,
        String kartSon4,
        String sahip,
        BigDecimal tutar,
        int toplamPuan,
        String durum,
        Boolean musteriTeyit,
        int hesapId,
        int kullaniciId
) {
}
