package com.fraud.detector.common;

/** CRUD ekranlarindaki is kurali ihlalleri icin (orn. "admin kendini silemez"). */
public class IsKuraliIhlaliException extends RuntimeException {
    public IsKuraliIhlaliException(String mesaj) {
        super(mesaj);
    }
}
