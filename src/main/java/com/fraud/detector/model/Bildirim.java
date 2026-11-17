package com.fraud.detector.model;

import java.time.LocalDateTime;

public record Bildirim(int id, String tip, String mesaj, boolean okundu, LocalDateTime olusturma) {
}
