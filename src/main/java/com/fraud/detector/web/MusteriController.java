package com.fraud.detector.web;

import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.BildirimService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MusteriController {

    private final BildirimService bildirimService;

    public MusteriController(BildirimService bildirimService) {
        this.bildirimService = bildirimService;
    }

    @GetMapping("/musteri")
    public String anaSayfa(Model model, @AuthenticationPrincipal AppUserPrincipal kullanici) {
        model.addAttribute("adSoyad", kullanici.getAdSoyad());
        model.addAttribute("okunmamisSayisi", bildirimService.okunmamisSayisi(kullanici.getId()));
        return "musteri/home";
    }
}
