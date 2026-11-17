package com.fraud.detector.repository;

import com.fraud.detector.common.AktifKullaniciBildirici;
import com.fraud.detector.model.Isyeri;
import com.fraud.detector.model.IsyeriRowMapper;
import com.fraud.detector.model.Kategori;
import com.fraud.detector.model.Sehir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class IsyeriRepository {

    private static final String TEMEL_SORGU = """
            SELECT y.id, y.ad, y.kategori_id, k.ad AS kategori_adi, y.sehir_id, s.ad AS sehir_adi
            FROM isyeri y JOIN kategori k ON k.id = y.kategori_id JOIN sehir s ON s.id = y.sehir_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AktifKullaniciBildirici aktifKullanici;

    public IsyeriRepository(JdbcTemplate jdbcTemplate, AktifKullaniciBildirici aktifKullanici) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktifKullanici = aktifKullanici;
    }

    public List<Kategori> kategoriler() {
        return jdbcTemplate.query("SELECT id, ad FROM kategori ORDER BY ad",
                (rs, n) -> new Kategori(rs.getInt("id"), rs.getString("ad")));
    }

    public List<Sehir> sehirler() {
        return jdbcTemplate.query("SELECT id, ad FROM sehir ORDER BY ad",
                (rs, n) -> new Sehir(rs.getInt("id"), rs.getString("ad")));
    }

    public List<Isyeri> listele() {
        return jdbcTemplate.query(TEMEL_SORGU + " ORDER BY y.id", new IsyeriRowMapper());
    }

    public Optional<Isyeri> bul(int id) {
        return jdbcTemplate.query(TEMEL_SORGU + " WHERE y.id = ?", new IsyeriRowMapper(), id)
                .stream().findFirst();
    }

    @Transactional
    public int ekle(String ad, int kategoriId, int sehirId, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        KeyHolder anahtar = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO isyeri(ad, kategori_id, sehir_id) VALUES (?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, ad);
            ps.setInt(2, kategoriId);
            ps.setInt(3, sehirId);
            return ps;
        }, anahtar);
        return anahtar.getKey().intValue();
    }

    @Transactional
    public void guncelle(int id, String ad, int kategoriId, int sehirId, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update(
                "UPDATE isyeri SET ad = ?, kategori_id = ?, sehir_id = ? WHERE id = ?",
                ad, kategoriId, sehirId, id);
    }

    /** Islemi olan isyeri FK ihlali (23503) ile DataIntegrityViolationException firlatir. */
    @Transactional
    public void sil(int id, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("DELETE FROM isyeri WHERE id = ?", id);
    }
}
