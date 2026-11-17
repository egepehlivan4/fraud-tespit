package com.fraud.detector.repository;

import com.fraud.detector.common.AktifKullaniciBildirici;
import com.fraud.detector.model.Bildirim;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class BildirimRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AktifKullaniciBildirici aktifKullanici;

    public BildirimRepository(JdbcTemplate jdbcTemplate, AktifKullaniciBildirici aktifKullanici) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktifKullanici = aktifKullanici;
    }

    public List<Bildirim> listele(int kullaniciId) {
        return jdbcTemplate.query("""
                SELECT id, tip, mesaj, okundu, olusturma FROM bildirim
                WHERE kullanici_id = ? ORDER BY olusturma DESC
                """,
                (rs, n) -> new Bildirim(
                        rs.getInt("id"),
                        rs.getString("tip"),
                        rs.getString("mesaj"),
                        rs.getBoolean("okundu"),
                        rs.getTimestamp("olusturma").toLocalDateTime()),
                kullaniciId);
    }

    public int okunmamisSayisi(int kullaniciId) {
        Integer sonuc = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bildirim WHERE kullanici_id = ? AND NOT okundu",
                Integer.class, kullaniciId);
        return sonuc == null ? 0 : sonuc;
    }

    /** kullanici_id = ? sarti sayesinde baskasinin bildirimi isaretlenemez. */
    @Transactional
    public void okunduIsaretle(int bildirimId, int kullaniciId, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update(
                "UPDATE bildirim SET okundu = true WHERE id = ? AND kullanici_id = ?",
                bildirimId, kullaniciId);
    }
}
