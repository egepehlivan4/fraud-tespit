package com.fraud.detector.service;

import com.fraud.detector.common.IsKuraliIhlaliException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Harcama Yap ekranindaki kart_id formdan geliyor; dropdown'da sadece
 * kullanicinin kendi kartlari gorunse de, bir kullanici formu elle degistirip
 * BASKA BIR MUSTERININ kart_id'sini gonderebilir. Bu testin amaci: sunucu
 * tarafindaki sahiplik kontrolunun (sadece arayuzdeki dropdown'un degil)
 * bu durumu gercekten reddettigini kanitlamak.
 */
@SpringBootTest
@Transactional
class MusteriIslemServiceTest {

    private static final int AYSE_KART_ID = 1;     // hesap 1 -> kullanici 3 (Ayşe)
    private static final int MEHMET_KULLANICI_ID = 4;
    private static final int ISYERI_ID = 1;

    @Autowired
    private MusteriIslemService musteriIslemService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void baskasininKartIdSiyleHarcamaYapmayaCalisinca_sunucuTarafindaReddedilirVeIslemEklenmez() {
        long oncekiIslemSayisi = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM islem WHERE kart_id = ?", Long.class, AYSE_KART_ID);

        assertThatThrownBy(() ->
                musteriIslemService.harcamaYap(AYSE_KART_ID, ISYERI_ID,
                        new BigDecimal("100.00"), MEHMET_KULLANICI_ID))
                .isInstanceOf(IsKuraliIhlaliException.class)
                .hasMessage("Bu kart size ait değil.");

        long sonrakiIslemSayisi = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM islem WHERE kart_id = ?", Long.class, AYSE_KART_ID);
        assertThat(sonrakiIslemSayisi).isEqualTo(oncekiIslemSayisi);
    }

    @Test
    void kendiKartiylaHarcamaYapabilirVeIslemEklenir() {
        long oncekiIslemSayisi = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM islem WHERE kart_id = ?", Long.class, AYSE_KART_ID);

        musteriIslemService.harcamaYap(AYSE_KART_ID, ISYERI_ID, new BigDecimal("50.00"), 3);

        long sonrakiIslemSayisi = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM islem WHERE kart_id = ?", Long.class, AYSE_KART_ID);
        assertThat(sonrakiIslemSayisi).isEqualTo(oncekiIslemSayisi + 1);
    }
}
