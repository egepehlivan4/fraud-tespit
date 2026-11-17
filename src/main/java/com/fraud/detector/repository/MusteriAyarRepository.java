package com.fraud.detector.repository;

import com.fraud.detector.common.AktifKullaniciBildirici;
import com.fraud.detector.model.MusteriAyar;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public class MusteriAyarRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AktifKullaniciBildirici aktifKullanici;

    public MusteriAyarRepository(JdbcTemplate jdbcTemplate, AktifKullaniciBildirici aktifKullanici) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktifKullanici = aktifKullanici;
    }

    public Optional<MusteriAyar> bul(int kullaniciId) {
        return jdbcTemplate.query(
                "SELECT tolerans_yuzde, otomatik_tolerans, bildirim_aktif FROM musteri_ayar WHERE kullanici_id = ?",
                (rs, n) -> new MusteriAyar(
                        rs.getBigDecimal("tolerans_yuzde"),
                        rs.getBoolean("otomatik_tolerans"),
                        rs.getBoolean("bildirim_aktif")),
                kullaniciId).stream().findFirst();
    }

    /**
     * Admin'den eklenen yeni MUSTERI icin musteri_ayar satiri olusturulmaz
     * (fn_ust_sinir COALESCE ile varsayilan kullanir) — bu yuzden UPSERT.
     */
    @Transactional
    public void guncelle(int kullaniciId, BigDecimal toleransYuzde, boolean otomatikTolerans,
                          boolean bildirimAktif, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("""
                INSERT INTO musteri_ayar(kullanici_id, tolerans_yuzde, otomatik_tolerans, bildirim_aktif)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (kullanici_id) DO UPDATE SET
                    tolerans_yuzde = EXCLUDED.tolerans_yuzde,
                    otomatik_tolerans = EXCLUDED.otomatik_tolerans,
                    bildirim_aktif = EXCLUDED.bildirim_aktif
                """, kullaniciId, toleransYuzde, otomatikTolerans, bildirimAktif);
    }
}
