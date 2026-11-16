package com.fraud.detector.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Iki farkli kullanici (admin id=1, analist id=2) art arda kural puani
 * guncelliyor. Her guncelleme trg_denetim_kural'i tetikler; bu testin amaci
 * denetim_log.kullanici_id'nin HER SEFERINDE o anki gercek kullaniciyi
 * gostermesi, onceki kullanicinin degerinin (connection pool'da baglanti
 * yeniden kullanilsa bile) bir sonrakine sizmamasidir.
 *
 * @Transactional: test metodu bitince Spring tum degisiklikleri (ve
 * tetiklenen denetim_log satirlarini) otomatik ROLLBACK eder — demo
 * verisinde kalici bir iz birakmaz, elle geri alma kodu gerekmez.
 */
@SpringBootTest
@Transactional
class KuralServiceTest {

    @Autowired
    private KuralService kuralService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void ikiFarkliKullaniciArtArdaGuncelleyinceDenetimLogDogruKullaniciyiYazar() {
        kuralService.puanGuncelle("GECE", 21, 1);   // Sistem Yoneticisi (admin)
        kuralService.puanGuncelle("GECE", 22, 2);   // Fraud Analisti

        List<Map<String, Object>> loglar = jdbcTemplate.queryForList(
                "SELECT kullanici_id, yeni->>'puan' AS puan FROM denetim_log " +
                "WHERE tablo = 'kural' ORDER BY id DESC LIMIT 2");

        // en son yazilan satir en ustte: once analist (2), sonra admin (1)
        assertThat(loglar.get(0).get("kullanici_id")).isEqualTo(2);
        assertThat(loglar.get(0).get("puan")).isEqualTo("22");
        assertThat(loglar.get(1).get("kullanici_id")).isEqualTo(1);
        assertThat(loglar.get(1).get("puan")).isEqualTo("21");
    }
}
