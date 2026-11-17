package com.fraud.detector.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HarcamaGrafikNokta(
        LocalDate ay,
        BigDecimal gercek,
        BigDecimal beklenen,
        BigDecimal ustSinir
) {
}
