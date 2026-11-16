package com.fraud.detector.model;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/** isyeri JOIN kategori JOIN sehir sorgusunda kullanilir. */
public class IsyeriRowMapper implements RowMapper<Isyeri> {

    @Override
    public Isyeri mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Isyeri(
                rs.getInt("id"),
                rs.getString("ad"),
                rs.getInt("kategori_id"),
                rs.getString("kategori_adi"),
                rs.getInt("sehir_id"),
                rs.getString("sehir_adi")
        );
    }
}
