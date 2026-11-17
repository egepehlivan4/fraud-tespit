package com.fraud.detector.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BekleyenOnay(int alarmId, LocalDateTime tarih, String isyeriAdi, BigDecimal tutar) {
}
