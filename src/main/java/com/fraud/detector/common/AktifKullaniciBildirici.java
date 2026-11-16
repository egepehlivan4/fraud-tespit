package com.fraud.detector.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Her yazma isleminden once aktif kullaniciyi veritabanina bildirir (CLAUDE.md kurali).
 *
 * set_config'in ucuncu parametresi (is_local) TRUE verilir — bu SET LOCAL ile
 * esdegerdir: deger yalnizca icinde bulunulan TRANSACTION boyunca gecerlidir ve
 * transaction COMMIT/ROLLBACK olunca otomatik silinir. Bu sayede HikariCP
 * connection pool'daki bir fiziksel baglanti islem bitince havuza geri
 * dondugunde, bir sonraki istek (baska bir kullanici) o baglantiyi tekrar
 * alsa bile eski kullanici_id degeri sizmaz.
 *
 * ONEMLI: Bu metod MUTLAKA @Transactional bir servis metodunun ICINDEN,
 * asil yazma sorgusundan ONCE cagrilmalidir. Aksi halde (autocommit modunda,
 * transaction yokken) set_config'in etkisi kendisini calistiran tek deyimle
 * birlikte hemen sona erer ve ayni metod icindeki bir sonraki sorgu bile
 * (JdbcTemplate farkli bir pooled baglanti alabileceginden) bu degeri
 * goremeyebilir. @Transactional sayesinde Spring, DataSourceTransactionManager
 * uzerinden ayni thread'deki tum JdbcTemplate cagrilarini AYNI fiziksel
 * baglantiya (connection) baglar (bkz. DataSourceUtils.getConnection) —
 * boylece set_config burada, asil UPDATE/INSERT ise bir sonraki satirda
 * calissa da ikisi ayni baglanti ve ayni transaction icinde yer alir.
 */
@Component
public class AktifKullaniciBildirici {

    private final JdbcTemplate jdbcTemplate;

    public AktifKullaniciBildirici(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ayarla(int kullaniciId) {
        jdbcTemplate.queryForObject(
                "SELECT set_config('app.kullanici_id', ?, true)",
                String.class,
                String.valueOf(kullaniciId));
    }
}
