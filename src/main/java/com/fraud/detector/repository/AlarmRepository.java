package com.fraud.detector.repository;

import com.fraud.detector.model.AlarmAciklama;
import com.fraud.detector.model.AlarmDetay;
import com.fraud.detector.model.AlarmOzet;
import com.fraud.detector.model.BekleyenOnay;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class AlarmRepository {

    private static final String ANALIST_TEMEL_SORGU = """
            SELECT a.id, i.tarih, y.ad AS isyeri_adi, k.son4 AS kart_son4, u.ad_soyad AS sahip,
                   i.tutar, a.toplam_puan, a.durum, a.musteri_teyit, h.id AS hesap_id, u.id AS kullanici_id
            FROM alarm a
            JOIN islem i ON i.id = a.islem_id
            JOIN kart k ON k.id = i.kart_id
            JOIN hesap h ON h.id = k.hesap_id
            JOIN kullanici u ON u.id = h.kullanici_id
            JOIN isyeri y ON y.id = i.isyeri_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public AlarmRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Bekleyenler ustte, aralarinda toplam puana gore azalan sirada. */
    public List<AlarmOzet> listele(String durumFiltre) {
        String sql = ANALIST_TEMEL_SORGU
                + " WHERE (?::varchar IS NULL OR a.durum = ?)"
                + " ORDER BY (a.durum = 'BEKLIYOR') DESC, a.toplam_puan DESC";
        return jdbcTemplate.query(sql, (rs, n) -> new AlarmOzet(
                rs.getInt("id"),
                rs.getTimestamp("tarih").toLocalDateTime(),
                rs.getString("isyeri_adi"),
                rs.getString("sahip"),
                rs.getBigDecimal("tutar"),
                rs.getInt("toplam_puan"),
                rs.getString("durum"),
                (Boolean) rs.getObject("musteri_teyit")),
                durumFiltre, durumFiltre);
    }

    public Optional<AlarmDetay> bul(int alarmId) {
        return jdbcTemplate.query(ANALIST_TEMEL_SORGU + " WHERE a.id = ?",
                (rs, n) -> new AlarmDetay(
                        rs.getInt("id"),
                        rs.getTimestamp("tarih").toLocalDateTime(),
                        rs.getString("isyeri_adi"),
                        rs.getString("kart_son4"),
                        rs.getString("sahip"),
                        rs.getBigDecimal("tutar"),
                        rs.getInt("toplam_puan"),
                        rs.getString("durum"),
                        (Boolean) rs.getObject("musteri_teyit"),
                        rs.getInt("hesap_id"),
                        rs.getInt("kullanici_id")),
                alarmId).stream().findFirst();
    }

    public List<AlarmAciklama> aciklama(int alarmId) {
        return jdbcTemplate.query("SELECT * FROM fn_alarm_aciklama(?)",
                (rs, n) -> new AlarmAciklama(
                        rs.getString("kural"), rs.getInt("puan"), rs.getString("aciklama")),
                alarmId);
    }

    /** sp_alarm_karar kendi icinde app.kullanici_id'yi set eder ve yetki/durum kontrolu yapar. */
    @Transactional
    public void karar(int alarmId, int analistId, String karar) {
        jdbcTemplate.update("CALL sp_alarm_karar(?, ?, ?)", alarmId, analistId, karar);
    }

    /**
     * bildirim tablosunda alarm_id yok; liste dogrudan alarm tablosundan,
     * musterinin kendi kartlarina ait VE hala BEKLIYOR durumunda olanlar icin
     * cekilir. Analist alarmi once kapatirsa (durum artik BEKLIYOR degil)
     * bu sorgu onu otomatik olarak listeden cikarir.
     */
    public List<BekleyenOnay> bekleyenler(int kullaniciId) {
        return jdbcTemplate.query("""
                SELECT a.id, i.tarih, y.ad AS isyeri_adi, i.tutar
                FROM alarm a
                JOIN islem i ON i.id = a.islem_id
                JOIN kart k ON k.id = i.kart_id
                JOIN hesap h ON h.id = k.hesap_id
                JOIN isyeri y ON y.id = i.isyeri_id
                WHERE h.kullanici_id = ? AND a.durum = 'BEKLIYOR'
                ORDER BY a.olusturma DESC
                """,
                (rs, n) -> new BekleyenOnay(
                        rs.getInt("id"),
                        rs.getTimestamp("tarih").toLocalDateTime(),
                        rs.getString("isyeri_adi"),
                        rs.getBigDecimal("tutar")),
                kullaniciId);
    }

    /**
     * sp_musteri_teyit kendi icinde hem sahiplik+durum kontrolu yapar (eszamanli
     * olarak analist once kapatmissa anlasilir RAISE EXCEPTION verir) hem de
     * app.kullanici_id'yi kendisi set eder — Java'da ekstra kontrol/SET gerekmez.
     */
    @Transactional
    public void teyitEt(int alarmId, int kullaniciId, boolean onay) {
        jdbcTemplate.update("CALL sp_musteri_teyit(?, ?, ?)", alarmId, kullaniciId, onay);
    }
}
