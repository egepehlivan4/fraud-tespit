package com.fraud.detector.config;

import com.fraud.detector.security.RoleBasedSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Seed veride sifreler "{noop}1234" seklinde duz metin olarak hazir (db/fraud_db.sql).
     * DelegatingPasswordEncoder bu onek sayesinde onlari degistirmeden dogrular;
     * yeni eklenen/guncellenen kullanicilarin sifreleri ileride "{bcrypt}" oneki ile
     * hashlenerek kaydedilecek. Sema/seed veriye dokunulmuyor.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                .requestMatchers("/musteri/**").hasRole("MUSTERI")
                .requestMatchers("/analist/**").hasRole("ANALIST")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(new RoleBasedSuccessHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?cikis")
                .permitAll()
            );
        return http.build();
    }
}
