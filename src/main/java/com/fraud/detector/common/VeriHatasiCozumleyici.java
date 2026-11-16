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
            case "P0001" -> sqlEx.getMessage();
            default -> "Veritabanı hatası: " + sqlEx.getMessage();
        };
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
