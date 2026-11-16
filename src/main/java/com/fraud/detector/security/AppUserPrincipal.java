package com.fraud.detector.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * Spring Security'nin kullandigi UserDetails karsiligi.
 * kullanici.id'yi tasir ki her islemden once "SET app.kullanici_id" ile
 * veritabanina aktif kullaniciyi bildirebilelim (CLAUDE.md kurali).
 */
public class AppUserPrincipal implements UserDetails {

    private final int id;
    private final String adSoyad;
    private final String email;
    private final String sifreHash;
    private final String rolAdi;      // MUSTERI, ANALIST, ADMIN
    private final boolean aktif;

    public AppUserPrincipal(int id, String adSoyad, String email, String sifreHash,
                             String rolAdi, boolean aktif) {
        this.id = id;
        this.adSoyad = adSoyad;
        this.email = email;
        this.sifreHash = sifreHash;
        this.rolAdi = rolAdi;
        this.aktif = aktif;
    }

    public int getId() {
        return id;
    }

    public String getAdSoyad() {
        return adSoyad;
    }

    public String getRolAdi() {
        return rolAdi;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rolAdi));
    }

    @Override
    public String getPassword() {
        return sifreHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return aktif;
    }
}
