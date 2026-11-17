package com.fraud.detector.service;

import com.fraud.detector.model.FraudHalkasiSonuc;
import com.fraud.detector.model.SehirKategoriRaporSatiri;
import com.fraud.detector.repository.RaporRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RaporService {

    private final RaporRepository raporRepository;

    public RaporService(RaporRepository raporRepository) {
        this.raporRepository = raporRepository;
    }

    public List<SehirKategoriRaporSatiri> sehirKategoriRaporu(LocalDate bas, LocalDate bit) {
        return raporRepository.sehirKategori(bas, bit);
    }

    public List<FraudHalkasiSonuc> fraudHalkasi(int hesapId) {
        return raporRepository.fraudHalkasi(hesapId);
    }
}
