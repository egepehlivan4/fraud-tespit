package com.fraud.detector.service;

import com.fraud.detector.model.DenetimLogSatiri;
import com.fraud.detector.repository.DenetimLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DenetimLogService {

    private static final int SAYFA_BOYUTU = 25;

    private final DenetimLogRepository denetimLogRepository;

    public DenetimLogService(DenetimLogRepository denetimLogRepository) {
        this.denetimLogRepository = denetimLogRepository;
    }

    public List<DenetimLogSatiri> listele(String tabloFiltre, int sayfa) {
        int gecerliSayfa = Math.max(sayfa, 1);
        return denetimLogRepository.listele(tabloFiltre, SAYFA_BOYUTU, (gecerliSayfa - 1) * SAYFA_BOYUTU);
    }

    public int toplamSayfa(String tabloFiltre) {
        int toplamSatir = denetimLogRepository.sayisi(tabloFiltre);
        return Math.max(1, (int) Math.ceil(toplamSatir / (double) SAYFA_BOYUTU));
    }
}
