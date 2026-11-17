package com.fraud.detector.service;

import com.fraud.detector.model.Bildirim;
import com.fraud.detector.repository.BildirimRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BildirimService {

    private final BildirimRepository bildirimRepository;

    public BildirimService(BildirimRepository bildirimRepository) {
        this.bildirimRepository = bildirimRepository;
    }

    public List<Bildirim> listele(int kullaniciId) {
        return bildirimRepository.listele(kullaniciId);
    }

    public int okunmamisSayisi(int kullaniciId) {
        return bildirimRepository.okunmamisSayisi(kullaniciId);
    }

    public void okunduIsaretle(int bildirimId, int kullaniciId) {
        bildirimRepository.okunduIsaretle(bildirimId, kullaniciId, kullaniciId);
    }
}
