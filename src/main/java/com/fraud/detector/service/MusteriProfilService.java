package com.fraud.detector.service;

import com.fraud.detector.model.HarcamaGrafikNokta;
import com.fraud.detector.model.HesapOzet;
import com.fraud.detector.model.MusteriAyar;
import com.fraud.detector.repository.HesapRepository;
import com.fraud.detector.repository.MusteriAyarRepository;
import com.fraud.detector.repository.ProfilRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MusteriProfilService {

    private static final MusteriAyar VARSAYILAN_AYAR = new MusteriAyar(new BigDecimal("20"), false, true);

    private final ProfilRepository profilRepository;
    private final MusteriAyarRepository musteriAyarRepository;
    private final HesapRepository hesapRepository;

    public MusteriProfilService(ProfilRepository profilRepository, MusteriAyarRepository musteriAyarRepository,
                                 HesapRepository hesapRepository) {
        this.profilRepository = profilRepository;
        this.musteriAyarRepository = musteriAyarRepository;
        this.hesapRepository = hesapRepository;
    }

    public List<HesapOzet> hesaplarim(int kullaniciId) {
        return hesapRepository.listeleKullanici(kullaniciId);
    }

    public List<HarcamaGrafikNokta> grafik(int hesapId) {
        return profilRepository.grafik(hesapId);
    }

    public BigDecimal ustSinir(int hesapId) {
        return profilRepository.ustSinir(hesapId);
    }

    public MusteriAyar ayarGetir(int kullaniciId) {
        return musteriAyarRepository.bul(kullaniciId).orElse(VARSAYILAN_AYAR);
    }

    public void ayarGuncelle(int kullaniciId, BigDecimal tolerans, boolean otomatikTolerans,
                              boolean bildirimAktif) {
        musteriAyarRepository.guncelle(kullaniciId, tolerans, otomatikTolerans, bildirimAktif, kullaniciId);
    }
}
