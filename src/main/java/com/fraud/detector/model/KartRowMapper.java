package com.fraud.detector.model;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class KartRowMapper implements RowMapper<Kart> {

    @Override
    public Kart mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Kart(
                rs.getInt("id"),
                rs.getInt("hesap_id"),
                rs.getString("iban"),
                rs.getString("sahip"),
                rs.getString("son4"),
                rs.getString("durum"),
                rs.getTimestamp("olusturma").toLocalDateTime()
        );
    }
}
