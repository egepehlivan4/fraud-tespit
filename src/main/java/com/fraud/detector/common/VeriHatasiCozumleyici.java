package com.fraud.detector.common;

import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

/**
 * PostgreSQL hatalarini (FK ihlali, benzersizlik ihlali, trigger/procedure
 * icindeki RAISE EXCEPTION) kullaniciya gosterilecek anlasilir Turkce
 * mesaja cevirir. SQLSTATE kodlari icin: 23503 = foreign key ihlali,
 * 23505 = unique ihlali, P0001 = PL/pgSQL RAISE EXCEPTION (bu durumda
 * mesaj zaten Turkce/anlasilir oldugu icin oldugu gibi gosterilir).
 */
public final class VeriHatasiCozumleyici {

    private VeriHatasiCozumleyici() {
    }

    public static String anlasilirMesaj(DataAccessException ex) {
        SQLException sqlEx = sqlExceptionBul(ex);
        if (sqlEx == null) {
            return "Beklenmeyen bir veritabanı hatası oluştu: " + ex.getMostSpecificCause().getMessage();
        }
        return switch (sqlEx.getSQLState()) {
            case "23503" -> "Bu kayıt başka bir tabloda kullanıldığı için silinemiyor veya değiştirilemiyor.";
            case "23505" -> "Bu değer zaten kullanılıyor, benzersiz olmalı.";
            case "23514" -> "Girilen değer izin verilen aralığın/listenin dışında.";
            case "P0001" -> ilkSatirTemiz(sqlEx.getMessage());
            default -> "Veritabanı hatası: " + sqlEx.getMessage();
        };
    }

    /** PostgreSQL surucusu RAISE EXCEPTION mesajina "ERROR: " oneki ve "Where:" satirlari ekler. */
    private static String ilkSatirTemiz(String mesaj) {
        if (mesaj == null) {
            return "";
        }
        String ilkSatir = mesaj.split("\\R", 2)[0].trim();
        return ilkSatir.startsWith("ERROR: ") ? ilkSatir.substring("ERROR: ".length()) : ilkSatir;
    }

    private static SQLException sqlExceptionBul(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException sqlEx) {
                return sqlEx;
            }
            t = t.getCause();
        }
        return null;
    }
}
