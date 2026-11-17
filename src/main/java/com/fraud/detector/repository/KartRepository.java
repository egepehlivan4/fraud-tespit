package com.fraud.detector.repository;

import com.fraud.detector.common.AktifKullaniciBildirici;
import com.fraud.detector.model.HesapSecenek;
import com.fraud.detector.model.Kart;
import com.fraud.detector.model.KartRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class KartRepository {

    private static final String TEMEL_SORGU = """
            SELECT k.id, k.hesap_id, h.iban, u.ad_soyad AS sahip, k.son4, k.durum, k.olusturma
            FROM kart k JOIN hesap h ON h.id = k.hesap_id JOIN kullanici u ON u.id = h.kullanici_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AktifKullaniciBildirici aktifKullanici;

    public KartRepository(JdbcTemplate jdbcTemplate, AktifKullaniciBildirici aktifKullanici) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktifKullanici = aktifKullanici;
    }

    public List<HesapSecenek> hesapSecenekleri() {
        return jdbcTemplate.query("""
                SELECT h.id, h.iban, u.ad_soyad AS sahip
                FROM hesap h JOIN kullanici u ON u.id = h.kullanici_id
                ORDER BY u.ad_soyad
                """,
                (rs, n) -> new HesapSecenek(rs.getInt("id"), rs.getString("iban"), rs.getString("sahip")));
    }

    /** Musteri'nin kendi kartlarim ekraninda kullanilir. */
    public List<Kart> listeleKullanici(int kullaniciId) {
        return jdbcTemplate.query(TEMEL_SORGU + " WHERE h.kullanici_id = ? ORDER BY k.id",
                new KartRowMapper(), kullaniciId);
    }

    /** durumFiltre null ise hepsi gelir. */
    public List<Kart> listele(String durumFiltre) {
        String sql = TEMEL_SORGU + " WHERE (?::varchar IS NULL OR k.durum = ?) ORDER BY k.id";
        return jdbcTemplate.query(sql, new KartRowMapper(), durumFiltre, durumFiltre);
    }

    public Optional<String> durumBul(int kartId) {
        return jdbcTemplate.query("SELECT durum FROM kart WHERE id = ?",
                        (rs, n) -> rs.getString("durum"), kartId)
                .stream().findFirst();
    }

    @Transactional
    public void ekle(int hesapId, String son4, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("INSERT INTO kart(hesap_id, son4) VALUES (?, ?)", hesapId, son4);
    }

    /** AKTIF<->BLOKE/IPTAL gecisleri icin duz UPDATE (bu gecisler icin procedure yok). */
    @Transactional
    public void durumGuncelle(int kartId, String yeniDurum, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("UPDATE kart SET durum = ? WHERE id = ?", yeniDurum, kartId);
    }

    /**
     * BLOKE -> AKTIF gecisi SADECE sp_kart_aktif_et ile yapilir (Java'da UPDATE yazilmaz).
     * Procedure zaten app.kullanici_id'yi kendisi set ediyor ve sadece ADMIN'e izin veriyor;
     * ayrica WHERE durum = 'BLOKE' korumasi sayesinde IPTAL kart hicbir sekilde AKTIF olmaz.
     */
    @Transactional
    public void aktifEt(int kartId, int adminId) {
        jdbcTemplate.update("CALL sp_kart_aktif_et(?, ?)", kartId, adminId);
    }
}
