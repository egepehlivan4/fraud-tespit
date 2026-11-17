package com.fraud.detector.repository;

import com.fraud.detector.model.DenetimLogSatiri;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DenetimLogRepository {

    private static final String TEMEL_SORGU = """
            SELECT d.id, d.tablo, d.kayit_id, d.islem_tipi, d.eski::text AS eski_json,
                   d.yeni::text AS yeni_json, d.kullanici_id, u.ad_soyad AS kullanici_adi, d.zaman
            FROM denetim_log d LEFT JOIN kullanici u ON u.id = d.kullanici_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public DenetimLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DenetimLogSatiri> listele(String tabloFiltre, int limit, int offset) {
        String sql = TEMEL_SORGU
                + " WHERE (?::varchar IS NULL OR d.tablo = ?) ORDER BY d.id DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, (rs, n) -> new DenetimLogSatiri(
                        rs.getLong("id"),
                        rs.getString("tablo"),
                        (Long) rs.getObject("kayit_id"),
                        rs.getString("islem_tipi"),
                        rs.getString("eski_json"),
                        rs.getString("yeni_json"),
                        (Integer) rs.getObject("kullanici_id"),
                        rs.getString("kullanici_adi"),
                        rs.getTimestamp("zaman").toLocalDateTime()),
                tabloFiltre, tabloFiltre, limit, offset);
    }

    public int sayisi(String tabloFiltre) {
        Integer sonuc = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM denetim_log WHERE (?::varchar IS NULL OR tablo = ?)",
                Integer.class, tabloFiltre, tabloFiltre);
        return sonuc == null ? 0 : sonuc;
    }
}
