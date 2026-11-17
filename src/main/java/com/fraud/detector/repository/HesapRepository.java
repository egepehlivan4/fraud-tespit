package com.fraud.detector.repository;

import com.fraud.detector.model.HesapOzet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class HesapRepository {

    private final JdbcTemplate jdbcTemplate;

    public HesapRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HesapOzet> listeleKullanici(int kullaniciId) {
        return jdbcTemplate.query(
                "SELECT id, iban, bakiye FROM hesap WHERE kullanici_id = ? ORDER BY id",
                (rs, n) -> new HesapOzet(rs.getInt("id"), rs.getString("iban"), rs.getBigDecimal("bakiye")),
                kullaniciId);
    }
}
