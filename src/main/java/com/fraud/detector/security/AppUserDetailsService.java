package com.fraud.detector.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Giris sirasinda kullanici+rol tablosunu okur. Sema/is mantigi veritabaninda
 * kaldigi icin burada sadece SELECT var, baska bir kural uygulanmiyor.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public AppUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String sql = """
                SELECT k.id, k.ad_soyad, k.email, k.sifre_hash, k.aktif, r.ad AS rol_adi
                FROM kullanici k JOIN rol r ON r.id = k.rol_id
                WHERE k.email = ?
                """;

        List<AppUserPrincipal> sonuc = jdbcTemplate.query(sql, (rs, rowNum) ->
                new AppUserPrincipal(
                        rs.getInt("id"),
                        rs.getString("ad_soyad"),
                        rs.getString("email"),
                        rs.getString("sifre_hash"),
                        rs.getString("rol_adi"),
                        rs.getBoolean("aktif")
                ), email);

        if (sonuc.isEmpty()) {
            throw new UsernameNotFoundException("Kullanici bulunamadi: " + email);
        }
        return sonuc.get(0);
    }
}
