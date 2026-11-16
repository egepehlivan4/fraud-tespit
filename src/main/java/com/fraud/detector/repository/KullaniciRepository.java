package com.fraud.detector.repository;

import com.fraud.detector.common.AktifKullaniciBildirici;
import com.fraud.detector.model.Kullanici;
import com.fraud.detector.model.KullaniciRowMapper;
import com.fraud.detector.model.Rol;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class KullaniciRepository {

    private static final String TEMEL_SORGU = """
            SELECT k.id, k.ad_soyad, k.email, k.sifre_hash, k.rol_id, r.ad AS rol_adi,
                   k.aktif, k.olusturma
            FROM kullanici k JOIN rol r ON r.id = k.rol_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AktifKullaniciBildirici aktifKullanici;

    public KullaniciRepository(JdbcTemplate jdbcTemplate, AktifKullaniciBildirici aktifKullanici) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktifKullanici = aktifKullanici;
    }

    public List<Rol> roller() {
        return jdbcTemplate.query("SELECT id, ad FROM rol ORDER BY id",
                (rs, n) -> new Rol(rs.getInt("id"), rs.getString("ad")));
    }

    /** rolFiltre/aktifFiltre null ise o kritere bakilmaz (hepsi gelir). */
    public List<Kullanici> listele(String rolFiltre, Boolean aktifFiltre) {
        String sql = TEMEL_SORGU +
                " WHERE (?::varchar IS NULL OR r.ad = ?) AND (?::boolean IS NULL OR k.aktif = ?)" +
                " ORDER BY k.id";
        return jdbcTemplate.query(sql, new KullaniciRowMapper(),
                rolFiltre, rolFiltre, aktifFiltre, aktifFiltre);
    }

    public Optional<Kullanici> bul(int id) {
        return jdbcTemplate.query(TEMEL_SORGU + " WHERE k.id = ?", new KullaniciRowMapper(), id)
                .stream().findFirst();
    }

    @Transactional
    public int ekle(String adSoyad, String email, String sifreHash, int rolId, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        KeyHolder anahtar = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            // PostgreSQL suruculeri RETURN_GENERATED_KEYS ile butun satiri dondurur;
            // getKey() tek kolon bekler, bu yuzden kolon adini acikca belirtiyoruz.
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO kullanici(ad_soyad, email, sifre_hash, rol_id) VALUES (?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, adSoyad);
            ps.setString(2, email);
            ps.setString(3, sifreHash);
            ps.setInt(4, rolId);
            return ps;
        }, anahtar);
        return anahtar.getKey().intValue();
    }

    /**
     * MUSTERI eklenince cagrilir. hesap INSERT edilince trg_hesap_profil_ac
     * kendisi bos bir harcama_profili satiri acar — burada tekrar yazilmaz.
     */
    @Transactional
    public void hesapAc(int kullaniciId, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        String iban = "TR" + String.format("%024d", kullaniciId);
        jdbcTemplate.update(
                "INSERT INTO hesap(kullanici_id, iban, bakiye) VALUES (?, ?, 0)",
                kullaniciId, iban);
    }

    @Transactional
    public void guncelle(int id, String adSoyad, String email, int rolId, boolean aktif,
                          String yeniSifreHashYadaNull, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        if (yeniSifreHashYadaNull == null) {
            jdbcTemplate.update(
                    "UPDATE kullanici SET ad_soyad = ?, email = ?, rol_id = ?, aktif = ? WHERE id = ?",
                    adSoyad, email, rolId, aktif, id);
        } else {
            jdbcTemplate.update(
                    "UPDATE kullanici SET ad_soyad = ?, email = ?, rol_id = ?, aktif = ?, sifre_hash = ? WHERE id = ?",
                    adSoyad, email, rolId, aktif, yeniSifreHashYadaNull, id);
        }
    }

    /** FK ihlali varsa (iliskili islem/alarm kaydi) DataIntegrityViolationException firlatir. */
    @Transactional
    public void kalicaSil(int id, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("DELETE FROM kullanici WHERE id = ?", id);
    }

    @Transactional
    public void pasifYap(int id, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("UPDATE kullanici SET aktif = false WHERE id = ?", id);
    }
}
