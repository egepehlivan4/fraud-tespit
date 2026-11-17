package com.fraud.detector.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ModelOzet(
        int hesapId,
        String sahip,
        String iban,
        BigDecimal ortalama,
        BigDecimal varyans,
        int egitilenAySayisi,
        LocalDate sonAy
) {
}
