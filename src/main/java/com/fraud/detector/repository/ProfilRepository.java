package com.fraud.detector.repository;

import com.fraud.detector.common.AktifKullaniciBildirici;
import com.fraud.detector.model.HarcamaGrafikNokta;
import com.fraud.detector.model.ModelOzet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class ProfilRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AktifKullaniciBildirici aktifKullanici;

    public ProfilRepository(JdbcTemplate jdbcTemplate, AktifKullaniciBildirici aktifKullanici) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktifKullanici = aktifKullanici;
    }

    public List<HarcamaGrafikNokta> grafik(int hesapId) {
        return jdbcTemplate.query("SELECT * FROM fn_harcama_grafik(?)",
                (rs, n) -> new HarcamaGrafikNokta(
                        rs.getDate("ay").toLocalDate(),
                        rs.getBigDecimal("gercek"),
                        rs.getBigDecimal("beklenen"),
                        rs.getBigDecimal("ust_sinir")),
                hesapId);
    }

    public BigDecimal ustSinir(int hesapId) {
        return jdbcTemplate.queryForObject("SELECT fn_ust_sinir(?)", BigDecimal.class, hesapId);
    }

    /** Admin - Model Yonetimi ekraninda tum hesaplarin ogrenilmis profilini gosterir. */
    public List<ModelOzet> hepsi() {
        return jdbcTemplate.query("""
                SELECT h.id AS hesap_id, u.ad_soyad AS sahip, h.iban,
                       p.ortalama, p.varyans, p.egitilen_ay_sayisi, p.son_ay
                FROM hesap h JOIN kullanici u ON u.id = h.kullanici_id
                JOIN harcama_profili p ON p.hesap_id = h.id
                ORDER BY h.id
                """,
                (rs, n) -> new ModelOzet(
                        rs.getInt("hesap_id"),
                        rs.getString("sahip"),
                        rs.getString("iban"),
                        rs.getBigDecimal("ortalama"),
                        rs.getBigDecimal("varyans"),
                        rs.getInt("egitilen_ay_sayisi"),
                        rs.getDate("son_ay") != null ? rs.getDate("son_ay").toLocalDate() : null));
    }

    @Transactional
    public void yenidenEgit(int hesapId, int aktifKullaniciId) {
        aktifKullanici.ayarla(aktifKullaniciId);
        jdbcTemplate.update("CALL sp_profil_yeniden_egit(?)", hesapId);
    }
}
