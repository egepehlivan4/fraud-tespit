package com.fraud.detector.model;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/** kullanici JOIN rol sorgusunda kullanilir (sonucta rol_adi kolonu olmali). */
public class KullaniciRowMapper implements RowMapper<Kullanici> {

    @Override
    public Kullanici mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Kullanici(
                rs.getInt("id"),
                rs.getString("ad_soyad"),
                rs.getString("email"),
                rs.getString("sifre_hash"),
                rs.getInt("rol_id"),
                rs.getString("rol_adi"),
                rs.getBoolean("aktif"),
                rs.getTimestamp("olusturma").toLocalDateTime()
        );
    }
}
