package com.fraud.detector.model;

import java.math.BigDecimal;

public record MusteriAyar(BigDecimal toleransYuzde, boolean otomatikTolerans, boolean bildirimAktif) {
}
