package com.fraud.detector.service;

import com.fraud.detector.model.ModelOzet;
import com.fraud.detector.repository.ProfilRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminModelService {

    private final ProfilRepository profilRepository;

    public AdminModelService(ProfilRepository profilRepository) {
        this.profilRepository = profilRepository;
    }

    public List<ModelOzet> hepsi() {
        return profilRepository.hepsi();
    }

    public void yenidenEgit(int hesapId, int adminId) {
        profilRepository.yenidenEgit(hesapId, adminId);
    }
}
