package com.fraud.detector.repository;

import com.fraud.detector.common.AktifKullaniciBildirici;
import com.fraud.detector.model.Kural;
import com.fraud.detector.model.KuralRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class KuralRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AktifKullaniciBildirici aktifKullanici;

    public KuralRepository(JdbcTemplate jdbcTemplate, AktifKullaniciBildirici aktifKullanici) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktifKullanici = aktifKullanici;
    }

    public List<Kural> listele() {
        return jdbcTemplate.query("SELECT * FROM kural ORDER BY kod, esik", new KuralRowMapper());
    }

    public Optional<Kural> bul(int id) {
        return jdbcTemplate.query("SELECT * FROM kural WHERE id = ?", new KuralRowMapper(), id)
                .stream().findFirst();
    }

    @Transactional
    public void ekle(String kod, String ad, int puan, BigDecimal esik, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update(
                "INSERT INTO kural(kod, ad, puan, esik) VALUES (?, ?, ?, ?)",
                kod, ad, puan, esik);
    }

    /** kod kasten parametre degil — bir kuralin tipi olusturulduktan sonra degistirilmez. */
    @Transactional
    public void guncelle(int id, String ad, int puan, BigDecimal esik, boolean aktif, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update(
                "UPDATE kural SET ad = ?, puan = ?, esik = ?, aktif = ? WHERE id = ?",
                ad, puan, esik, aktif, id);
    }

    /** kural_ihlal kaydi varsa (kural_id FK, CASCADE yok) DataIntegrityViolationException firlatir. */
    @Transactional
    public void kalicaSil(int id, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("DELETE FROM kural WHERE id = ?", id);
    }

    @Transactional
    public void pasifYap(int id, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("UPDATE kural SET aktif = false WHERE id = ?", id);
    }

    public BigDecimal alarmEsikGetir() {
        return jdbcTemplate.queryForObject(
                "SELECT deger FROM ayar WHERE anahtar = 'ALARM_ESIK'", BigDecimal.class);
    }

    @Transactional
    public void alarmEsikGuncelle(BigDecimal deger, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("UPDATE ayar SET deger = ? WHERE anahtar = 'ALARM_ESIK'", deger);
    }
}
