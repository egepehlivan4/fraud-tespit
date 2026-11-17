package com.fraud.detector.service;

import com.fraud.detector.model.Kural;
import com.fraud.detector.repository.KuralRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class KuralService {

    /** Trigger'in (fn_islem_risk) tanidigi 5 sabit tip — formda dropdown olarak sunulur. */
    private static final List<String> TIPLER =
            List.of("GECE", "YUKSEK_TUTAR", "YENI_ISYERI", "HIZ", "IMKANSIZ_SEYAHAT");

    private final KuralRepository kuralRepository;

    public KuralService(KuralRepository kuralRepository) {
        this.kuralRepository = kuralRepository;
    }

    public List<String> tipler() {
        return TIPLER;
    }

    public List<Kural> listele() {
        return kuralRepository.listele();
    }

    public Optional<Kural> bul(int id) {
        return kuralRepository.bul(id);
    }

    public void ekle(String kod, String ad, int puan, BigDecimal esik, int aktifKullaniciId) {
        kuralRepository.ekle(kod, ad, puan, esik, aktifKullaniciId);
    }

    public void guncelle(int id, String ad, int puan, BigDecimal esik, boolean aktif, int aktifKullaniciId) {
        kuralRepository.guncelle(id, ad, puan, esik, aktif, aktifKullaniciId);
    }

    /**
     * Once kalici silme denenir (kalicaSil ayri bir transaction'da calisir).
     * kural_ihlal kaydi varsa FK ihlaliyle duser; bu durumda TAMAMEN YENI bir
     * transaction'da pasifYap cagrilir (ayni mantik KullaniciService.sil ile).
     *
     * @return true: tamamen silindi, false: FK nedeniyle pasife alindi
     */
    public boolean sil(int id, int aktifKullaniciId) {
        try {
            kuralRepository.kalicaSil(id, aktifKullaniciId);
            return true;
        } catch (DataIntegrityViolationException e) {
            kuralRepository.pasifYap(id, aktifKullaniciId);
            return false;
        }
    }

    public BigDecimal alarmEsikGetir() {
        return kuralRepository.alarmEsikGetir();
    }

    public void alarmEsikGuncelle(BigDecimal deger, int aktifKullaniciId) {
        kuralRepository.alarmEsikGuncelle(deger, aktifKullaniciId);
    }
}
