package com.fraud.detector.service;

import com.fraud.detector.model.Isyeri;
import com.fraud.detector.model.Kategori;
import com.fraud.detector.model.Sehir;
import com.fraud.detector.repository.IsyeriRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IsyeriService {

    private final IsyeriRepository isyeriRepository;

    public IsyeriService(IsyeriRepository isyeriRepository) {
        this.isyeriRepository = isyeriRepository;
    }

    public List<Isyeri> listele() {
        return isyeriRepository.listele();
    }

    public List<Kategori> kategoriler() {
        return isyeriRepository.kategoriler();
    }

    public List<Sehir> sehirler() {
        return isyeriRepository.sehirler();
    }

    public Optional<Isyeri> bul(int id) {
        return isyeriRepository.bul(id);
    }

    public void ekle(String ad, int kategoriId, int sehirId, int aktifKullaniciId) {
        isyeriRepository.ekle(ad, kategoriId, sehirId, aktifKullaniciId);
    }

    public void guncelle(int id, String ad, int kategoriId, int sehirId, int aktifKullaniciId) {
        isyeriRepository.guncelle(id, ad, kategoriId, sehirId, aktifKullaniciId);
    }

    public void sil(int id, int aktifKullaniciId) {
        isyeriRepository.sil(id, aktifKullaniciId);
    }
}
