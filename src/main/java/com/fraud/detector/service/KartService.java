package com.fraud.detector.service;

import com.fraud.detector.common.IsKuraliIhlaliException;
import com.fraud.detector.model.HesapSecenek;
import com.fraud.detector.model.Kart;
import com.fraud.detector.repository.KartRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KartService {

    private final KartRepository kartRepository;

    public KartService(KartRepository kartRepository) {
        this.kartRepository = kartRepository;
    }

    public List<Kart> listele(String durumFiltre) {
        return kartRepository.listele(durumFiltre);
    }

    public List<HesapSecenek> hesapSecenekleri() {
        return kartRepository.hesapSecenekleri();
    }

    public void ekle(int hesapId, String son4, int aktifKullaniciId) {
        if (son4 == null || !son4.matches("\\d{4}")) {
            throw new IsKuraliIhlaliException("Kartın son 4 hanesi tam olarak 4 rakam olmalı.");
        }
        kartRepository.ekle(hesapId, son4, aktifKullaniciId);
    }

    /**
     * IPTAL kalicidir — hicbir sekilde durum degistirilemez.
     * AKTIF'e donus SADECE BLOKE karttan, SADECE sp_kart_aktif_et ile yapilir.
     */
    public void durumDegistir(int kartId, String yeniDurum, int aktifKullaniciId) {
        String mevcutDurum = kartRepository.durumBul(kartId)
                .orElseThrow(() -> new IsKuraliIhlaliException("Kart bulunamadı."));

        if ("IPTAL".equals(mevcutDurum)) {
            throw new IsKuraliIhlaliException("İptal edilmiş bir kartın durumu değiştirilemez.");
        }

        if ("AKTIF".equals(yeniDurum)) {
            if (!"BLOKE".equals(mevcutDurum)) {
                throw new IsKuraliIhlaliException("Sadece BLOKE durumundaki kart AKTIF yapılabilir.");
            }
            kartRepository.aktifEt(kartId, aktifKullaniciId);
        } else {
            kartRepository.durumGuncelle(kartId, yeniDurum, aktifKullaniciId);
        }
    }
}
