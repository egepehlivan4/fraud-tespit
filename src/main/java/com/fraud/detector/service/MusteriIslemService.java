package com.fraud.detector.service;

import com.fraud.detector.common.IsKuraliIhlaliException;
import com.fraud.detector.model.HesapOzet;
import com.fraud.detector.model.IslemOzet;
import com.fraud.detector.model.Kart;
import com.fraud.detector.repository.HesapRepository;
import com.fraud.detector.repository.IslemRepository;
import com.fraud.detector.repository.KartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MusteriIslemService {

    private static final int SAYFA_BOYUTU = 20;

    private final IslemRepository islemRepository;
    private final KartRepository kartRepository;
    private final HesapRepository hesapRepository;

    public MusteriIslemService(IslemRepository islemRepository, KartRepository kartRepository,
                                HesapRepository hesapRepository) {
        this.islemRepository = islemRepository;
        this.kartRepository = kartRepository;
        this.hesapRepository = hesapRepository;
    }

    public List<Kart> kartlarim(int kullaniciId) {
        return kartRepository.listeleKullanici(kullaniciId);
    }

    public List<HesapOzet> hesaplarim(int kullaniciId) {
        return hesapRepository.listeleKullanici(kullaniciId);
    }

    /**
     * GUVENLIK: kartId formdan geliyor — dropdown'da yalniz kendi kartlarini
     * gorse de, kullanici formu elle degistirip baska birinin kart_id'sini
     * gonderebilir. Bu yuzden sahiplik HER ZAMAN burada, sunucu tarafinda
     * dogrulanir (bkz. MusteriIslemServiceTest).
     *
     * @return true: alarm acildi (guvenlik kontrolune alindi), false: normal onay
     */
    public boolean harcamaYap(int kartId, int isyeriId, BigDecimal tutar, int kullaniciId) {
        if (!islemRepository.kartKullaniciyaAitMi(kartId, kullaniciId)) {
            throw new IsKuraliIhlaliException("Bu kart size ait değil.");
        }
        Integer cihazId = islemRepository.enSikCihaz(kartId).orElse(null);
        long islemId = islemRepository.ekle(kartId, isyeriId, cihazId, tutar, kullaniciId);
        return islemRepository.alarmVarMi(islemId);
    }

    /**
     * kartId filtresi her ne olursa olsun sorgu h.kullanici_id = kullaniciId ile
     * sinirlandigi icin baska birinin kartiyla filtrelemeye calismak sadece
     * bos sonuc doner — veri sizintisi olmaz, ekstra kontrole gerek yok.
     */
    public List<IslemOzet> gecmis(int kullaniciId, Integer kartId, LocalDate bas, LocalDate bit, int sayfa) {
        int gecerliSayfa = Math.max(sayfa, 1);
        return islemRepository.gecmis(kullaniciId, kartId, bas, bit,
                SAYFA_BOYUTU, (gecerliSayfa - 1) * SAYFA_BOYUTU);
    }

    public int gecmisToplamSayfa(int kullaniciId, Integer kartId, LocalDate bas, LocalDate bit) {
        int toplamSatir = islemRepository.gecmisSayisi(kullaniciId, kartId, bas, bit);
        return Math.max(1, (int) Math.ceil(toplamSatir / (double) SAYFA_BOYUTU));
    }
}
