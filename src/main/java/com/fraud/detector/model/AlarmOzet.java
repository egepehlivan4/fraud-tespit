package com.fraud.detector.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlarmOzet(
        int id,
        LocalDateTime tarih,
        String isyeriAdi,
        String sahip,
        BigDecimal tutar,
        int toplamPuan,
        String durum,
        Boolean musteriTeyit
) {
}
