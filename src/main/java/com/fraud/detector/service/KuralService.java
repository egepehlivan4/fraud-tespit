package com.fraud.detector.service;

import com.fraud.detector.common.AktifKullaniciBildirici;
import com.fraud.detector.model.Kural;
import com.fraud.detector.model.KuralRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Asama 4'teki tam Kural CRUD ekraninin altyapisi; su an sadece
 * app.kullanici_id mekanizmasini dogrulamak icin tek bir yazma metodu var.
 */
@Service
public class KuralService {

    private final JdbcTemplate jdbcTemplate;
    private final AktifKullaniciBildirici aktifKullanici;

    public KuralService(JdbcTemplate jdbcTemplate, AktifKullaniciBildirici aktifKullanici) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktifKullanici = aktifKullanici;
    }

    public List<Kural> hepsi() {
        return jdbcTemplate.query("SELECT * FROM kural ORDER BY kod", new KuralRowMapper());
    }

    @Transactional
    public void puanGuncelle(String kod, int yeniPuan, int kullaniciId) {
        aktifKullanici.ayarla(kullaniciId);
        jdbcTemplate.update("UPDATE kural SET puan = ? WHERE kod = ?", yeniPuan, kod);
    }
}
