package com.fraud.detector.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Gercek PostgreSQL hatalari uretip VeriHatasiCozumleyici'nin bunlari
 * anlasilir Turkce mesaja cevirdigini dogrular.
 *
 * @Transactional: her test metodu kendi transaction'inda calisir ve
 * test bitince otomatik ROLLBACK edilir (bir hata sonrasi Postgres
 * transaction'i "aborted" durumuna da gecse, rollback bunu da temizler) —
 * demo verisinde kalici degisiklik birakmaz, elle geri alma gerekmez.
 */
@SpringBootTest
@Transactional
class VeriHatasiCozumleyiciTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void fkIhlaliAnlasilirMesajaCevrilir() {
        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO isyeri(ad, kategori_id, sehir_id) VALUES (?, ?, ?)",
                        "Test Isyeri", 99999, 1));

        String mesaj = VeriHatasiCozumleyici.anlasilirMesaj(ex);
        assertThat(mesaj).contains("kullanıldığı için silinemiyor");
    }

    @Test
    void benzersizlikIhlaliAnlasilirMesajaCevrilir() {
        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update("INSERT INTO rol(ad) VALUES ('MUSTERI')"));

        String mesaj = VeriHatasiCozumleyici.anlasilirMesaj(ex);
        assertThat(mesaj).contains("benzersiz olmalı");
    }

    @Test
    void trigerRaiseExceptionMesajiOldugiGibiGosterilir() {
        jdbcTemplate.update("UPDATE kart SET durum = 'BLOKE' WHERE id = 1");

        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO islem(kart_id, isyeri_id, tutar) VALUES (1, 1, 50)"));

        String mesaj = VeriHatasiCozumleyici.anlasilirMesaj(ex);
        assertThat(mesaj).contains("BLOKE");
    }
}
