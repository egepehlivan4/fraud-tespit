package com.fraud.detector.service;

import com.fraud.detector.model.AlarmAciklama;
import com.fraud.detector.model.AlarmDetay;
import com.fraud.detector.model.AlarmOzet;
import com.fraud.detector.model.FraudHalkasiSonuc;
import com.fraud.detector.model.IslemOzet;
import com.fraud.detector.repository.AlarmRepository;
import com.fraud.detector.repository.IslemRepository;
import com.fraud.detector.repository.RaporRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AnalistAlarmService {

    private static final int BAGLAM_ISLEM_SAYISI = 10;

    private final AlarmRepository alarmRepository;
    private final IslemRepository islemRepository;
    private final RaporRepository raporRepository;

    public AnalistAlarmService(AlarmRepository alarmRepository, IslemRepository islemRepository,
                                RaporRepository raporRepository) {
        this.alarmRepository = alarmRepository;
        this.islemRepository = islemRepository;
        this.raporRepository = raporRepository;
    }

    public List<AlarmOzet> listele(String durumFiltre) {
        return alarmRepository.listele(durumFiltre);
    }

    public Optional<AlarmDetay> detay(int alarmId) {
        return alarmRepository.bul(alarmId);
    }

    public List<AlarmAciklama> aciklama(int alarmId) {
        return alarmRepository.aciklama(alarmId);
    }

    /** Baglam icin musterinin (tum kartlari dahil) son islemleri. */
    public List<IslemOzet> sonIslemler(int kullaniciId) {
        return islemRepository.gecmis(kullaniciId, null, null, null, BAGLAM_ISLEM_SAYISI, 0);
    }

    public List<FraudHalkasiSonuc> fraudHalkasi(int hesapId) {
        return raporRepository.fraudHalkasi(hesapId);
    }

    public void karar(int alarmId, int analistId, String karar) {
        alarmRepository.karar(alarmId, analistId, karar);
    }
}
