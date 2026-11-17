package com.fraud.detector.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IslemOzet(
        long id,
        LocalDateTime tarih,
        String isyeriAdi,
        BigDecimal tutar,
        String durum,
        String kartSon4
) {
}
