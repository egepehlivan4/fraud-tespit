package com.fraud.detector.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Giris basarili olunca kullaniciyi rolune gore farkli ana sayfaya yonlendirir.
 * Hoca isteri: 3 kullanici tipi (MUSTERI, ANALIST, ADMIN) icin ayri ekranlar.
 */
public class RoleBasedSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        String hedef = "/login";
        for (GrantedAuthority yetki : authentication.getAuthorities()) {
            switch (yetki.getAuthority()) {
                case "ROLE_ADMIN"   -> hedef = "/admin";
                case "ROLE_ANALIST" -> hedef = "/analist";
                case "ROLE_MUSTERI" -> hedef = "/musteri";
                default -> { /* bilinmeyen rol - yonlendirme degismez */ }
            }
        }
        response.sendRedirect(hedef);
    }
}
