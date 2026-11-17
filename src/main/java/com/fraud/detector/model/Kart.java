package com.fraud.detector.model;

import java.time.LocalDateTime;

public record Kart(
        int id,
        int hesapId,
        String iban,
        String sahip,
        String son4,
        String durum,
        LocalDateTime olusturma
) {
}
