package com.fraud.detector.service;

import com.fraud.detector.model.BekleyenOnay;
import com.fraud.detector.repository.AlarmRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MusteriOnayService {

    private final AlarmRepository alarmRepository;

    public MusteriOnayService(AlarmRepository alarmRepository) {
        this.alarmRepository = alarmRepository;
    }

    public List<BekleyenOnay> bekleyenler(int kullaniciId) {
        return alarmRepository.bekleyenler(kullaniciId);
    }

    public void onayla(int alarmId, int kullaniciId) {
        alarmRepository.teyitEt(alarmId, kullaniciId, true);
    }

    public void reddet(int alarmId, int kullaniciId) {
        alarmRepository.teyitEt(alarmId, kullaniciId, false);
    }
}
