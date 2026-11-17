package com.fraud.detector.repository;

import com.fraud.detector.common.AktifKullaniciBildirici;
import com.fraud.detector.model.AlarmAciklama;
import com.fraud.detector.model.CihazSecenek;
import com.fraud.detector.model.IslemOzet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class IslemRepository {

    private static final String GECMIS_WHERE = """
            FROM islem i
            JOIN kart k ON k.id = i.kart_id
            JOIN hesap h ON h.id = k.hesap_id
            JOIN isyeri y ON y.id = i.isyeri_id
            WHERE h.kullanici_id = ?
              AND (?::int IS NULL OR k.id = ?)
              AND (?::date IS NULL OR i.tarih::date >= ?)
              AND (?::date IS NULL OR i.tarih::date <= ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AktifKullaniciBildirici aktifKullanici;

    public IslemRepository(JdbcTemplate jdbcTemplate, AktifKullaniciBildirici aktifKullanici) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktifKullanici = aktifKullanici;
    }

    /** Guvenlik siniri: kart_id formda elle degistirilse bile gercek sahiplik burada dogrulanir. */
    public boolean kartKullaniciyaAitMi(int kartId, int kullaniciId) {
        Boolean sonuc = jdbcTemplate.queryForObject("""
                SELECT EXISTS(
                    SELECT 1 FROM kart k JOIN hesap h ON h.id = k.hesap_id
                    WHERE k.id = ? AND h.kullanici_id = ?
                )
                """, Boolean.class, kartId, kullaniciId);
        return Boolean.TRUE.equals(sonuc);
    }

    /** Semada kullanici-cihaz iliskisi yok; kartin gecmiste en sik kullandigi cihaz alinir. */
    public Optional<Integer> enSikCihaz(int kartId) {
        return jdbcTemplate.query("""
                SELECT cihaz_id FROM islem WHERE kart_id = ? AND cihaz_id IS NOT NULL
                GROUP BY cihaz_id ORDER BY COUNT(*) DESC LIMIT 1
                """, (rs, n) -> rs.getInt("cihaz_id"), kartId).stream().findFirst();
    }

    @Transactional
    public long ekle(int kartId, int isyeriId, Integer cihazId, BigDecimal tutar, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        KeyHolder anahtar = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO islem(kart_id, isyeri_id, cihaz_id, tutar) VALUES (?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setInt(1, kartId);
            ps.setInt(2, isyeriId);
            if (cihazId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, cihazId);
            }
            ps.setBigDecimal(4, tutar);
            return ps;
        }, anahtar);
        return anahtar.getKey().longValue();
    }

    public boolean alarmVarMi(long islemId) {
        Boolean sonuc = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM alarm WHERE islem_id = ?)", Boolean.class, islemId);
        return Boolean.TRUE.equals(sonuc);
    }

    /** Hangi kurallarin kac puan verdigini gosterir (alarm esigine ulasmasa bile). */
    public List<AlarmAciklama> kuralIhlalleri(long islemId) {
        return jdbcTemplate.query("""
                SELECT k.ad AS kural, ki.puan, ki.aciklama
                FROM kural_ihlal ki JOIN kural k ON k.id = ki.kural_id
                WHERE ki.islem_id = ? ORDER BY ki.puan DESC
                """,
                (rs, n) -> new AlarmAciklama(rs.getString("kural"), rs.getInt("puan"), rs.getString("aciklama")),
                islemId);
    }

    public int riskPuani(long islemId) {
        Integer sonuc = jdbcTemplate.queryForObject(
                "SELECT risk_puani FROM islem WHERE id = ?", Integer.class, islemId);
        return sonuc == null ? 0 : sonuc;
    }

    /** Kartin son kullandigi isyeri — hazir senaryo butonlari icin varsayilan secim. */
    public Optional<Integer> sonIsyeri(int kartId) {
        return jdbcTemplate.query(
                "SELECT isyeri_id FROM islem WHERE kart_id = ? ORDER BY tarih DESC LIMIT 1",
                (rs, n) -> rs.getInt("isyeri_id"), kartId).stream().findFirst();
    }

    public List<CihazSecenek> cihazSecenekleri() {
        return jdbcTemplate.query("SELECT id, tanim FROM cihaz ORDER BY tanim",
                (rs, n) -> new CihazSecenek(rs.getInt("id"), rs.getString("tanim")));
    }

    /**
     * Admin Islem Simulatoru icin — musterinin "harcama yap" ekranindan farkli
     * olarak tarih elle secilebilir (Gece/Hiz/Imkansiz Seyahat senaryolarini
     * istenilen anda tetiklemek icin).
     */
    @Transactional
    public long ekleSimulasyon(int kartId, int isyeriId, Integer cihazId, BigDecimal tutar,
                                LocalDateTime tarih, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        KeyHolder anahtar = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO islem(kart_id, isyeri_id, cihaz_id, tutar, tarih) VALUES (?, ?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setInt(1, kartId);
            ps.setInt(2, isyeriId);
            if (cihazId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, cihazId);
            }
            ps.setBigDecimal(4, tutar);
            ps.setTimestamp(5, Timestamp.valueOf(tarih));
            return ps;
        }, anahtar);
        return anahtar.getKey().longValue();
    }

    public List<IslemOzet> gecmis(int kullaniciId, Integer kartId, LocalDate bas, LocalDate bit,
                                   int limit, int offset) {
        String sql = "SELECT i.id, i.tarih, y.ad AS isyeri_adi, i.tutar, i.durum, k.son4 "
                + GECMIS_WHERE + " ORDER BY i.tarih DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql,
                (rs, n) -> new IslemOzet(
                        rs.getLong("id"),
                        rs.getTimestamp("tarih").toLocalDateTime(),
                        rs.getString("isyeri_adi"),
                        rs.getBigDecimal("tutar"),
                        rs.getString("durum"),
                        rs.getString("son4")),
                kullaniciId, kartId, kartId, bas, bas, bit, bit, limit, offset);
    }

    public int gecmisSayisi(int kullaniciId, Integer kartId, LocalDate bas, LocalDate bit) {
        String sql = "SELECT count(*) " + GECMIS_WHERE;
        Integer sonuc = jdbcTemplate.queryForObject(sql, Integer.class,
                kullaniciId, kartId, kartId, bas, bas, bit, bit);
        return sonuc == null ? 0 : sonuc;
    }
}
