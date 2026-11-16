package com.fraud.detector.model;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class KuralRowMapper implements RowMapper<Kural> {

    @Override
    public Kural mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Kural(
                rs.getInt("id"),
                rs.getString("kod"),
                rs.getString("ad"),
                rs.getInt("puan"),
                rs.getBigDecimal("esik"),
                rs.getBoolean("aktif")
        );
    }
}
