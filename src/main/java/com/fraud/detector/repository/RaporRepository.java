package com.fraud.detector.repository;

import com.fraud.detector.model.FraudHalkasiSonuc;
import com.fraud.detector.model.SehirKategoriRaporSatiri;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class RaporRepository {

    private final JdbcTemplate jdbcTemplate;

    public RaporRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SehirKategoriRaporSatiri> sehirKategori(LocalDate bas, LocalDate bit) {
        return jdbcTemplate.query("SELECT * FROM fn_rapor_sehir_kategori(?, ?)",
                (rs, n) -> new SehirKategoriRaporSatiri(
                        rs.getString("sehir"),
                        rs.getString("kategori"),
                        rs.getLong("islem_sayisi"),
                        rs.getLong("alarm_sayisi"),
                        rs.getLong("fraud_sayisi"),
                        rs.getBigDecimal("alarm_orani")),
                bas, bit);
    }

    public List<FraudHalkasiSonuc> fraudHalkasi(int hesapId) {
        return jdbcTemplate.query("SELECT * FROM fn_fraud_halkasi(?)",
                (rs, n) -> new FraudHalkasiSonuc(
                        rs.getInt("hesap_id"), rs.getString("sahibi"), rs.getInt("derinlik")),
                hesapId);
    }
}
